package de.pixelrpg.rpg.npc;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class NpcBehaviorRegistry {
    private final Map<NpcType, NpcBehavior> behaviors = new EnumMap<>(NpcType.class);
    public void register(NpcBehavior behavior) {
        if (behavior == null) throw new IllegalArgumentException("behavior must not be null");
        if (behaviors.putIfAbsent(behavior.type(), behavior) != null) throw new IllegalStateException("NPC behavior already registered for type " + behavior.type());
    }
    public Optional<NpcBehavior> get(NpcType type) { return type == null ? Optional.empty() : Optional.ofNullable(behaviors.get(type)); }
}
