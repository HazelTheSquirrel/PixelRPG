package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
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
    private final GuildAPI guildAPI;
    private final Map<UUID, List<ItemStack>> pendingReturns = new ConcurrentHashMap<>();

    public SoulboundDeathListener(GuildAPI guildAPI) {
        this.guildAPI = guildAPI;
    }

    // Zuständig für das Sichern von PixelRPG-Seelen gebundener Gegenstände beim Tod registrierter Spieler.
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!guildAPI.isRegistered(player.getUniqueId())) return;

        List<ItemStack> keep = new ArrayList<>();
        event.getDrops().removeIf(item -> {
            if (!SoulboundService.isSoulbound(item)) return false;
            keep.add(item.clone());
            return true;
        });

        if (!keep.isEmpty()) pendingReturns.put(player.getUniqueId(), keep);
    }

    // Zuständig für die Rückgabe der gesicherten Seelen gebundenen Gegenstände nach dem Respawn.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!guildAPI.isRegistered(event.getPlayer().getUniqueId())) {
            pendingReturns.remove(event.getPlayer().getUniqueId());
            return;
        }

        List<ItemStack> items = pendingReturns.remove(event.getPlayer().getUniqueId());
        if (items == null || items.isEmpty()) return;

        for (ItemStack item : items) {
            event.getPlayer().getInventory().addItem(item).values()
                    .forEach(remainder -> event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), remainder));
        }
    }
}
