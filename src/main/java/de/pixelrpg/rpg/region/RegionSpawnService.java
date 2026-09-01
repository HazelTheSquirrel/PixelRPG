package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Spawns at explicit region points while keeping one managed hostile mob per point. */
public final class RegionSpawnService {
    private static final long CHECK_INTERVAL_TICKS = 100L;
    private static final long RESPAWN_DELAY_TICKS = 200L;
    private static final double PLAYER_RANGE = 64.0D;

    private final JavaPlugin plugin;
    private final RegionManager regions;
    private final NamespacedKey markerKey;
    private final java.util.Map<String, Long> nextSpawnTicks = new java.util.HashMap<>();
    private BukkitTask task;

    public RegionSpawnService(JavaPlugin plugin, RegionManager regions) {
        this.plugin = plugin;
        this.regions = regions;
        this.markerKey = new NamespacedKey(plugin, "region_spawn_point");
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) task.cancel();
        nextSpawnTicks.clear();
    }

    /** Identifies mobs created by the explicit region spawn-point system. */
    public boolean isManagedSpawn(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING);
    }

    private void tick() {
        long now = plugin.getServer().getCurrentTick();
        Set<String> activePoints = new HashSet<>();

        for (PixelRegion region : regions.all()) {
            if (region.spawnPoints().isEmpty()) continue;
            World world = plugin.getServer().getWorld(region.worldName());
            if (world == null) continue;

            int index = 0;
            for (RegionSpawnPoint point : region.spawnPoints()) {
                Location location = point.location(world);
                if (location == null) { index++; continue; }
                String pointKey = region.id() + ":" + index;
                activePoints.add(pointKey);
                if (!hasNearbyPlayer(location)) { index++; continue; }
                if (hasManagedMob(location, pointKey)) { index++; continue; }

                long next = nextSpawnTicks.getOrDefault(pointKey, 0L);
                if (now < next) { index++; continue; }

                EntityType type = SpawnMobType.resolve(point.mobType());
                if (type == null || type.getEntityClass() == null || !Monster.class.isAssignableFrom(type.getEntityClass())) {
                    index++;
                    continue;
                }

                Entity entity = world.spawnEntity(location, type);
                entity.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, pointKey);
                nextSpawnTicks.put(pointKey, now + RESPAWN_DELAY_TICKS);
                index++;
            }
        }

        nextSpawnTicks.keySet().removeIf(key -> !activePoints.contains(key));
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
}
