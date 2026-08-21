package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.api.GuildAPI;

/** Temporary compatibility name during the rank-to-level migration. */
@Deprecated(forRemoval = true)
public final class MobRankScalingListener extends MobLevelScalingListener {
    public MobRankScalingListener(GuildAPI guildAPI, MobScalingConfig scalingConfig) {
        super(guildAPI, scalingConfig);
    }
}
