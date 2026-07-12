// src/main/java/de/pixelrpg/rpg/player/PlayerProfileRepository.java
package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.leaderboard.LeaderboardEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerProfileRepository {

    void init() throws Exception;

    Optional<PlayerProfile> load(UUID uuid) throws Exception;

    void save(PlayerProfile profile) throws Exception;

    void shutdown();

    List<LeaderboardEntry> getTopByExperience(int limit) throws Exception;

    List<LeaderboardEntry> getTopByMoney(int limit) throws Exception;

    List<LeaderboardEntry> getTopByStatistic(String statisticKey, int limit) throws Exception;
}