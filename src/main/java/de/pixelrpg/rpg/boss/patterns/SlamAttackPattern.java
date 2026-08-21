package de.pixelrpg.rpg.boss.patterns;

import de.pixelrpg.rpg.boss.BossAttackPattern;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Collection;

public final class SlamAttackPattern implements BossAttackPattern {
    @Override
    public String id() {
        return "SLAM";
    }

    @Override
    public void execute(Plugin plugin, LivingEntity boss, Collection<Player> targets) {
        Location center = boss.getLocation();
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 3);
        center.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ATTACK, 1.5f, 0.6f);

        double radiusSquared = 36.0;
        double damage = boss.getAttribute(Attribute.ATTACK_DAMAGE) != null
                ? boss.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5
                : 15.0;

        for (Player player : targets) {
            if (!player.isOnline() || player.getWorld() != boss.getWorld()) continue;
            if (player.getLocation().distanceSquared(center) > radiusSquared) continue;
            player.damage(damage, boss);
            Vector offset = player.getLocation().toVector().subtract(center.toVector());
            if (offset.lengthSquared() > 0.0001) {
                Vector knockback = offset.normalize().multiply(1.5).setY(0.5);
                player.setVelocity(knockback);
            }
        }
    }
}
