package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.World;

/** Immutable hostile-mob spawn point belonging to a PixelRPG region. */
public record RegionSpawnPoint(String mobType, String worldName, double x, double y, double z) {
    public RegionSpawnPoint {
        if (mobType == null || mobType.isBlank()) throw new IllegalArgumentException("mobType must not be blank");
        if (worldName == null || worldName.isBlank()) throw new IllegalArgumentException("worldName must not be blank");
    }

    public Location location(World world) {
        if (world == null || !world.getName().equals(worldName)) return null;
        return new Location(world, x, y, z);
    }
}
