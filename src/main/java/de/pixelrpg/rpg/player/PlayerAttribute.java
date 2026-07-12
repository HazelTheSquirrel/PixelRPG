// src/main/java/de/pixelrpg/rpg/player/PlayerAttribute.java (VOLLSTÄNDIG, ersetzt alte Datei)
package de.pixelrpg.rpg.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum PlayerAttribute {

    VITALITY(Component.text("Vitality", NamedTextColor.RED), 7),
    AGILITY(Component.text("Agility", NamedTextColor.GREEN), 7),
    PRECISION(Component.text("Precision", NamedTextColor.YELLOW), 7),
    RANGE(Component.text("Range", NamedTextColor.AQUA), 7),
    TOUGHNESS(Component.text("Toughness", NamedTextColor.GRAY), 7),
    SOULVIEW(Component.text("Soulview", NamedTextColor.DARK_PURPLE), 1),
    ELYTRA_PERMIT(Component.text("Elytra Permit", NamedTextColor.LIGHT_PURPLE), 1);

    private final Component displayName;
    private final int maxPoints;

    PlayerAttribute(Component displayName, int maxPoints) {
        this.displayName = displayName;
        this.maxPoints = maxPoints;
    }

    public Component displayName() {
        return displayName;
    }

    public int getMaxPoints() {
        return maxPoints;
    }
}