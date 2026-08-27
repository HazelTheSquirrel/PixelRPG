package de.pixelrpg.rpg.npc;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Central registry for the single runtime instance of every NPC behavior. */
public final class NpcBehaviorRegistry {
    private final Map<NpcType, NpcBehavior> behaviors = new EnumMap<>(NpcType.class);

    public void register(NpcBehavior behavior) {
        if (behavior == null) throw new IllegalArgumentException("behavior must not be null");
        NpcBehavior previous = behaviors.put(behavior.type(), behavior);
        if (previous != null && previous != behavior) {
            throw new IllegalStateException("NPC behavior already registered for type: " + behavior.type());
        }
    }

    public Optional<NpcBehavior> get(NpcType type) {
        return type == null ? Optional.empty() : Optional.ofNullable(behaviors.get(type));
    }
}
