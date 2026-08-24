package de.pixelrpg.rpg.companion;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** O(1) runtime indexes for owner and spawned entity lookup; no global entity scans are required. */
public final class CompanionRuntimeRegistry {
    private final Map<UUID, CompanionRuntime> byOwner = new ConcurrentHashMap<>();
    private final Map<UUID, CompanionRuntime> byEntity = new ConcurrentHashMap<>();

    public void register(CompanionRuntime runtime) {
        if (runtime == null) return;

        CompanionRuntime previousOwner = byOwner.put(runtime.ownerUuid(), runtime);
        if (previousOwner != null && !previousOwner.entityUuid().equals(runtime.entityUuid())) {
            byEntity.remove(previousOwner.entityUuid(), previousOwner);
        }

        CompanionRuntime previousEntity = byEntity.put(runtime.entityUuid(), runtime);
        if (previousEntity != null && !previousEntity.ownerUuid().equals(runtime.ownerUuid())) {
            byOwner.remove(previousEntity.ownerUuid(), previousEntity);
        }
    }

    public CompanionRuntime byOwner(UUID ownerUuid) {
        return ownerUuid == null ? null : byOwner.get(ownerUuid);
    }

    public CompanionRuntime byEntity(UUID entityUuid) {
        return entityUuid == null ? null : byEntity.get(entityUuid);
    }

    public CompanionRuntime unregisterOwner(UUID ownerUuid) {
        if (ownerUuid == null) return null;
        CompanionRuntime removed = byOwner.remove(ownerUuid);
        if (removed != null) byEntity.remove(removed.entityUuid(), removed);
        return removed;
    }

    public CompanionRuntime unregisterEntity(UUID entityUuid) {
        if (entityUuid == null) return null;
        CompanionRuntime removed = byEntity.remove(entityUuid);
        if (removed != null) byOwner.remove(removed.ownerUuid(), removed);
        return removed;
    }

    public Map<UUID, CompanionRuntime> byOwner() {
        return Map.copyOf(byOwner);
    }

    public void clear() {
        byOwner.clear();
        byEntity.clear();
    }

    public record CompanionRuntime(UUID ownerUuid, UUID entityUuid, String companionId) {
        public CompanionRuntime {
            if (ownerUuid == null) throw new IllegalArgumentException("ownerUuid must not be null");
            if (entityUuid == null) throw new IllegalArgumentException("entityUuid must not be null");
            if (companionId == null || companionId.isBlank()) throw new IllegalArgumentException("companionId must not be blank");
        }
    }
}
