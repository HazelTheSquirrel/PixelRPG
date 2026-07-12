// src/main/java/de/pixelrpg/rpg/region/biome/BiomeClusterManager.java (VOLLSTÄNDIG, ersetzt alte Datei — korrektes Connected-Component-Clustering)
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

        // Alle vier Nachbar-Chunks aktiv prüfen (auch unbesuchte), um verbundene
        // Flächen unabhängig von der Besuchsreihenfolge korrekt zusammenzuführen.
        Set<String> matchingNeighborClusterIds = new HashSet<>();
        int[][] neighborOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] offset : neighborOffsets) {
            int neighborChunkX = chunkX + offset[0];
            int neighborChunkZ = chunkZ + offset[1];
            long neighborKey = packChunkKey(neighborChunkX, neighborChunkZ);

            String neighborClusterId = chunkToClusterId.get(neighborKey);
            if (neighborClusterId != null) {
                BiomeCluster neighborCluster = clustersById.get(neighborClusterId);
                if (neighborCluster != null && neighborCluster.getBiomeKey().equals(biomeKey)) {
                    matchingNeighborClusterIds.add(neighborClusterId);
                }
                continue;
            }

            // Unbesuchter Nachbar: Biome vorab abfragen, aber keinen neuen Cluster für ihn
            // anlegen (das passiert erst, wenn der Spieler ihn tatsächlich betritt).
            String neighborBiomeKey = sampleBiomeKey(world, neighborChunkX, neighborChunkZ, sampleY);
            if (!neighborBiomeKey.equals(biomeKey)) {
                continue;
            }
            // Kein Cluster vorhanden, aber gleiches Biom: nichts zu mergen, da noch kein Cluster existiert.
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