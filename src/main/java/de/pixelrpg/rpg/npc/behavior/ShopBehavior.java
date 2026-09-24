package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.gui.ShopGUI;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.shop.ShopManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import java.util.List;

public final class ShopBehavior implements NpcBehavior {
    private final ShopManager shops; private final PlayerProfileManager profiles; private final DialogueEngine dialogue;
    public ShopBehavior(ShopManager shops,PlayerProfileManager profiles,DialogueEngine dialogue){this.shops=shops;this.profiles=profiles;this.dialogue=dialogue;}
    @Override public NpcType type(){return NpcType.SHOP;}
    @Override public void onInteract(Player player,RPGNpc npc){
        if(!profiles.isRegistered(player.getUniqueId())){dialogue.openNotice(player,Component.text("Händler",NamedTextColor.GOLD),Component.text("Du musst zuerst registriertes Rathausmitglied sein.",NamedTextColor.WHITE),Component.text("Schließen",NamedTextColor.GRAY));return;}
        dialogue.openMultiAction(player,Component.text("Händler",NamedTextColor.GOLD),List.of(DialogBody.plainMessage(Component.text("Öffne den PixelRPG-Shop dieses Händlers.",NamedTextColor.WHITE))),List.of(dialogue.actionButton(Component.text("Shop öffnen"),NamedTextColor.GREEN,target->new ShopGUI(target,npc.id(),shops,profiles).open(target))),1);
    }
}
