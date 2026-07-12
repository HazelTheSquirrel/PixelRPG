// src/main/java/de/pixelrpg/rpg/combat/gem/ActiveSkillGemDefinition.java
package de.pixelrpg.rpg.combat.gem;

import de.pixelrpg.rpg.player.PlayerClass;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

public record ActiveSkillGemDefinition(
        String id,
        String displayName,
        String description,
        PlayerClass ownerClass,
        long cooldownMillis,
        Material icon,
        ActiveSkillActionType actionType,
        double baseDamageMultiplier,
        double flatBase,
        double radius,
        int effectDurationTicks,
        boolean applyBurn,
        boolean applySlow,
        boolean applyRoot,
        boolean applyWeaken,
        boolean lifesteal,
        Sound sound,
        Particle particle) 
    {
}