package de.pixelrpg.rpg.api;

import de.pixelrpg.rpg.player.PlayerAttribute;
import de.pixelrpg.rpg.player.PlayerClass;

import java.util.UUID;

public interface GuildAPI {

    boolean isRegistered(UUID uuid);

    int getLevel(UUID uuid);

    PlayerClass getPlayerClass(UUID uuid);

    boolean hasSelectedClass(UUID uuid);

    long getExperience(UUID uuid);

    void addExperience(UUID uuid, long amount);

    int getAttributePoints(UUID uuid, PlayerAttribute attribute);
}
