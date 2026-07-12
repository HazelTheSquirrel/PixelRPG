// src/main/java/de/pixelrpg/rpg/region/RegionWandListener.java
package de.pixelrpg.rpg.region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class RegionWandListener implements Listener {

    private final RegionSelectionManager selectionManager;

    public RegionWandListener(RegionSelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!RegionWandFactory.isWand(event.getItem())) {
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
            player.sendActionBar(Component.text("Position 1 set. Select Position 2, then run /rpgadmin region create|addbox <id>. Repeat for L-shapes / multi-box regions.", NamedTextColor.YELLOW));
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            selectionManager.setPos2(player.getUniqueId(), event.getClickedBlock().getLocation());
            player.sendActionBar(Component.text("Position 2 set. Run /rpgadmin region create|addbox <id> to save this box.", NamedTextColor.YELLOW));
        }
    }
}