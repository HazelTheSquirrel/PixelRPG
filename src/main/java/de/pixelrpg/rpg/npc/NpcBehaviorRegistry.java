package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.behavior.ProfessionTrainerBehavior;

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

        if (type == NpcType.PROFESSION_TRAINER) {
            PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
            if (plugin != null && plugin.getProfessionSystem() != null) {
                return Optional.of(new ProfessionTrainerBehavior(
                        plugin.getPlayerProfileManager(),
                        plugin.getProfessionSystem().professionService(),
                        plugin.getProfessionSystem().craftingService(),
                        new DialogueEngine()));
            }
        }
        return Optional.empty();
    }
}
