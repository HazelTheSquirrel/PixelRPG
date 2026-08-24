package de.pixelrpg.rpg.quest;

import org.bukkit.entity.LivingEntity;
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

    // Zuständig für HUNT-Quests und globale Kill-Events auf Basis echter Vanilla-Lebewesen.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLivingEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        String entityKey = entity.getType().name();
        questManager.progressHuntQuests(killer, entityKey);
        questManager.progressGlobalEvent(entityKey);
    }
}
