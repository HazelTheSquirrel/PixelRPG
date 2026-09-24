package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.api.events.BossDefeatedEvent;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.StatisticType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class QuestBossStatisticListener implements Listener {
    private final StatisticsAPI statistics;
    public QuestBossStatisticListener(StatisticsAPI statistics){this.statistics=statistics;}
    // Zuständig für das Hochzählen der QUESTS_COMPLETED-Statistik bei Quest-Abschluss.
    @EventHandler public void onQuestCompleted(QuestCompletedEvent event){statistics.recordStatistic(event.getPlayer().getUniqueId(),StatisticType.QUESTS_COMPLETED,1L);}
    // Zuständig für das Hochzählen der BOSSES_DEFEATED-Statistik aller Teilnehmer bei Boss-Kill.
    @EventHandler public void onBossDefeated(BossDefeatedEvent event){for(var uuid:event.getParticipants())statistics.recordStatistic(uuid,StatisticType.BOSSES_DEFEATED,1L);}
}
