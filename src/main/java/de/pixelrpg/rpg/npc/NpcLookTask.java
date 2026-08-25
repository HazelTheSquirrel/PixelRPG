package de.pixelrpg.rpg.npc;

import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class NpcLookTask {

    private final Plugin plugin;
    private final NpcManager npcManager;
    private final double radius;
    private final int intervalTicks;
    private BukkitTask task;

    public NpcLookTask(Plugin plugin, NpcManager npcManager, double radius, int intervalTicks) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.radius = Math.max(1.0D, radius);
        this.intervalTicks = Math.max(1, intervalTicks);
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    private void tick() {
        for (var entityUuid : npcManager.getSpawnedEntityUuids()) {
            var entity = Bukkit.getEntity(entityUuid);
            if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isValid()) continue;

            Player nearest = null;
            double nearestDistanceSquared = radius * radius;
            for (var nearby : livingEntity.getNearbyEntities(radius, radius, radius)) {
                if (!(nearby instanceof Player player)) continue;
                double distanceSquared = player.getLocation().distanceSquared(livingEntity.getLocation());
                if (distanceSquared <= nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearest = player;
                }
            }

            if (nearest != null) livingEntity.lookAt(nearest.getEyeLocation(), LookAnchor.EYES);
        }
    }
}