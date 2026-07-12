// src/main/java/de/pixelrpg/rpg/combat/scaling/RegionDangerProvider.java
package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.core.Rank;
import org.bukkit.Location;

public interface RegionDangerProvider {

    Rank getMinRank(Location location);

    Rank getMaxRank(Location location);
}