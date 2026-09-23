package de.pixelrpg.rpg.api;

import java.util.UUID;

public interface GuildAPI {
    boolean isRegistered(UUID uuid);
    int getLevel(UUID uuid);
    long getExperience(UUID uuid);
    void addExperience(UUID uuid,long amount);
}
