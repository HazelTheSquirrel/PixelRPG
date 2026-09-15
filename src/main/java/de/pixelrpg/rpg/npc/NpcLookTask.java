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

/** Event-driven NPC look and per-player native name visibility controller. */
public final class NpcLookTask implements Listener {
    private static final double INTERACTION_RADIUS = 5.0D;
    private static final double INTERACTION_RADIUS_SQUARED = INTERACTION_RADIUS * INTERACTION_RADIUS;

    private final Plugin plugin;
    private final NpcManager npcManager;
    private final NpcNameVisibilityService nameVisibilityService;
    private boolean started;

    public NpcLookTask(Plugin plugin, NpcManager npcManager, double ignoredLookRadius, double ignoredNameplateRadius, int ignoredIntervalTicks) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.nameVisibilityService = new NpcNameVisibilityService(plugin.getLogger());
    }

    public void start() {
        if (started) return;
        started = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            refreshAllFor(player);
        }
    }

    public void stop() {
        if (!started) return;
        HandlerList.unregisterAll(this);
        started = false;
    }

    // A player joining must receive native NPC names only for mannequins within the interaction radius.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refreshAllFor(event.getPlayer());
    }

    // NPC look direction and native name visibility update when the player crosses a block boundary.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        updateAround(event.getFrom());
        updateAround(event.getTo());
    }

    // A disconnect can change which player is the nearest active look target.
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

    private void refreshAllFor(Player player) {
        for (RPGNpc npc : npcManager.getAll()) {
            Entity mannequin = getMannequin(npc);
            if (mannequin == null) continue;
            updateNpc(npc, mannequin);
        }
    }

    private void updateNpc(LivingEntity living) {
        String npcId = living.getPersistentDataContainer().get(RPGKeys.Npc.npcId(), PersistentDataType.STRING);
        if (npcId == null) return;

        RPGNpc npc = npcManager.getById(npcId).orElse(null);
        if (npc == null) return;
        updateNpc(npc, living);
    }

    private void updateNpc(RPGNpc npc, Entity mannequin) {
        if (!mannequin.isValid()) return;

        Location npcLocation = mannequin.getLocation();
        Player nearestPlayer = null;
        double nearestDistanceSquared = INTERACTION_RADIUS_SQUARED;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld() != npcLocation.getWorld()) continue;

            double distanceSquared = player.getLocation().distanceSquared(npcLocation);
            boolean inRange = distanceSquared <= INTERACTION_RADIUS_SQUARED;
            nameVisibilityService.setVisible(player, mannequin, inRange);

            if (inRange && distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestPlayer = player;
            }
        }

        if (nearestPlayer != null && mannequin instanceof LivingEntity living) {
            living.lookAt(nearestPlayer.getEyeLocation(), LookAnchor.EYES);
        }
    }

    private Entity getMannequin(RPGNpc npc) {
        for (java.util.UUID uuid : npcManager.getSpawnedEntityUuids()) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity == null || !entity.isValid()) continue;
            if (!(entity instanceof LivingEntity)) continue;

            String npcId = entity.getPersistentDataContainer().get(RPGKeys.Npc.npcId(), PersistentDataType.STRING);
            if (npc.id().equals(npcId)) return entity;
        }
        return null;
    }
}
