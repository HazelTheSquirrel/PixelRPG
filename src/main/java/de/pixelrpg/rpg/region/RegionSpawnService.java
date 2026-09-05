package de.pixelrpg.rpg.region;

import de.pixelrpg.rpg.core.WakeScheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/** Event-driven explicit region spawns. Spawn points are evaluated only after relevant world state changes. */
public final class RegionSpawnService implements Listener {
    private static final long CHECK_INTERVAL_TICKS = 100L;
    private static final long RESPAWN_DELAY_TICKS = 200L;
    private static final double PLAYER_RANGE = 64.0D;
    private static final int PLAYER_RANGE_CHUNKS = 4;

    private final JavaPlugin plugin;
    private final RegionManager regions;
    private final org.bukkit.NamespacedKey markerKey;
    private final java.util.Map<String, Long> nextSpawnTicks = new java.util.HashMap<>();
    private final WakeScheduler<String> pointScheduler;
    private boolean started;

    public RegionSpawnService(JavaPlugin plugin, RegionManager regions) {
        this.plugin = plugin;
        this.regions = regions;
        this.markerKey = new org.bukkit.NamespacedKey(plugin, "region_spawn_point");
        this.pointScheduler = new WakeScheduler<>(plugin);
    }

    public void start() {
        if (started) return;
        started = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        long initialReadyTick = plugin.getServer().getCurrentTick() + CHECK_INTERVAL_TICKS;
        for (RegionManager.SpawnPointRef reference : allSpawnPoints()) {
            nextSpawnTicks.putIfAbsent(pointKey(reference), initialReadyTick);
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) evaluateAround(player.getLocation());
    }

    public void stop() {
        if (!started) return;
        HandlerList.unregisterAll(this);
        pointScheduler.clear();
        nextSpawnTicks.clear();
        started = false;
    }

    /** Identifies mobs created by the explicit region spawn-point system. */
    public boolean isManagedSpawn(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING);
    }

    // A player entering or joining the world can activate nearby explicit spawn points immediately after the normal spawn interval.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        evaluateAround(event.getPlayer().getLocation());
    }

    // Spawn processing is driven by block-boundary movement instead of a global periodic player scan.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        evaluateAround(event.getFrom());
        evaluateAround(event.getTo());
    }

    // A world change activates spawn points in the player's new area without scanning unrelated regions.
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        evaluateAround(event.getPlayer().getLocation());
    }

    // A managed spawn death is the exact state change that can require a respawn, so only that point is woken.
    @EventHandler
    public void onManagedSpawnDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        String pointKey = entity.getPersistentDataContainer().get(markerKey, PersistentDataType.STRING);
        if (pointKey == null || pointKey.isBlank()) return;
        long now = plugin.getServer().getCurrentTick();
        long next = nextSpawnTicks.getOrDefault(pointKey, now);
        long delay = Math.max(1L, next - now);
        pointScheduler.cancel(pointKey);
        pointScheduler.wakeLater(pointKey, delay, () -> evaluatePointByKey(pointKey));
    }

    // A disconnect only rechecks the affected area; no global spawn scan is performed.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        evaluateAround(event.getPlayer().getLocation());
    }

    private void evaluateAround(Location center) {
        if (!started || center == null || center.getWorld() == null) return;
        double radiusSquared = PLAYER_RANGE * PLAYER_RANGE;
        for (RegionManager.SpawnPointRef reference : regions.spawnPointsNear(center, PLAYER_RANGE_CHUNKS)) {
            RegionSpawnPoint point = reference.point();
            Location location = point.location(center.getWorld());
            if (location == null || location.distanceSquared(center) > radiusSquared) continue;
            evaluatePoint(reference, pointKey(reference), center, location);
        }
    }

    private void evaluatePointByKey(String pointKey) {
        RegionManager.SpawnPointRef reference = findPoint(pointKey);
        if (reference == null) return;
        RegionSpawnPoint point = reference.point();
        World world = plugin.getServer().getWorld(point.worldName());
        if (world == null) return;
        Location location = point.location(world);
        if (location == null || !hasNearbyPlayer(location)) return;
        evaluatePoint(reference, pointKey, null, location);
    }

    private void evaluatePoint(RegionManager.SpawnPointRef reference, String pointKey, Location center, Location location) {
        long now = plugin.getServer().getCurrentTick();
        long next = nextSpawnTicks.getOrDefault(pointKey, now);
        if (now < next) {
            schedulePointCheck(pointKey, next - now);
            return;
        }
        if (center == null && !hasNearbyPlayer(location)) return;
        if (center != null && location.distanceSquared(center) > PLAYER_RANGE * PLAYER_RANGE) return;
        if (hasManagedMob(location, pointKey)) return;

        EntityType type = SpawnMobType.resolve(reference.point().mobType());
        if (type == null || type.getEntityClass() == null || !Monster.class.isAssignableFrom(type.getEntityClass())) return;

        World world = location.getWorld();
        if (world == null) return;
        Entity entity = world.spawnEntity(location, type);
        entity.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, pointKey);
        nextSpawnTicks.put(pointKey, now + RESPAWN_DELAY_TICKS);
        pointScheduler.cancel(pointKey);
    }

    private void schedulePointCheck(String pointKey, long delay) {
        pointScheduler.cancel(pointKey);
        pointScheduler.wakeLater(pointKey, Math.max(1L, delay), () -> evaluatePointByKey(pointKey));
    }

    private RegionManager.SpawnPointRef findPoint(String pointKey) {
        int separator = pointKey.indexOf(':');
        if (separator <= 0 || separator == pointKey.length() - 1) return null;
        UUID regionId;
        int index;
        try {
            regionId = UUID.fromString(pointKey.substring(0, separator));
            index = Integer.parseInt(pointKey.substring(separator + 1));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        if (index < 0) return null;
        PixelRegion region = regions.get(regionId).orElse(null);
        if (region == null || index >= region.spawnPoints().size()) return null;
        return new RegionManager.SpawnPointRef(regionId, index, region.spawnPoints().get(index));
    }

    private Iterable<RegionManager.SpawnPointRef> allSpawnPoints() {
        java.util.List<RegionManager.SpawnPointRef> result = new java.util.ArrayList<>();
        for (PixelRegion region : regions.all()) {
            for (int index = 0; index < region.spawnPoints().size(); index++) {
                result.add(new RegionManager.SpawnPointRef(region.id(), index, region.spawnPoints().get(index)));
            }
        }
        return result;
    }

    private boolean hasNearbyPlayer(Location location) {
        for (Player player : location.getNearbyPlayers(PLAYER_RANGE)) {
            if (player.isOnline()) return true;
        }
        return false;
    }

    private boolean hasManagedMob(Location location, String pointKey) {
        for (Entity entity : location.getNearbyEntities(1.5D, 2.5D, 1.5D)) {
            if (!(entity instanceof Monster)) continue;
            String marker = entity.getPersistentDataContainer().get(markerKey, PersistentDataType.STRING);
            if (pointKey.equals(marker)) return true;
        }
        return false;
    }

    private static String pointKey(RegionManager.SpawnPointRef reference) {
        return reference.regionId() + ":" + reference.index();
    }
}
