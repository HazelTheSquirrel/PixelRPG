// src/main/java/de/pixelrpg/rpg/dungeon/DungeonBossDeathListener.java
package de.pixelrpg.rpg.dungeon;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class DungeonBossDeathListener implements Listener {

    private final DungeonInstanceManager instanceManager;

    public DungeonBossDeathListener(DungeonInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String bossDungeonId = entity.getPersistentDataContainer()
                .get(RPGKeys.Boss.bossId(), PersistentDataType.STRING);
        if (bossDungeonId == null) {
            return;
        }

        String instanceIdRaw = entity.getPersistentDataContainer()
                .get(RPGKeys.Dungeon.instanceId(), PersistentDataType.STRING);
        if (instanceIdRaw == null) {
            return;
        }

        instanceManager.onBossDefeated(bossDungeonId, UUID.fromString(instanceIdRaw));
    }
}