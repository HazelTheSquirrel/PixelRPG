// src/main/java/de/pixelrpg/rpg/player/PlayerClass.java
package de.pixelrpg.rpg.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum PlayerClass {

    NONE(Component.text("None", NamedTextColor.GRAY)),
    WARRIOR(Component.text("Warrior", NamedTextColor.RED)),
    RANGER(Component.text("Ranger", NamedTextColor.GREEN)),
    HEALER(Component.text("Healer", NamedTextColor.YELLOW)),
    MAGE(Component.text("Mage", NamedTextColor.LIGHT_PURPLE)),
    ROGUE(Component.text("Rogue", NamedTextColor.DARK_GRAY));

    private final Component displayName;

    PlayerClass(Component displayName) {
        this.displayName = displayName;
    }

    public Component displayName() {
        return displayName;
    }

    public boolean isNone() {
        return this == NONE;
    }
}