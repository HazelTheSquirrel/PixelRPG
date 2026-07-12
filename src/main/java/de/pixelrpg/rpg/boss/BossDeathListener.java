// src/main/java/de/pixelrpg/rpg/boss/BossDeathListener.java
package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public final class BossDeathListener implements Listener {

    private final BossManager bossManager;

    public BossDeathListener(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        boolean isWorldBoss = Boolean.TRUE.equals(entity.getPersistentDataContainer()
                .get(RPGKeys.Boss.worldBossMarker(), PersistentDataType.BOOLEAN));

        boolean hasBossId = entity.getPersistentDataContainer()
                .has(RPGKeys.Boss.bossId(), PersistentDataType.STRING);

        if (!isWorldBoss && !hasBossId) {
            return;
        }

        bossManager.onBossDeath(entity.getUniqueId());
    }
}