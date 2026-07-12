// src/main/java/de/pixelrpg/rpg/boss/patterns/SummonAddsPattern.java
package de.pixelrpg.rpg.boss.patterns;

import de.pixelrpg.rpg.boss.BossAttackPattern;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class SummonAddsPattern implements BossAttackPattern {

    @Override
    public String id() {
        return "SUMMON_ADDS";
    }

    @Override
    public void execute(Plugin plugin, LivingEntity boss) {
        Location center = boss.getLocation();
        center.getWorld().spawnParticle(Particle.SOUL, center.clone().add(0, 1, 0), 30, 1.0, 1.0, 1.0);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.2f);

        Integer bossRankOrdinal = boss.getPersistentDataContainer()
                .get(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER);
        int rankOrdinal = bossRankOrdinal != null ? bossRankOrdinal : 0;

        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2 / 3) * i;
            Location spawnLoc = center.clone().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4);

            LivingEntity add = (LivingEntity) center.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            var hpAttribute = add.getAttribute(Attribute.MAX_HEALTH);
            if (hpAttribute != null) {
                double hp = 20.0 + rankOrdinal * 15.0;
                hpAttribute.setBaseValue(hp);
                add.setHealth(hp);
            }
            add.getPersistentDataContainer().set(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER, rankOrdinal);
        }
    }
}