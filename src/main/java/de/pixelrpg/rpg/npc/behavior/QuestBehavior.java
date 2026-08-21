package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.gui.QuestCategoryGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

public final class QuestBehavior implements NpcBehavior {
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final LanguageManager lang;

    public QuestBehavior(QuestManager questManager, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.lang = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    public NpcType type() {
        return NpcType.QUEST;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            lang.send(player, "npc.not-registered");
            return;
        }

        dialogueEngine.openMultiAction(
                player,
                Component.text("Quests", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Verwalte deine PixelRPG-Quests."))),
                List.of(dialogueEngine.actionButton(
                        Component.text("Questbuch öffnen"),
                        NamedTextColor.GREEN,
                        target -> new QuestCategoryGUI(target, questManager, profileManager).open(target)
                )),
                1
        );
    }
}
