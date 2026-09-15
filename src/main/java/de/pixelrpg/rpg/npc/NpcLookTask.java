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

/** Event-driven NPC look and per-player visibility controller. */
public final class NpcLookTask implements Listener {
    private static final double INTERACTION_RADIUS = 5.0D;
    private static final double INTERACTION_RADIUS_SQUARED = INTERACTION_RADIUS * INTERACTION_RADIUS;

    private final Plugin plugin;
    private boolean started;

    public NpcLookTask(Plugin plugin, NpcManager npcManager, double ignoredLookRadius, double ignoredNameplateRadius, int ignoredIntervalTicks) {
        this.plugin = plugin;
    }

    public void start() {
        if (started) return;
        started = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updateAround(player.getLocation());
        }
    }

    public void stop() {
        if (!started) return;
        HandlerList.unregisterAll(this);
        started = false;
    }

    // A player joining can immediately enter the interaction radius of nearby NPCs.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateAround(event.getPlayer().getLocation());
    }

    // NPC visibility and look direction update when the player crosses a block boundary.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        updateAround(event.getFrom());
        updateAround(event.getTo());
    }

    // A disconnect can change NPC visibility and the active look target for nearby players.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        updateAround(event.getPlayer().getLocation());
    }

    private void updateAround(Location center) {
        if (center == null || center.getWorld() == null) return;

        for (Entity entity : center.getNearbyEntities(INTERACTION_RADIUS, INTERACTION_RADIUS, INTERACTION_RADIUS)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.getPersistentDataContainer().has(RPGKeys.Npc.npcId(), PersistentDataType.STRING)) continue;
            updateNpc(living);
        }
    }

    private void updateNpc(LivingEntity living) {
        if (!living.isValid()) return;

        Location npcLocation = living.getLocation();
        Player nearestPlayer = null;
        double nearestDistanceSquared = INTERACTION_RADIUS_SQUARED;
        boolean anyPlayerInRange = false;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld() != npcLocation.getWorld()) continue;

            double distanceSquared = player.getLocation().distanceSquared(npcLocation);
            boolean inRange = distanceSquared <= INTERACTION_RADIUS_SQUARED;

            if (inRange) {
                anyPlayerInRange = true;
                player.showEntity(plugin, living);
                if (distanceSquared < nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearestPlayer = player;
                }
            } else {
                player.hideEntity(plugin, living);
            }
        }

        // The configured NPC name is only rendered while at least one viewer is inside the radius.
        living.setCustomNameVisible(anyPlayerInRange);

        if (nearestPlayer != null) {
            living.lookAt(nearestPlayer.getEyeLocation(), LookAnchor.EYES);
        }
    }
}
