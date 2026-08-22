package de.pixelrpg.rpg.companion;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

/** Keeps active passive companions near their owning player without enabling combat AI. */
public final class CompanionFollowTask implements Runnable {
    private static final double FOLLOW_DISTANCE_SQUARED = 9.0D;
    private static final double FOLLOW_OFFSET = 2.0D;

    private final Plugin plugin;
    private final Map<UUID, UUID> activeEntities;

    public CompanionFollowTask(Plugin plugin, Map<UUID, UUID> activeEntities) {
        this.plugin = plugin;
        this.activeEntities = activeEntities;
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, UUID> entry : activeEntities.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            Entity companion = plugin.getServer().getEntity(entry.getValue());

            if (player == null || companion == null || !companion.isValid()) {
                activeEntities.remove(entry.getKey(), entry.getValue());
                continue;
            }

            Location playerLocation = player.getLocation();
            if (companion.getWorld() != player.getWorld()) {
                companion.teleport(playerLocation);
                continue;
            }

            if (companion.getLocation().distanceSquared(playerLocation) <= FOLLOW_DISTANCE_SQUARED) continue;

            var direction = playerLocation.getDirection();
            if (direction.lengthSquared() < 0.001D) direction.setX(0.0D).setZ(1.0D);
            direction.normalize().multiply(FOLLOW_OFFSET);

            Location target = playerLocation.clone().subtract(direction);
            target.setY(playerLocation.getY());
            companion.teleport(target);
        }
    }
}
