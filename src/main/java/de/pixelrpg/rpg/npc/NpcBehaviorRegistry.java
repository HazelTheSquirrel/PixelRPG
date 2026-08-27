package de.pixelrpg.rpg.npc;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Central registry for the single runtime instance of every NPC behavior. */
public final class NpcBehaviorRegistry {
    private final Map<NpcType, NpcBehavior> behaviors = new EnumMap<>(NpcType.class);

    public void register(NpcBehavior behavior) {
        if (behavior == null) throw new IllegalArgumentException("behavior must not be null");
        behaviors.put(behavior.type(), behavior);
    }

    public Optional<NpcBehavior> get(NpcType type) {
        return Optional.ofNullable(behaviors.get(type));
    }
}
