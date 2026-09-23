package de.pixelrpg.rpg.npc;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface NpcRepository extends AutoCloseable {
    CompletableFuture<List<NpcRecord>> load();
    CompletableFuture<Void> save(int nextId, List<NpcRecord> records);
    @Override default void close() {}
    record NpcRecord(String id, String type, String name, String world, double x, double y, double z,
                     float yaw, float pitch, String skinSource, String skinValue, String skinSignature,
                     String profession) {}
}
