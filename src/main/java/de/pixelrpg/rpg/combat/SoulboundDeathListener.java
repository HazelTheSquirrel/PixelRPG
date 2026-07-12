// src/main/java/de/pixelrpg/rpg/combat/SoulboundDeathListener.java
package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.item.SoulboundService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulboundDeathListener implements Listener {

    private final Map<UUID, List<ItemStack>> pendingReturns = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> keep = new ArrayList<>();

        event.getDrops().removeIf(item -> {
            if (SoulboundService.isSoulbound(item)) {
                keep.add(item.clone());
                return true;
            }
            return false;
        });

        if (!keep.isEmpty()) {
            pendingReturns.put(player.getUniqueId(), keep);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        List<ItemStack> items = pendingReturns.remove(event.getPlayer().getUniqueId());
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ItemStack item : items) {
            event.getPlayer().getInventory().addItem(item).values()
                    .forEach(remainder -> event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), remainder));
        }
    }
}