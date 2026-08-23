package de.pixelrpg.rpg.companion;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** O(1) runtime indexes for owner and spawned entity lookup; no global entity scans are required. */
public final class CompanionRuntimeRegistry {
    private final Map<UUID, CompanionRuntime> byOwner = new ConcurrentHashMap<>();
    private final Map<UUID, CompanionRuntime> byEntity = new ConcurrentHashMap<>();

    public void register(CompanionRuntime runtime) {
        unregisterOwner(runtime.ownerUuid());
        byOwner.put(runtime.ownerUuid(), runtime);
        byEntity.put(runtime.entityUuid(), runtime);
    }

    public CompanionRuntime byOwner(UUID ownerUuid) { return byOwner.get(ownerUuid); }
    public CompanionRuntime byEntity(UUID entityUuid) { return byEntity.get(entityUuid); }

    public CompanionRuntime unregisterOwner(UUID ownerUuid) {
        CompanionRuntime removed = byOwner.remove(ownerUuid);
        if (removed != null) byEntity.remove(removed.entityUuid(), removed);
        return removed;
    }

    public CompanionRuntime unregisterEntity(UUID entityUuid) {
        CompanionRuntime removed = byEntity.remove(entityUuid);
        if (removed != null) byOwner.remove(removed.ownerUuid(), removed);
        return removed;
    }

    public Map<UUID, CompanionRuntime> byOwner() { return Map.copyOf(byOwner); }
    public void clear() { byOwner.clear(); byEntity.clear(); }

    public record CompanionRuntime(UUID ownerUuid, UUID entityUuid, String companionId) {}
}
