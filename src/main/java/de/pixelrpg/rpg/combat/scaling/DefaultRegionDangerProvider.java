package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.core.Level;
import org.bukkit.Location;

public final class DefaultRegionDangerProvider implements RegionDangerProvider {
    @Override public int getMinLevel(Location location) { return Level.MIN_LEVEL; }
    @Override public int getMaxLevel(Location location) { return Level.MAX_NORMAL_LEVEL; }
}
