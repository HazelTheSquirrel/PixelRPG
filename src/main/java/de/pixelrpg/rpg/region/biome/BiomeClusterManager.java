package de.pixelrpg.rpg.region.biome;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BiomeClusterManager {

    // Maximale Anzahl an Chunks, die in eine Richtung "durch" Fluss-Chunks
    // hindurch gesucht wird, um dahinterliegendes Land zu finden. Begrenzt
    // auf realistische Flussbreiten (6 Chunks = 96 Blöcke).
    private static final int MAX_RIVER_SKIP_CHUNKS = 6;

    private final Plugin plugin;
    private final BiomeClusterStorage storage;
    private final BiomeNameRepository nameRepository;

    private final Map<String, BiomeCluster> clustersById = new ConcurrentHashMap<>();
    private final Map<Long, String> chunkToClusterId = new ConcurrentHashMap<>();
    private final Map<String, Integer> biomeOrdinalCounters = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    public BiomeClusterManager(Plugin plugin, BiomeNameRepository nameRepository) {
        this.plugin = plugin;
        this.storage = new BiomeClusterStorage(new File(plugin.getDataFolder(), "biome_clusters.json"), plugin.getLogger());
        this.nameRepository = nameRepository;
    }

    public void load() {
        clustersById.clear();
        chunkToClusterId.clear();
        biomeOrdinalCounters.clear();

        BiomeClusterStorage.LoadResult result = storage.load();
        for (BiomeCluster cluster : result.clusters()) {
            clustersById.put(cluster.getId(), cluster);
            for (Long chunkKey : cluster.getChunkKeys()) {
                chunkToClusterId.put(chunkKey, cluster.getId());
            }
        }
        biomeOrdinalCounters.putAll(result.ordinalCounters());
    }

    public void saveIfDirty() {
        if (!dirty) {
            return;
        }
        storage.save(new ArrayList<>(clustersById.values()), new HashMap<>(biomeOrdinalCounters));
        dirty = false;
    }

    public record ClusterInfo(String clusterId, String biomeKey, String displayName) {
    }

    /**
     * Liefert die Cluster-Info für den Chunk, in dem sich die Location befindet.
     * Neuberechnung erfolgt ausschließlich pro Chunk (nicht pro Block), Biome-Sampling
     * am Chunk-Zentrum, um Rand-/Übergangsrauschen der 4x4x4-Biome-Zellen zu vermeiden.
     *
     * Fluss-Chunks (River/Frozen River) erzeugen keinen eigenen Cluster: sie werden
     * entweder einem angrenzenden Land-Cluster zugeschlagen oder liefern (falls noch
     * kein Nachbar-Cluster existiert) kein Ergebnis, statt einen "Fluss"-Titel zu zeigen.
     * Zusätzlich wird bei der Nachbarschaftsprüfung "durch" Fluss-Chunks hindurchgesucht,
     * damit zwei durch einen Fluss getrennte Flächen desselben Bioms zu einem einzigen
     * Cluster zusammengeführt werden.
     */
    public Optional<ClusterInfo> resolveChunk(World world, int chunkX, int chunkZ, int sampleY) {
        long chunkKey = packChunkKey(chunkX, chunkZ);

        String existingClusterId = chunkToClusterId.get(chunkKey);
        if (existingClusterId != null) {
            BiomeCluster cluster = clustersById.get(existingClusterId);
            if (cluster != null) {
                String name = nameRepository.getOrGenerate(cluster.getId(), cluster.getBiomeKey(), extractOrdinal(cluster.getId()));
                return Optional.of(new ClusterInfo(cluster.getId(), cluster.getBiomeKey(), name));
            }
        }

        String biomeKey = sampleBiomeKey(world, chunkX, chunkZ, sampleY);

        if (isRiverBiome(biomeKey)) {
            return resolveRiverChunk(world, chunkX, chunkZ, chunkKey, sampleY);
        }

        // Alle vier Nachbar-Richtungen prüfen, dabei ggf. durch Fluss-Chunks
        // hindurchsuchen, um verbundene Flächen desselben Bioms unabhängig
        // von einem dazwischenliegenden Fluss korrekt zusammenzuführen.
        Set<String> matchingNeighborClusterIds = new HashSet<>();
        int[][] neighborOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] offset : neighborOffsets) {
            findMatchingClusterThroughRivers(world, chunkX, chunkZ, offset[0], offset[1], biomeKey, sampleY)
                    .ifPresent(matchingNeighborClusterIds::add);
        }

        BiomeCluster targetCluster;

        if (matchingNeighborClusterIds.isEmpty()) {
            int nextOrdinal = biomeOrdinalCounters.merge(biomeKey, 1, Integer::sum);
            String newClusterId = biomeKey.replace(':', '_') + "#" + nextOrdinal;
            targetCluster = new BiomeCluster(newClusterId, world.getName(), biomeKey);
            clustersById.put(newClusterId, targetCluster);
        } else if (matchingNeighborClusterIds.size() == 1) {
            targetCluster = clustersById.get(matchingNeighborClusterIds.iterator().next());
        } else {
            // Dieser Chunk verbindet mehrere bisher getrennte Cluster desselben Bioms:
            // alle in einen einzigen zusammenführen (echtes Connected-Component-Verhalten).
            targetCluster = mergeClusters(matchingNeighborClusterIds);
        }

        targetCluster.addChunk(chunkKey);
        chunkToClusterId.put(chunkKey, targetCluster.getId());
        dirty = true;

        int ordinal = extractOrdinal(targetCluster.getId());
        String name = nameRepository.getOrGenerate(targetCluster.getId(), targetCluster.getBiomeKey(), ordinal);
        return Optional.of(new ClusterInfo(targetCluster.getId(), targetCluster.getBiomeKey(), name));
    }

    /**
     * Behandelt einen Chunk, dessen gesampeltes Biom ein Fluss ist. Erzeugt keinen
     * eigenen Cluster, sondern hängt sich an einen direkt angrenzenden, bereits
     * existierenden Land-Cluster an (beliebiger Biomtyp – der Fluss-Chunk selbst
     * bekommt nie einen eigenen Titel). Existiert noch kein Nachbar-Cluster,
     * wird kein Ergebnis geliefert (Titel bleibt unverändert).
     */
    private Optional<ClusterInfo> resolveRiverChunk(World world, int chunkX, int chunkZ, long chunkKey, int sampleY) {
        int[][] neighborOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] offset : neighborOffsets) {
            long neighborKey = packChunkKey(chunkX + offset[0], chunkZ + offset[1]);
            String neighborClusterId = chunkToClusterId.get(neighborKey);
            if (neighborClusterId == null) {
                continue;
            }
            BiomeCluster neighborCluster = clustersById.get(neighborClusterId);
            if (neighborCluster == null) {
                continue;
            }

            neighborCluster.addChunk(chunkKey);
            chunkToClusterId.put(chunkKey, neighborCluster.getId());
            dirty = true;

            int ordinal = extractOrdinal(neighborCluster.getId());
            String name = nameRepository.getOrGenerate(neighborCluster.getId(), neighborCluster.getBiomeKey(), ordinal);
            return Optional.of(new ClusterInfo(neighborCluster.getId(), neighborCluster.getBiomeKey(), name));
        }

        // Noch kein angrenzender Land-Cluster bekannt (z. B. Spieler betritt den
        // Fluss als Erstes) – kein Titel-Update, Fluss selbst bleibt unbenannt.
        return Optional.empty();
    }

    /**
     * Sucht in eine Richtung (dx, dz) nach einem Cluster mit passendem Biomtyp,
     * wobei Fluss-Chunks auf dem Weg übersprungen werden (bis zu MAX_RIVER_SKIP_CHUNKS).
     * Liefert die Cluster-ID, falls am Ende der Suche Land mit gleichem Biom und
     * bereits existierendem Cluster gefunden wird.
     */
    private Optional<String> findMatchingClusterThroughRivers(World world, int chunkX, int chunkZ,
                                                                int dx, int dz, String biomeKey, int sampleY) {
        int currentX = chunkX;
        int currentZ = chunkZ;

        for (int step = 1; step <= MAX_RIVER_SKIP_CHUNKS; step++) {
            currentX += dx;
            currentZ += dz;

            long neighborKey = packChunkKey(currentX, currentZ);
            String neighborClusterId = chunkToClusterId.get(neighborKey);

            if (neighborClusterId != null) {
                BiomeCluster neighborCluster = clustersById.get(neighborClusterId);
                if (neighborCluster != null) {
                    if (neighborCluster.getBiomeKey().equals(biomeKey)) {
                        return Optional.of(neighborClusterId);
                    }
                    // Anderer, bereits geclusterter Biomtyp (kein Fluss) beendet
                    // die Suche in dieser Richtung – kein Durchsuchen fremder Cluster.
                    return Optional.empty();
                }
            }

            // Kein Cluster an dieser Stelle bekannt: prüfen, ob es sich um einen
            // Fluss-Chunk handelt, um ggf. weiterzusuchen; andernfalls abbrechen.
            String sampledBiome = sampleBiomeKey(world, currentX, currentZ, sampleY);
            if (!isRiverBiome(sampledBiome)) {
                // Unbesuchtes Land ohne Cluster – nichts zu mergen (analog zum
                // bisherigen Verhalten bei unbesuchten direkten Nachbarn).
                return Optional.empty();
            }
            // Fluss-Chunk: weitersuchen in dieselbe Richtung.
        }

        return Optional.empty();
    }

    private boolean isRiverBiome(String biomeKey) {
        return biomeKey.toLowerCase().contains("river");
    }

    private BiomeCluster mergeClusters(Set<String> clusterIds) {
        List<String> sorted = new ArrayList<>(clusterIds);
        sorted.sort(String::compareTo);
        String survivorId = sorted.get(0);
        BiomeCluster survivor = clustersById.get(survivorId);

        for (int i = 1; i < sorted.size(); i++) {
            BiomeCluster absorbed = clustersById.remove(sorted.get(i));
            if (absorbed == null) {
                continue;
            }
            for (Long chunkKey : absorbed.getChunkKeys()) {
                survivor.addChunk(chunkKey);
                chunkToClusterId.put(chunkKey, survivor.getId());
            }
        }
        dirty = true;
        return survivor;
    }

    private String sampleBiomeKey(World world, int chunkX, int chunkZ, int sampleY) {
        int sampleX = (chunkX << 4) + 8;
        int sampleZ = (chunkZ << 4) + 8;
        Biome biome = world.getBiome(sampleX, sampleY, sampleZ);
        return biome.getKey().toString();
    }

    private int extractOrdinal(String clusterId) {
        int hashIndex = clusterId.lastIndexOf('#');
        if (hashIndex < 0) {
            return 1;
        }
        try {
            return Integer.parseInt(clusterId.substring(hashIndex + 1));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private long packChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xffffffffL) | (((long) chunkZ & 0xffffffffL) << 32);
    }
}