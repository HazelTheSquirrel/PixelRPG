package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.core.RPGKeys;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

/** Event-driven NPC look controller. NPCs are evaluated only when nearby players actually move. */
public final class NpcLookTask implements Listener {
    private final Plugin plugin;
    private final NpcManager npcManager;
    private final double radius;
    private final double radiusSquared;
    private boolean started;

    public NpcLookTask(Plugin plugin, NpcManager npcManager, double radius, int ignoredIntervalTicks) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.radius = Math.max(1.0D, radius);
        this.radiusSquared = this.radius * this.radius;
    }

    public void start() {
        if (started) return;
        started = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) updateAround(player.getLocation());
    }

    public void stop() {
        if (!started) return;
        org.bukkit.event.HandlerList.unregisterAll(this);
        started = false;
    }

    // A player joining can immediately become the nearest target for nearby NPCs.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateAround(event.getPlayer().getLocation());
    }

    // NPC look state wakes only when the player crosses a block boundary near the NPC.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || sameBlock(from, to)) return;
        updateAround(from);
        updateAround(to);
    }

    // A disconnect can change the nearest-player target of NPCs in the player's old area.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        updateAround(event.getPlayer().getLocation());
    }

    private void updateAround(Location center) {
        if (center == null || center.getWorld() == null) return;
        Set<UUID> candidates = new HashSet<>();
        for (Entity entity : center.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.getPersistentDataContainer().has(RPGKeys.Npc.npcId(), org.bukkit.persistence.PersistentDataType.STRING)) continue;
            if (living.getLocation().distanceSquared(center) <= radiusSquared) candidates.add(living.getUniqueId());
        }
        for (UUID entityId : candidates) updateNpc(entityId);
    }

    private void updateNpc(UUID entityId) {
        Entity entity = plugin.getServer().getEntity(entityId);
        if (!(entity instanceof LivingEntity living) || !living.isValid()) return;
        Player nearest = null;
        double nearestDistanceSquared = radiusSquared;
        for (Player player : living.getLocation().getNearbyPlayers(radius)) {
            double distanceSquared = player.getLocation().distanceSquared(living.getLocation());
            if (distanceSquared <= nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = player;
            }
        }
        if (nearest != null) living.lookAt(nearest.getEyeLocation(), LookAnchor.EYES);
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() == second.getWorld()
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
