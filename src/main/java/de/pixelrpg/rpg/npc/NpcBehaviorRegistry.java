// src/main/java/de/pixelrpg/rpg/npc/NpcBehaviorRegistry.java
package de.pixelrpg.rpg.npc;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class NpcBehaviorRegistry {

    private final Map<NpcType, NpcBehavior> behaviors = new EnumMap<>(NpcType.class);

    public void register(NpcBehavior behavior) {
        behaviors.put(behavior.type(), behavior);
    }

    public Optional<NpcBehavior> get(NpcType type) {
        return Optional.ofNullable(behaviors.get(type));
    }
}