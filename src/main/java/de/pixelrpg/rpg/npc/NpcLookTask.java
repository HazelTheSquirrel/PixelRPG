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

/** Event-driven NPC look and nameplate controller. NPCs are evaluated when nearby players cross block boundaries. */
public final class NpcLookTask implements Listener {
    private final Plugin plugin;
    private final double lookRadius;
    private final double lookRadiusSquared;
    private final double nameplateRadius;
    private boolean started;

    public NpcLookTask(Plugin plugin, NpcManager npcManager, double lookRadius, double nameplateRadius, int ignoredIntervalTicks) {
        this.plugin = plugin;
        this.lookRadius = Math.max(1.0D, lookRadius);
        this.lookRadiusSquared = this.lookRadius * this.lookRadius;
        this.nameplateRadius = Math.max(this.lookRadius, nameplateRadius);
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

    // NPC look and nameplate state wakes only when the player crosses a block boundary near the NPC.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        updateAround(event.getFrom());
        updateAround(event.getTo());
    }

    // A disconnect can change the nearest-player target and nameplate state of NPCs in the player's old area.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        updateAround(event.getPlayer().getLocation());
    }

    private void updateAround(Location center) {
        if (center == null || center.getWorld() == null) return;
        double searchRadius = Math.max(lookRadius, nameplateRadius);
        for (Entity entity : center.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.getPersistentDataContainer().has(RPGKeys.Npc.npcId(), PersistentDataType.STRING)) continue;
            Location npcLocation = living.getLocation();
            double distanceSquared = npcLocation.distanceSquared(center);
            if (distanceSquared > searchRadius * searchRadius) continue;
            updateNpc(living, npcLocation);
        }
    }

    private void updateNpc(LivingEntity living, Location npcLocation) {
        if (!living.isValid()) return;

        Player nearestLookTarget = null;
        double nearestLookDistanceSquared = lookRadiusSquared;
        boolean playerInNameplateRange = false;

        for (Player player : npcLocation.getNearbyPlayers(nameplateRadius)) {
            double distanceSquared = player.getLocation().distanceSquared(npcLocation);
            if (distanceSquared <= nameplateRadius * nameplateRadius) {
                playerInNameplateRange = true;
            }
            if (distanceSquared <= nearestLookDistanceSquared) {
                nearestLookDistanceSquared = distanceSquared;
                nearestLookTarget = player;
            }
        }

        living.setCustomNameVisible(playerInNameplateRange);
        if (nearestLookTarget != null) living.lookAt(nearestLookTarget.getEyeLocation(), LookAnchor.EYES);
    }
}
