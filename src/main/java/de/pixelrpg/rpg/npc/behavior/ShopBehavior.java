// src/main/java/de/pixelrpg/rpg/npc/behavior/ShopBehavior.java (VOLLSTÄNDIG, ersetzt alte Datei — Warnung an Spieler/Log bei leerem Shop, damit ein ID-Mismatch sofort auffällt statt still leer zu bleiben)
package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.gui.ShopGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public final class ShopBehavior implements NpcBehavior {

    private final ShopManager shopManager;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public ShopBehavior(ShopManager shopManager, PlayerProfileManager profileManager) {
        this.shopManager = shopManager;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    public NpcType type() {
        return NpcType.SHOP;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            lang.send(player, "npc.not-registered");
            return;
        }

        if (shopManager.getEntries(npc.id()).isEmpty()) {
            PixelRPGPlugin.getInstance().getLogger().log(Level.WARNING,
                    "Shop NPC '" + npc.name() + "' (internal id: " + npc.id() + ") has no items configured. "
                            + "Run '/rpgadmin shop edit " + npc.id() + "' to stock it.");
            if (player.hasPermission("rpg.admin")) {
                player.sendMessage(Component.text(
                        "This shop is empty. Run '/rpgadmin shop edit " + npc.id() + "' to stock it.",
                        NamedTextColor.RED));
            }
        }

        new ShopGUI(player, npc.id(), shopManager, profileManager).open(player);
    }
}