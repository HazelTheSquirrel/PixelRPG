package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
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
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ShopBehavior implements NpcBehavior {
    private final ShopManager shopManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public ShopBehavior(ShopManager shopManager, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.shopManager = shopManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    @Override public NpcType type() { return NpcType.SHOP; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst registriertes Rathausmitglied sein.", NamedTextColor.RED));
            return;
        }
        if (shopManager.getEntries(npc.id()).isEmpty()) {
            PixelRPGPlugin.getInstance().getLogger().log(Level.WARNING,
                    "Shop NPC '" + npc.name() + "' (internal id: " + npc.id() + ") has no items configured. "
                            + "Run '/rpgadmin shop edit " + npc.id() + "' to stock it.");
        }
        dialogueEngine.openMultiAction(
                player,
                Component.text("Händler", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Öffne den PixelRPG-Shop dieses Händlers."))),
                List.of(dialogueEngine.actionButton(Component.text("Shop öffnen"), NamedTextColor.GREEN,
                        target -> new ShopGUI(target, npc.id(), shopManager, profileManager).open(target))),
                1
        );
    }
}
