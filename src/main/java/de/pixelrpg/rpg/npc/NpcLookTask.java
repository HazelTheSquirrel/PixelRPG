package de.pixelrpg.rpg.npc;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.entity.LookAnchor;

public final class NpcLookTask {

    private final Plugin plugin;
    private final NpcManager npcManager;
    private final double radius;
    private final int intervalTicks;

    public NpcLookTask(Plugin plugin, NpcManager npcManager, double radius, int intervalTicks) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.radius = radius;
        this.intervalTicks = intervalTicks;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    private void tick() {
        for (var entityUuid : npcManager.getSpawnedEntityUuids()) {
            var entity = Bukkit.getEntity(entityUuid);
            if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isValid()) {
                continue;
            }

            // Nutzt räumliche Nachbarschaftssuche statt alle Weltspieler zu iterieren.
            // Bei vielen NPCs und hoher Spielerzahl deutlich günstiger, da nur
            // Spieler im tatsächlich relevanten Radius betrachtet werden.
            Player nearest = null;
            double nearestDistanceSquared = radius * radius;

            for (var nearby : livingEntity.getNearbyEntities(radius, radius, radius)) {
                if (!(nearby instanceof Player player)) {
                    continue;
                }
                double distanceSquared = player.getLocation().distanceSquared(livingEntity.getLocation());
                if (distanceSquared <= nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearest = player;
                }
            }

            if (nearest != null) {
                livingEntity.lookAt(nearest.getEyeLocation(), LookAnchor.EYES);
            }
        }
    }
}