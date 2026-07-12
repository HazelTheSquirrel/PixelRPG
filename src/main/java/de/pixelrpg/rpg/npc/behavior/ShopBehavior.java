// src/main/java/de/pixelrpg/rpg/npc/behavior/ShopBehavior.java
package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.gui.ShopGUI;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class ShopBehavior implements NpcBehavior {

    private final ShopManager shopManager;
    private final PlayerProfileManager profileManager;

    public ShopBehavior(ShopManager shopManager, PlayerProfileManager profileManager) {
        this.shopManager = shopManager;
        this.profileManager = profileManager;
    }

    @Override
    public NpcType type() {
        return NpcType.SHOP;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a registered guild member.", NamedTextColor.RED));
            return;
        }
        new ShopGUI(player, npc.id(), shopManager, profileManager).open(player);
    }
}