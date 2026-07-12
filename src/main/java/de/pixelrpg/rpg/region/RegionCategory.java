// src/main/java/de/pixelrpg/rpg/region/RegionCategory.java
package de.pixelrpg.rpg.region;

import net.kyori.adventure.text.format.NamedTextColor;

public enum RegionCategory {

    WILDERNESS(NamedTextColor.GREEN),
    SETTLEMENT(NamedTextColor.GOLD),
    DUNGEON(NamedTextColor.DARK_RED),
    LANDMARK(NamedTextColor.AQUA);

    private final NamedTextColor color;

    RegionCategory(NamedTextColor color) {
        this.color = color;
    }

    public NamedTextColor getColor() {
        return color;
    }
}