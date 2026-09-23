package de.pixelrpg.rpg.boss;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public interface BossAttackPattern {
    String id();

    void execute(Plugin plugin, LivingEntity boss, Collection<Player> targets);
}
