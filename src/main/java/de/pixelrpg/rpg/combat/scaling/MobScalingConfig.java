package de.pixelrpg.rpg.combat.scaling;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public final class MobScalingConfig {
    public record LevelBaseStats(double hp, double damage) { }
    public record DimensionModifier(double hpMultiplier, double damageMultiplier, int levelOffset, int baseLevel) { }

    private DimensionModifier overworld;
    private DimensionModifier nether;
    private DimensionModifier theEnd;
    private int nameplateDurationTicks = 120;
    private double vanillaPlayerDamageCap = 4.0;
    private double xpPerMaxHealth = 0.9;
    private double playerParityMultiplier = 1.15;
    private double hpPerLevel = 8.0;
    private double damagePerLevel = 1.1;

    public void load(FileConfiguration config) {
        playerParityMultiplier = config.getDouble("mob-scaling.player-parity-multiplier", 1.15);
        hpPerLevel = config.getDouble("mob-scaling.hp-per-level", 8.0);
        damagePerLevel = config.getDouble("mob-scaling.damage-per-level", 1.1);
        overworld = loadDimension(config, "mob-scaling.dimension.overworld", 1.0, 1.0, 0, 1);
        nether = loadDimension(config, "mob-scaling.dimension.nether", 1.30, 1.20, 0, 50);
        theEnd = loadDimension(config, "mob-scaling.dimension.the-end", 1.60, 1.40, 0, 75);
        nameplateDurationTicks = config.getInt("mob-scaling.nameplate-duration-ticks", 120);
        vanillaPlayerDamageCap = config.getDouble("mob-scaling.vanilla-player-damage-cap", 4.0);
        xpPerMaxHealth = config.getDouble("mob-scaling.xp-per-max-health", 0.9);
    }

    private DimensionModifier loadDimension(FileConfiguration config, String path, double defaultHp, double defaultDmg,
                                            int defaultOffset, int defaultBaseLevel) {
        return new DimensionModifier(
                config.getDouble(path + ".hp-multiplier", defaultHp),
                config.getDouble(path + ".damage-multiplier", defaultDmg),
                config.getInt(path + ".level-offset", defaultOffset),
                config.getInt(path + ".base-level", defaultBaseLevel)
        );
    }

    public LevelBaseStats getBaseStats(int level) {
        int safeLevel = Math.max(1, Math.min(99, level));
        return new LevelBaseStats(20.0 + safeLevel * hpPerLevel, 2.0 + safeLevel * damagePerLevel);
    }

    public DimensionModifier getDimensionModifier(World.Environment environment) {
        return switch (environment) {
            case NETHER -> nether;
            case THE_END -> theEnd;
            default -> overworld;
        };
    }

    public int getNameplateDurationTicks() { return nameplateDurationTicks; }
    public double getVanillaPlayerDamageCap() { return vanillaPlayerDamageCap; }
    public double getXpPerMaxHealth() { return xpPerMaxHealth; }
    public double getPlayerParityMultiplier() { return playerParityMultiplier; }
}
