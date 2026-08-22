package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/** Generic quest destination NPC used for village, travel and player-built settlement chains. */
public final class FillerBehavior implements NpcBehavior {
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public FillerBehavior(QuestManager questManager, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public NpcType type() {
        return NpcType.FILLER;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openNotice(player,
                    Component.text("Questziel", NamedTextColor.WHITE),
                    Component.text("Du bist noch nicht registriert.", NamedTextColor.WHITE),
                    Component.text("Schließen", NamedTextColor.WHITE));
            return;
        }

        questManager.progressTalkToNpc(player, npc.id());
        dialogueEngine.openNotice(player,
                Component.text(npc.name(), NamedTextColor.WHITE),
                Component.text("Du hast den Ziel-NPC erreicht.", NamedTextColor.WHITE),
                Component.text("Schließen", NamedTextColor.WHITE));
    }
}
