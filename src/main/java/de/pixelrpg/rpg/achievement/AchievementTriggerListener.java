// src/main/java/de/pixelrpg/rpg/achievement/AchievementTriggerListener.java
package de.pixelrpg.rpg.achievement;

import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.api.events.BossDefeatedEvent;
import de.pixelrpg.rpg.api.events.DungeonClearedEvent;
import de.pixelrpg.rpg.api.events.PlayerClassChangeEvent;
import de.pixelrpg.rpg.api.events.PlayerRankUpEvent;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.StatisticType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class AchievementTriggerListener implements Listener {

    private final AchievementManager achievementManager;
    private final StatisticsAPI statisticsAPI;

    public AchievementTriggerListener(AchievementManager achievementManager, StatisticsAPI statisticsAPI) {
        this.achievementManager = achievementManager;
        this.statisticsAPI = statisticsAPI;
    }

    @EventHandler
    public void onRankUp(PlayerRankUpEvent event) {
        achievementManager.checkRankAchievements(event.getPlayer());
    }

    @EventHandler
    public void onClassChange(PlayerClassChangeEvent event) {
        achievementManager.checkClassChosenAchievements(event.getPlayer());
    }

    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        statisticsAPI.recordStatistic(event.getPlayer().getUniqueId(), StatisticType.QUESTS_COMPLETED, 1L);
    }

    @EventHandler
    public void onBossDefeated(BossDefeatedEvent event) {
        for (var uuid : event.getParticipants()) {
            statisticsAPI.recordStatistic(uuid, StatisticType.BOSSES_DEFEATED, 1L);
        }
    }

    @EventHandler
    public void onDungeonCleared(DungeonClearedEvent event) {
        for (var uuid : event.getParticipants()) {
            statisticsAPI.recordStatistic(uuid, StatisticType.DUNGEONS_CLEARED, 1L);
        }
    }
}