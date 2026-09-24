package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class FillerBehavior implements NpcBehavior {
    private final QuestService quests;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogue;
    public FillerBehavior(QuestService quests, PlayerProfileManager profiles, DialogueEngine dialogue) { this.quests=quests; this.profiles=profiles; this.dialogue=dialogue; }
    @Override public NpcType type(){ return NpcType.FILLER; }
    @Override public void onInteract(Player player,RPGNpc npc){
        if(!profiles.isRegistered(player.getUniqueId())){ dialogue.openNotice(player,Component.text("Questziel",NamedTextColor.WHITE),Component.text("Du bist noch nicht registriert.",NamedTextColor.WHITE),Component.text("Schließen",NamedTextColor.WHITE)); return; }
        quests.progressTalk(player,npc.id());
        dialogue.openNotice(player,Component.text(npc.name(),NamedTextColor.WHITE),Component.text("Du hast den Ziel-NPC erreicht.",NamedTextColor.WHITE),Component.text("Schließen",NamedTextColor.WHITE));
    }
}
