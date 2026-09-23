package de.pixelrpg.rpg.player;

import java.util.Optional;
import java.util.UUID;

public interface PlayerProfileRepository {
    void init() throws Exception;
    Optional<PlayerProfile> load(UUID uuid) throws Exception;
    long save(PlayerProfile profile) throws Exception;
    void shutdown();
}