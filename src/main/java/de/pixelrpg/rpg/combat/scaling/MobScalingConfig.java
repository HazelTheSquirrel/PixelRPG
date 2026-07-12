// src/main/java/de/pixelrpg/rpg/combat/scaling/MobScalingConfig.java
package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.core.Rank;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

public final class MobScalingConfig {

    public record RankBaseStats(double hp, double damage) {
    }

    public record DimensionModifier(double hpMultiplier, double damageMultiplier, int rankOffset) {
    }

    private final Map<Rank, RankBaseStats> baseStatsByRank = new EnumMap<>(Rank.class);
    private DimensionModifier overworld;
    private DimensionModifier nether;
    private DimensionModifier theEnd;

    private int nameplateDurationTicks = 120;
    private double vanillaPlayerDamageCap = 4.0;
    private double xpPerMaxHealth = 0.9;
    private double playerParityMultiplier = 1.15;

    public void load(FileConfiguration config) {
        for (Rank rank : Rank.values()) {
            String path = "mob-scaling.base-stats." + rank.name();
            double hp = config.getDouble(path + ".hp", 20.0 + rank.ordinal() * 20.0);
            double damage = config.getDouble(path + ".damage", 3.0 + rank.ordinal() * 3.0);
            baseStatsByRank.put(rank, new RankBaseStats(hp, damage));
            playerParityMultiplier = config.getDouble("mob-scaling.player-parity-multiplier", 1.15);
        }

        overworld = loadDimension(config, "mob-scaling.dimension.overworld", 1.0, 1.0, 0);
        nether = loadDimension(config, "mob-scaling.dimension.nether", 1.30, 1.20, 1);
        theEnd = loadDimension(config, "mob-scaling.dimension.the-end", 1.60, 1.40, 2);

        nameplateDurationTicks = config.getInt("mob-scaling.nameplate-duration-ticks", 120);
        vanillaPlayerDamageCap = config.getDouble("mob-scaling.vanilla-player-damage-cap", 4.0);
        xpPerMaxHealth = config.getDouble("mob-scaling.xp-per-max-health", 0.9);
    }

    private DimensionModifier loadDimension(FileConfiguration config, String path,
                                             double defaultHp, double defaultDmg, int defaultOffset) {
        return new DimensionModifier(
                config.getDouble(path + ".hp-multiplier", defaultHp),
                config.getDouble(path + ".damage-multiplier", defaultDmg),
                config.getInt(path + ".rank-offset", defaultOffset)
        );
    }

    public RankBaseStats getBaseStats(Rank rank) {
        return baseStatsByRank.getOrDefault(rank, new RankBaseStats(20.0, 3.0));
    }

    public DimensionModifier getDimensionModifier(World.Environment environment) {
        return switch (environment) {
            case NETHER -> nether;
            case THE_END -> theEnd;
            default -> overworld;
        };
    }

    public int getNameplateDurationTicks() {
        return nameplateDurationTicks;
    }

    public double getVanillaPlayerDamageCap() {
        return vanillaPlayerDamageCap;
    }

    public double getXpPerMaxHealth() {
        return xpPerMaxHealth;
    }
    public double getPlayerParityMultiplier() {
        return playerParityMultiplier;
    }
}