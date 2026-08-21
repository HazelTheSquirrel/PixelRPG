package de.pixelrpg.rpg.combat.scaling;

import org.bukkit.Location;

public interface RegionDangerProvider {
    int getMinLevel(Location location);
    int getMaxLevel(Location location);
}
