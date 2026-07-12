// src/main/java/de/pixelrpg/rpg/combat/scaling/DefaultRegionDangerProvider.java
package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.core.Rank;
import org.bukkit.Location;

public final class DefaultRegionDangerProvider implements RegionDangerProvider {

    @Override
    public Rank getMinRank(Location location) {
        return Rank.F;
    }

    @Override
    public Rank getMaxRank(Location location) {
        return Rank.S;
    }
}