// src/main/java/de/pixelrpg/rpg/boss/BossAttackPattern.java
package de.pixelrpg.rpg.boss;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public interface BossAttackPattern {

    String id();

    void execute(Plugin plugin, LivingEntity boss);
}