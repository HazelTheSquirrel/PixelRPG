// src/main/java/de/pixelrpg/rpg/dungeon/DungeonWandListener.java
package de.pixelrpg.rpg.dungeon;

import de.pixelrpg.rpg.PixelRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class DungeonWandListener implements Listener {

    private final PixelRPGPlugin plugin;
    private final DungeonSelectionManager selectionManager;

    public DungeonWandListener(PixelRPGPlugin plugin, DungeonSelectionManager selectionManager) {
        this.plugin = plugin;
        this.selectionManager = selectionManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!DungeonWandFactory.isWand(plugin, event.getItem())) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            selectionManager.setPos1(player.getUniqueId(), event.getClickedBlock().getLocation());
            player.sendActionBar(Component.text("Position 1 set.", NamedTextColor.YELLOW));
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            selectionManager.setPos2(player.getUniqueId(), event.getClickedBlock().getLocation());
            player.sendActionBar(Component.text("Position 2 set.", NamedTextColor.YELLOW));
        }
    }
}