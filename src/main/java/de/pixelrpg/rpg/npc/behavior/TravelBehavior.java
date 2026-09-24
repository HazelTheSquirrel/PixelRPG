package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.TravelDialog;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class TravelBehavior implements NpcBehavior {
    private final NpcManager npcs; private final PlayerProfileManager profiles; private final DialogueEngine dialogue;
    public TravelBehavior(NpcManager npcs,PlayerProfileManager profiles,DialogueEngine dialogue){this.npcs=npcs;this.profiles=profiles;this.dialogue=dialogue;}
    @Override public NpcType type(){return NpcType.TRAVEL;}
    @Override public void onInteract(Player player,RPGNpc npc){
        if(!profiles.isRegistered(player.getUniqueId())){dialogue.openNotice(player,Component.text("Schnellreise",NamedTextColor.LIGHT_PURPLE),Component.text("Du musst zuerst registriertes Rathausmitglied sein.",NamedTextColor.WHITE),Component.text("Schließen",NamedTextColor.GRAY));return;}
        PlayerProfile profile=profiles.getProfile(player.getUniqueId()).orElse(null); if(profile==null)return;
        if(!profile.hasUnlockedWaypoint(npc.id())){profiles.unlockWaypoint(player.getUniqueId(),npc.id());dialogue.openNotice(player,Component.text("Reisepunkt freigeschaltet",NamedTextColor.GREEN),Component.text(npc.name()+" wurde als Reisepunkt freigeschaltet."),Component.text("Schließen",NamedTextColor.GREEN));return;}
        new TravelDialog(npcs,profiles,dialogue).open(player,npc.id());
    }
}
