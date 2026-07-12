// src/main/java/de/pixelrpg/rpg/region/biome/BiomeCluster.java
package de.pixelrpg.rpg.region.biome;

import java.util.HashSet;
import java.util.Set;

public final class BiomeCluster {

    private final String id;
    private final String worldName;
    private final String biomeKey;
    private final Set<Long> chunkKeys = new HashSet<>();

    public BiomeCluster(String id, String worldName, String biomeKey) {
        this.id = id;
        this.worldName = worldName;
        this.biomeKey = biomeKey;
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getBiomeKey() {
        return biomeKey;
    }

    public Set<Long> getChunkKeys() {
        return chunkKeys;
    }

    public void addChunk(long chunkKey) {
        chunkKeys.add(chunkKey);
    }
}