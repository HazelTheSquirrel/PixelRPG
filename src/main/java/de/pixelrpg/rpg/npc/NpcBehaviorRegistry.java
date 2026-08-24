package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.behavior.FillerBehavior;
import de.pixelrpg.rpg.npc.behavior.ProfessionTrainerBehavior;
import de.pixelrpg.rpg.npc.behavior.QuestBehaviorV2;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class NpcBehaviorRegistry {
    private final Map<NpcType, NpcBehavior> behaviors = new EnumMap<>(NpcType.class);

    public void register(NpcBehavior behavior) {
        behaviors.put(behavior.type(), behavior);
    }

    public Optional<NpcBehavior> get(NpcType type) {
        PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
        if (plugin == null) return Optional.empty();

        if (type == NpcType.QUEST && plugin.getQuestManager() != null) {
            return Optional.of(new QuestBehaviorV2(plugin.getQuestManager(), plugin.getPlayerProfileManager(), new DialogueEngine()));
        }

        NpcBehavior behavior = behaviors.get(type);
        if (behavior != null) return Optional.of(behavior);

        if (type == NpcType.PROFESSION_TRAINER && plugin.getProfessionSystem() != null) {
            return Optional.of(new ProfessionTrainerBehavior(
                    plugin.getPlayerProfileManager(),
                    plugin.getProfessionSystem().professionService(),
                    plugin.getProfessionSystem().craftingService(),
                    new DialogueEngine()));
        }

        if (type == NpcType.FILLER && plugin.getQuestManager() != null) {
            return Optional.of(new FillerBehavior(
                    plugin.getQuestManager(),
                    plugin.getPlayerProfileManager(),
                    new DialogueEngine()));
        }

        return Optional.empty();
    }
}
