// src/main/java/de/pixelrpg/rpg/region/CuboidBounds.java
package de.pixelrpg.rpg.region;

import org.bukkit.Location;

public record CuboidBounds(
        String worldName,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ
) {

    public static CuboidBounds fromCorners(Location a, Location b) {
        return new CuboidBounds(
                a.getWorld().getName(),
                Math.min(a.getBlockX(), b.getBlockX()),
                Math.min(a.getBlockY(), b.getBlockY()),
                Math.min(a.getBlockZ(), b.getBlockZ()),
                Math.max(a.getBlockX(), b.getBlockX()),
                Math.max(a.getBlockY(), b.getBlockY()),
                Math.max(a.getBlockZ(), b.getBlockZ())
        );
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public long volume() {
        long dx = (long) (maxX - minX) + 1L;
        long dy = (long) (maxY - minY) + 1L;
        long dz = (long) (maxZ - minZ) + 1L;
        return dx * dy * dz;
    }
}