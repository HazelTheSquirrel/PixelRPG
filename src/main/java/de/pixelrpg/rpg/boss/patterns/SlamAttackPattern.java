// src/main/java/de/pixelrpg/rpg/boss/patterns/SlamAttackPattern.java
package de.pixelrpg.rpg.boss.patterns;

import de.pixelrpg.rpg.boss.BossAttackPattern;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class SlamAttackPattern implements BossAttackPattern {

    @Override
    public String id() {
        return "SLAM";
    }

    @Override
    public void execute(Plugin plugin, LivingEntity boss) {
        Location center = boss.getLocation();
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 3);
        center.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ATTACK, 1.5f, 0.6f);

        double radius = 6.0;
        double damage = boss.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE) != null
                ? boss.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue() * 1.5
                : 15.0;

        for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player player) {
                player.damage(damage, boss);
                Vector knockback = player.getLocation().toVector()
                        .subtract(center.toVector()).normalize().multiply(1.5).setY(0.5);
                player.setVelocity(knockback);
            }
        }
    }
}