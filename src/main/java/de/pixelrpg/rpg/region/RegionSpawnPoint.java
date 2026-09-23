package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.World;

/** Immutable explicit hostile-mob spawn point belonging to a region. */
public record RegionSpawnPoint(String mobType, String worldName, double x, double y, double z) {
    public RegionSpawnPoint {
        if (mobType == null || mobType.isBlank()) throw new IllegalArgumentException("mobType must not be blank");
        if (worldName == null || worldName.isBlank()) throw new IllegalArgumentException("worldName must not be blank");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("coordinates must be finite");
    }
    public Location location(World world) {
        return world != null && world.getName().equals(worldName) ? new Location(world, x, y, z) : null;
    }
}
