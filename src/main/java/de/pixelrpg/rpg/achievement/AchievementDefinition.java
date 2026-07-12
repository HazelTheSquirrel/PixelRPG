// src/main/java/de/pixelrpg/rpg/achievement/AchievementDefinition.java
package de.pixelrpg.rpg.achievement;

import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.core.StatisticType;

public record AchievementDefinition(
        String id,
        String displayName,
        String description,
        AchievementTriggerType triggerType,
        StatisticType statisticType,
        long thresholdValue,
        Rank requiredRank,
        double rewardMoney,
        long rewardExp,
        String rewardTitle
) {
}