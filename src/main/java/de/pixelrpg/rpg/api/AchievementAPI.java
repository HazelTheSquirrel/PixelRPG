// src/main/java/de/pixelrpg/rpg/api/AchievementAPI.java
package de.pixelrpg.rpg.api;

import de.pixelrpg.rpg.achievement.AchievementDefinition;

import java.util.Set;
import java.util.UUID;

public interface AchievementAPI {

    boolean hasAchievement(UUID uuid, String achievementId);

    Set<String> getUnlockedAchievements(UUID uuid);

    void registerAchievement(AchievementDefinition definition);
}