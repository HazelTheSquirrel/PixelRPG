// src/main/java/de/pixelrpg/rpg/leaderboard/LeaderboardType.java
package de.pixelrpg.rpg.leaderboard;

import de.pixelrpg.rpg.core.StatisticType;

public enum LeaderboardType {
    EXPERIENCE(null),
    MONEY(null),
    MOBS_KILLED(StatisticType.MOBS_KILLED),
    BOSSES_DEFEATED(StatisticType.BOSSES_DEFEATED),
    DUNGEONS_CLEARED(StatisticType.DUNGEONS_CLEARED),
    QUESTS_COMPLETED(StatisticType.QUESTS_COMPLETED);

    private final StatisticType statisticType;

    LeaderboardType(StatisticType statisticType) {
        this.statisticType = statisticType;
    }

    public StatisticType getStatisticType() {
        return statisticType;
    }
}