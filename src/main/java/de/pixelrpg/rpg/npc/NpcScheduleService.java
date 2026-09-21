package de.pixelrpg.rpg.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcScheduleService {
    private static final double MOVEMENT_SPEED = 0.18D;
    private final Plugin plugin;
    private final NpcManager npcManager;
    private final NpcProfileStore profileStore;
    private final NamespacedKey activityKey;
    private final NpcScheduleStore scheduleStore;
    private final Map<UUID, Location> destinations = new ConcurrentHashMap<>();
    private BukkitTask task;

    public NpcScheduleService(Plugin plugin, NpcManager npcManager, NpcProfileStore profileStore) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.npcManager = Objects.requireNonNull(npcManager, "npcManager");
        this.profileStore = Objects.requireNonNull(profileStore, "profileStore");
        this.activityKey = new NamespacedKey(plugin, "npc_activity");
        this.scheduleStore = new NpcScheduleStore(plugin);
        this.scheduleStore.load();
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    private void tick() {
        for (var uuid : npcManager.getSpawnedEntityUuids()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity == null || !entity.isValid()) {
                destinations.remove(uuid);
                continue;
            }

            npcManager.getByEntity(uuid).flatMap(npc -> profileStore.get(npc.id())).ifPresent(profile -> {
                String activity = activity(profile.schedule(), entity.getWorld().getTime());
                String previous = entity.getPersistentDataContainer().get(activityKey, PersistentDataType.STRING);
                if (!activity.equals(previous)) {
                    entity.getPersistentDataContainer().set(activityKey, PersistentDataType.STRING, activity);
                    npcManager.getByEntity(uuid).ifPresent(npc ->
                            destinations.put(uuid, scheduleStore.resolve(profile.schedule(), activity, npc.location())));
                }

                Location destination = destinations.get(uuid);
                if (destination != null) moveTowards(entity, destination);
            });
        }
    }

    private void moveTowards(Entity entity, Location destination) {
        if (!destination.getWorld().equals(entity.getWorld())) return;
        Location current = entity.getLocation();
        Vector delta = destination.toVector().subtract(current.toVector());
        double distance = delta.length();
        if (distance <= MOVEMENT_SPEED) {
            entity.teleport(destination.clone().setDirection(current.getDirection()));
            return;
        }

        Vector step = delta.normalize().multiply(MOVEMENT_SPEED);
        Location next = current.clone().add(step);
        if (next.getBlock().isPassable() && next.clone().add(0, 1, 0).getBlock().isPassable()) {
            entity.teleport(next);
        }
    }

    private String activity(String schedule, long time) {
        long hour = (time % 24000L) / 1000L;
        if (hour < 6L) return "sleep";
        if (hour < 12L) return "work";
        if (hour < 18L) return "social";
        if (hour < 22L) return schedule.equalsIgnoreCase("guard") ? "guard" : "social";
        return "sleep";
    }

    public void shutdown() {
        if (task != null) task.cancel();
        task = null;
        destinations.clear();
    }
}
