// src/main/java/de/pixelrpg/rpg/player/PlayerProfileRepository.java (VOLLSTÄNDIG, ersetzt alte Datei — Leaderboard-Methoden entfernt)
package de.pixelrpg.rpg.player;

import java.util.Optional;
import java.util.UUID;

public interface PlayerProfileRepository {

    void init() throws Exception;

    Optional<PlayerProfile> load(UUID uuid) throws Exception;

    void save(PlayerProfile profile) throws Exception;

    void shutdown();
}