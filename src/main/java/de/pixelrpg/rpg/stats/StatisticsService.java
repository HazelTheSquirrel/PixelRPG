// src/main/java/de/pixelrpg/rpg/stats/StatisticsService.java
package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.core.StatisticType;
import de.pixelrpg.rpg.player.PlayerProfileManager;

import java.util.UUID;

public final class StatisticsService implements StatisticsAPI {

    private final PlayerProfileManager profileManager;

    public StatisticsService(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public long getStatistic(UUID uuid, StatisticType type) {
        return getCustomStatistic(uuid, type.name());
    }

    @Override
    public void recordStatistic(UUID uuid, StatisticType type, long amount) {
        recordCustomStatistic(uuid, type.name(), amount);
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