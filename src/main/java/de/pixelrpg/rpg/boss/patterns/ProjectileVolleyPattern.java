// src/main/java/de/pixelrpg/rpg/boss/patterns/ProjectileVolleyPattern.java
package de.pixelrpg.rpg.boss.patterns;

import de.pixelrpg.rpg.boss.BossAttackPattern;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class ProjectileVolleyPattern implements BossAttackPattern {

    @Override
    public String id() {
        return "PROJECTILE_VOLLEY";
    }

    @Override
    public void execute(Plugin plugin, LivingEntity boss) {
        Location eye = boss.getEyeLocation();
        boss.getWorld().playSound(eye, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.8f);

        Player nearestTarget = boss.getWorld().getNearbyEntities(boss.getLocation(), 30, 30, 30).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .min((a, b) -> Double.compare(
                        a.getLocation().distanceSquared(boss.getLocation()),
                        b.getLocation().distanceSquared(boss.getLocation())))
                .orElse(null);

        if (nearestTarget == null) {
            return;
        }

        Vector direction = nearestTarget.getLocation().toVector().subtract(eye.toVector()).normalize();

        for (int i = -1; i <= 1; i++) {
            Vector spread = direction.clone().add(new Vector(i * 0.15, 0, i * 0.15));
            Fireball fireball = boss.launchProjectile(Fireball.class, spread.multiply(1.2));
            fireball.setYield(0.0f);
            fireball.setIsIncendiary(false);
        }
    }
}