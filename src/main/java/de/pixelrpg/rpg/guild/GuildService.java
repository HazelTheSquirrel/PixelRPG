package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.api.GuildAPI;
import java.util.UUID;

/** Compatibility facade for the guild API; runtime ownership belongs to GuildManager. */
public final class GuildService implements GuildAPI {
    private final GuildManager manager;

    public GuildService(GuildManager manager) {
        this.manager = manager;
    }

    @Override public boolean isRegistered(UUID uuid) { return manager.isRegistered(uuid); }
    @Override public int getLevel(UUID uuid) { return manager.getLevel(uuid); }
    @Override public long getExperience(UUID uuid) { return manager.getExperience(uuid); }
    @Override public void addExperience(UUID uuid, long amount) { manager.addExperience(uuid, amount); }
}
