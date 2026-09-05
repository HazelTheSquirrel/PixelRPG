package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.core.RPGKeys;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Event-driven NPC look controller. NPCs are evaluated only when nearby players actually move. */
public final class NpcLookTask implements Listener {
    private final Plugin plugin;
    private final double radius;
    private final double radiusSquared;
    private boolean started;

    public NpcLookTask(Plugin plugin, NpcManager npcManager, double radius, int ignoredIntervalTicks) {
        this.plugin = plugin;
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
        HandlerList.unregisterAll(this);
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
        if (!event.hasChangedBlock()) return;
        Location from = event.getFrom();
        Location to = event.getTo();
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
        for (Entity entity : center.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.getPersistentDataContainer().has(RPGKeys.Npc.npcId(), PersistentDataType.STRING)) continue;
            Location npcLocation = living.getLocation();
            if (npcLocation.distanceSquared(center) > radiusSquared) continue;
            updateNpc(living, npcLocation);
        }
    }

    private void updateNpc(LivingEntity living, Location npcLocation) {
        if (!living.isValid()) return;
        Player nearest = null;
        double nearestDistanceSquared = radiusSquared;
        for (Player player : npcLocation.getNearbyPlayers(radius)) {
            double distanceSquared = player.getLocation().distanceSquared(npcLocation);
            if (distanceSquared <= nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = player;
            }
        }
        if (nearest != null) living.lookAt(nearest.getEyeLocation(), LookAnchor.EYES);
    }
}
