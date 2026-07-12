// src/main/java/de/pixelrpg/rpg/combat/gem/SkillGemDefinition.java
package de.pixelrpg.rpg.combat.gem;

import de.pixelrpg.rpg.player.PlayerClass;
import org.bukkit.Material;

import java.util.Optional;

public record SkillGemDefinition(
        String id,
        String displayName,
        String description,
        GemType type,
        Optional<PlayerClass> ownerClass,
        long baseCooldownMillis,
        Material icon
) {
}