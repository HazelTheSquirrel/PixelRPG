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

/** Central registry for the single runtime instance of every NPC behavior. */
public final class NpcBehaviorRegistry {
    private final Map<NpcType, NpcBehavior> behaviors = new EnumMap<>(NpcType.class);
    private final QuickActionsDialogService quickActions;

    public NpcBehaviorRegistry(QuickActionsDialogService quickActions) {
        this.quickActions = quickActions;
    }

    public void register(NpcBehavior behavior) {
        if (behavior == null) throw new IllegalArgumentException("behavior must not be null");
        behaviors.put(behavior.type(), behavior);
    }

    public Optional<NpcBehavior> get(NpcType type) {
        if (type == null) return Optional.empty();
        NpcBehavior registered = behaviors.get(type);
        if (registered != null) return Optional.of(registered);

        PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
        if (plugin == null) return Optional.empty();

        NpcBehavior created = createMissingBehavior(plugin, type);
        if (created == null) return Optional.empty();
        behaviors.put(type, created);
        return Optional.of(created);
    }

    private NpcBehavior createMissingBehavior(PixelRPGPlugin plugin, NpcType type) {
        if (type == NpcType.QUEST && plugin.getQuestManager() != null) {
            return new QuestBehavior(plugin.getQuestManager(), plugin.getPlayerProfileManager(), new DialogueEngine());
        }

        if (type == NpcType.FILLER && plugin.getQuestManager() != null) {
            return new FillerBehavior(plugin.getQuestManager(), plugin.getPlayerProfileManager(), new DialogueEngine());
        }

        if (plugin.getProfessionSystem() != null && quickActions != null) {
            Profession profession = professionFor(type);
            if (profession != null) {
                return new ProfessionTrainerBehavior(
                        type,
                        profession,
                        plugin.getPlayerProfileManager(),
                        plugin.getProfessionSystem().professionService(),
                        new DialogueEngine(),
                        quickActions);
            }
        }
        return null;
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
