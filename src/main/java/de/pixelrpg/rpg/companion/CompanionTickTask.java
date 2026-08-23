package de.pixelrpg.rpg.companion;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

/** Ticks active companions and delegates Mannequin companions to their dedicated controller. */
public final class CompanionTickTask implements Runnable {
    private static final double TELEPORT_DISTANCE_SQUARED = 900.0D;
    private final Plugin plugin;
    private final Map<UUID, UUID> activeEntities;
    private final MannequinCompanionController mannequinController;

    public CompanionTickTask(Plugin plugin, Map<UUID, UUID> activeEntities) {
        this.plugin = plugin;
        this.activeEntities = activeEntities;
        this.mannequinController = new MannequinCompanionController(plugin);
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, UUID> entry : activeEntities.entrySet()) {
            Player owner = plugin.getServer().getPlayer(entry.getKey());
            Entity entity = plugin.getServer().getEntity(entry.getValue());
            if (owner == null || !owner.isOnline() || entity == null || !entity.isValid()) {
                activeEntities.remove(entry.getKey(), entry.getValue());
                continue;
            }

            if (entity instanceof Mannequin mannequin) {
                mannequinController.tick(owner, mannequin);
                continue;
            }

            if (entity.getWorld() != owner.getWorld()) {
                entity.teleport(owner.getLocation().clone().add(1.0D, 0.0D, 1.0D));
                continue;
            }

            double distanceSquared = entity.getLocation().distanceSquared(owner.getLocation());
            if (distanceSquared > TELEPORT_DISTANCE_SQUARED) {
                entity.teleport(owner.getLocation().clone().add(1.0D, 0.0D, 1.0D));
                entity.setVelocity(new Vector());
                continue;
            }

            if (distanceSquared > 9.0D && entity instanceof Mob mob) {
                Location target = owner.getLocation().clone().add(0.0D, 0.0D, -1.5D);
                mob.setAware(true);
                mob.getPathfinder().moveTo(target, owner.isSprinting() ? 1.35D : 1.10D);
            } else if (distanceSquared > 9.0D && entity instanceof LivingEntity living) {
                Vector delta = owner.getLocation().toVector().subtract(living.getLocation().toVector());
                delta.setY(0.0D);
                if (delta.lengthSquared() > 0.04D) living.setVelocity(delta.normalize().multiply(owner.isSprinting() ? 0.34D : 0.28D));
            }
        }
    }
}
