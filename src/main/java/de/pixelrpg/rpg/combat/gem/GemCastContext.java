// src/main/java/de/pixelrpg/rpg/combat/gem/GemCastContext.java
package de.pixelrpg.rpg.combat.gem;

import de.pixelrpg.rpg.stats.StatEngine;
import org.bukkit.entity.Player;

public record GemCastContext(
        Player caster,
        StatEngine.CachedStats stats,
        SupportGemModifiers modifiers
) {

    public double amplify(double baseDamage) {
        return baseDamage * modifiers.damageMultiplier();
    }
}