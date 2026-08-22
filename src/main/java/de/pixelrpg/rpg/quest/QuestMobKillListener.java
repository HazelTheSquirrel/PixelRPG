// src/main/java/de/pixelrpg/rpg/quest/QuestMobKillListener.java
package de.pixelrpg.rpg.quest;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class QuestMobKillListener implements Listener {

    private final QuestManager questManager;

    public QuestMobKillListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }

        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        String mobKey = entity.getType().name();
        questManager.progressHuntQuests(killer, mobKey);
        questManager.progressGlobalEvent(mobKey);
    }
}