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
        NpcBehavior behavior = behaviors.get(type);
        if (behavior != null) return Optional.of(behavior);

        // Profession trainers reuse the existing dialogue/crafting behavior pipeline.
        if (type == NpcType.PROFESSION_TRAINER) return Optional.ofNullable(behaviors.get(NpcType.BLACKSMITH));
        return Optional.empty();
    }
}
