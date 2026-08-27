package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.QuickActionsDialogService;
import de.pixelrpg.rpg.npc.behavior.FillerBehavior;
import de.pixelrpg.rpg.npc.behavior.ProfessionTrainerBehavior;
import de.pixelrpg.rpg.npc.behavior.QuestBehavior;
import de.pixelrpg.rpg.profession.Profession;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class NpcBehaviorRegistry {
    private final Map<NpcType, NpcBehavior> behaviors = new EnumMap<>(NpcType.class);
    private final QuickActionsDialogService quickActions;

    public NpcBehaviorRegistry(QuickActionsDialogService quickActions) {
        this.quickActions = quickActions;
    }

    public void register(NpcBehavior behavior) {
        behaviors.put(behavior.type(), behavior);
    }

    public Optional<NpcBehavior> get(NpcType type) {
        PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
        if (plugin == null) return Optional.empty();

        if (type == NpcType.QUEST && plugin.getQuestManager() != null) {
            return Optional.of(new QuestBehavior(
                    plugin.getQuestManager(),
                    plugin.getPlayerProfileManager(),
                    new DialogueEngine()));
        }

        NpcBehavior behavior = behaviors.get(type);
        if (behavior != null) return Optional.of(behavior);

        if (plugin.getProfessionSystem() != null && quickActions != null) {
            Profession profession = professionFor(type);
            if (profession != null) {
                return Optional.of(new ProfessionTrainerBehavior(
                        type,
                        profession,
                        plugin.getPlayerProfileManager(),
                        plugin.getProfessionSystem().professionService(),
                        new DialogueEngine(),
                        quickActions));
            }
        }

        if (type == NpcType.FILLER && plugin.getQuestManager() != null) {
            return Optional.of(new FillerBehavior(
                    plugin.getQuestManager(),
                    plugin.getPlayerProfileManager(),
                    new DialogueEngine()));
        }

        return Optional.empty();
    }

    private Profession professionFor(NpcType type) {
        return switch (type) {
            case PROFESSION_BLACKSMITH -> Profession.BLACKSMITH;
            case PROFESSION_PROVISIONER -> Profession.PROVISIONER;
            case PROFESSION_SCHOLAR -> Profession.SCHOLAR;
            case PROFESSION_ALCHEMIST -> Profession.ALCHEMIST;
            default -> null;
        };
    }
}
