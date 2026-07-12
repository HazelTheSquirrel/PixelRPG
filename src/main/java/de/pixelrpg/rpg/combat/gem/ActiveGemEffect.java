// src/main/java/de/pixelrpg/rpg/combat/gem/ActiveGemEffect.java
package de.pixelrpg.rpg.combat.gem;

import org.bukkit.entity.LivingEntity;

import java.util.List;

public interface ActiveGemEffect {
    List<LivingEntity> execute(GemCastContext context);
}