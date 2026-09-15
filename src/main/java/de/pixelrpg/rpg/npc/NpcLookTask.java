package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.core.RPGKeys;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Event-driven NPC look and per-player native name visibility controller. */
public final class NpcLookTask implements Listener {
    private static final double INTERACTION_RADIUS = 5.0D;
    private static final double INTERACTION_RADIUS_SQUARED = INTERACTION_RADIUS * INTERACTION_RADIUS;

    private final Plugin plugin;
    private final NpcManager npcManager;
    private final NpcNameVisibilityService nameVisibilityService;
    private final Map<UUID, Set<String>> visibleNpcIdsByPlayer = new ConcurrentHashMap<>();
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
            refreshPlayer(player);
        }
    }

    public void stop() {
        if (!started) return;
        HandlerList.unregisterAll(this);
        visibleNpcIdsByPlayer.clear();
        started = false;
    }

    // A player joining must receive native NPC names only for mannequins within the interaction radius.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refreshPlayer(event.getPlayer());
    }

    // NPC name visibility and look direction update when the player crosses a block boundary.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        refreshPlayer(event.getPlayer());
        updateAround(event.getFrom());
        updateAround(event.getTo());
    }

    // A disconnect removes that player's tracked NPC-name state and can change the nearest look target.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        visibleNpcIdsByPlayer.remove(event.getPlayer().getUniqueId());
        updateAround(event.getPlayer().getLocation());
    }

    // A newly tracked entity may have just been sent to a player, so apply native name visibility after tracking.
    @EventHandler
    public void onTrack(PlayerTrackEntityEvent event) {
        if (!isNpc(event.getEntity())) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshPlayer(event.getPlayer()));
    }

    private void refreshPlayer(Player player) {
        if (!player.isOnline()) return;

        Set<String> visibleNow = new HashSet<>();
        for (RPGNpc npc : npcManager.getAll()) {
            Entity mannequin = getMannequin(npc);
            if (mannequin == null || mannequin.getWorld() != player.getWorld()) continue;

            boolean inRange = player.getLocation().distanceSquared(mannequin.getLocation()) <= INTERACTION_RADIUS_SQUARED;
            if (inRange) visibleNow.add(npc.id());
            nameVisibilityService.setVisible(player, mannequin, inRange);
        }

        Set<String> previouslyVisible = visibleNpcIdsByPlayer.put(player.getUniqueId(), Set.copyOf(visibleNow));
        if (previouslyVisible == null) return;

        for (String npcId : previouslyVisible) {
            if (visibleNow.contains(npcId)) continue;
            RPGNpc npc = npcManager.getById(npcId).orElse(null);
            Entity mannequin = getMannequin(npc);
            if (mannequin != null) nameVisibilityService.setVisible(player, mannequin, false);
        }
    }

    private void updateAround(Location center) {
        if (center == null || center.getWorld() == null) return;

        for (Entity entity : center.getNearbyEntities(INTERACTION_RADIUS, INTERACTION_RADIUS, INTERACTION_RADIUS)) {
            if (!(entity instanceof LivingEntity living) || !isNpc(living)) continue;
            updateLook(living);
        }
    }

    private void updateLook(LivingEntity living) {
        String npcId = living.getPersistentDataContainer().get(RPGKeys.Npc.npcId(), PersistentDataType.STRING);
        if (npcId == null || npcManager.getById(npcId).isEmpty()) return;

        Location npcLocation = living.getLocation();
        Player nearestPlayer = null;
        double nearestDistanceSquared = INTERACTION_RADIUS_SQUARED;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld() != npcLocation.getWorld()) continue;
            double distanceSquared = player.getLocation().distanceSquared(npcLocation);
            if (distanceSquared <= nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestPlayer = player;
            }
        }

        if (nearestPlayer != null) {
            living.lookAt(nearestPlayer.getEyeLocation(), LookAnchor.EYES);
        }
    }

    private boolean isNpc(Entity entity) {
        return entity instanceof LivingEntity living
                && living.getPersistentDataContainer().has(RPGKeys.Npc.npcId(), PersistentDataType.STRING);
    }

    private Entity getMannequin(RPGNpc npc) {
        if (npc == null) return null;

        for (UUID uuid : npcManager.getSpawnedEntityUuids()) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity == null || !entity.isValid()) continue;
            if (!(entity instanceof LivingEntity)) continue;

            String npcId = entity.getPersistentDataContainer().get(RPGKeys.Npc.npcId(), PersistentDataType.STRING);
            if (npc.id().equals(npcId)) return entity;
        }
        return null;
    }
}
