// src/main/java/de/pixelrpg/rpg/region/biome/BiomeClusterStorage.java
// JSON-Persistenz über Gson. Auf Paper-Servern durchgehend im Classpath vorhanden.
// Sollte der Import bei euch fehlschlagen, ist diese Datei die einzige Stelle, die angepasst werden muss.
package de.pixelrpg.rpg.region.biome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BiomeClusterStorage {

    private record ClusterDto(String id, String worldName, String biomeKey, List<Long> chunkKeys) {
    }

    private record StorageDto(List<ClusterDto> clusters, Map<String, Integer> biomeOrdinalCounters) {
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Logger logger;

    public BiomeClusterStorage(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public void save(List<BiomeCluster> clusters, Map<String, Integer> ordinalCounters) {
        List<ClusterDto> dtos = new ArrayList<>();
        for (BiomeCluster cluster : clusters) {
            dtos.add(new ClusterDto(cluster.getId(), cluster.getWorldName(), cluster.getBiomeKey(),
                    new ArrayList<>(cluster.getChunkKeys())));
        }

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(new StorageDto(dtos, ordinalCounters), writer);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save biome_clusters.json", e);
        }
    }

    public record LoadResult(List<BiomeCluster> clusters, Map<String, Integer> ordinalCounters) {
    }

    public LoadResult load() {
        if (!file.exists()) {
            return new LoadResult(new ArrayList<>(), new java.util.HashMap<>());
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<StorageDto>() {}.getType();
            StorageDto dto = gson.fromJson(reader, type);
            if (dto == null) {
                return new LoadResult(new ArrayList<>(), new java.util.HashMap<>());
            }

            List<BiomeCluster> clusters = new ArrayList<>();
            if (dto.clusters() != null) {
                for (ClusterDto clusterDto : dto.clusters()) {
                    BiomeCluster cluster = new BiomeCluster(clusterDto.id(), clusterDto.worldName(), clusterDto.biomeKey());
                    if (clusterDto.chunkKeys() != null) {
                        for (Long key : clusterDto.chunkKeys()) {
                            cluster.addChunk(key);
                        }
                    }
                    clusters.add(cluster);
                }
            }

            Map<String, Integer> ordinalCounters = dto.biomeOrdinalCounters() != null
                    ? dto.biomeOrdinalCounters() : new java.util.HashMap<>();

            return new LoadResult(clusters, ordinalCounters);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load biome_clusters.json", e);
            return new LoadResult(new ArrayList<>(), new java.util.HashMap<>());
        }
    }
}