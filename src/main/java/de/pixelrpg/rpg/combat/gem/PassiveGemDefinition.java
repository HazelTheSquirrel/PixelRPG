// src/main/java/de/pixelrpg/rpg/combat/gem/PassiveGemDefinition.java
package de.pixelrpg.rpg.combat.gem;

import org.bukkit.Material;

public record PassiveGemDefinition(
        String id,
        String displayName,
        String description,
        Material icon,
        StatModifierType modifierType,
        double value
) {
}