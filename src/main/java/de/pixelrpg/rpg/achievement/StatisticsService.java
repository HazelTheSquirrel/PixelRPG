// src/main/java/de/pixelrpg/rpg/achievement/StatisticsService.java
package de.pixelrpg.rpg.achievement;

import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.core.StatisticType;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class StatisticsService implements StatisticsAPI {

    private final PlayerProfileManager profileManager;
    private final AchievementManager achievementManager;

    public StatisticsService(PlayerProfileManager profileManager, AchievementManager achievementManager) {
        this.profileManager = profileManager;
        this.achievementManager = achievementManager;
    }

    @Override
    public long getStatistic(UUID uuid, StatisticType type) {
        return getCustomStatistic(uuid, type.name());
    }

    @Override
    public void recordStatistic(UUID uuid, StatisticType type, long amount) {
        recordCustomStatistic(uuid, type.name(), amount);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            achievementManager.checkStatisticAchievements(player, type);
        }
    }

    @Override
    public long getCustomStatistic(UUID uuid, String key) {
        return profileManager.getProfile(uuid).map(p -> p.getStatistic(key)).orElse(0L);
    }

    @Override
    public void recordCustomStatistic(UUID uuid, String key, long amount) {
        profileManager.getProfile(uuid).ifPresent(profile -> {
            profile.incrementStatistic(key, amount);
            profileManager.saveProfileAsync(uuid);
        });
    }
}