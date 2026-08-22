package de.pixelrpg.rpg.combat.scaling;

import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class MobScalingConfig {
    public record LevelBaseStats(double hp, double damage) { }
    public record DimensionModifier(double hpMultiplier, double damageMultiplier, int levelOffset, int baseLevel) { }
    private DimensionModifier overworld;
    private DimensionModifier nether;
    private DimensionModifier theEnd;
    private int nameplateDurationTicks = 120;
    private double xpPerMaxHealth = 0.6;
    private double playerParityMultiplier = 1.15;
    private double hpPerLevel = 8.0;
    private double damagePerLevel = 1.1;

    /** Loads mob scaling formulas from the user-editable JSON data file. */
    public void load(JsonDataManager dataManager) {
        JsonObject root = dataManager.load("mob-scaling.json");
        nameplateDurationTicks = intValue(root, "nameplateDurationTicks", nameplateDurationTicks);
        xpPerMaxHealth = number(root, "xpPerMaxHealth", xpPerMaxHealth);
        playerParityMultiplier = number(root, "playerParityMultiplier", playerParityMultiplier);
        hpPerLevel = number(root, "hpPerLevel", hpPerLevel);
        damagePerLevel = number(root, "damagePerLevel", damagePerLevel);
        JsonObject dimensions = root.getAsJsonObject("dimensions");
        overworld = loadDimension(dimensions, "overworld", 1.0, 1.0, 0, 1);
        nether = loadDimension(dimensions, "nether", 1.30, 1.20, 0, 50);
        theEnd = loadDimension(dimensions, "the_end", 1.60, 1.40, 0, 75);
    }

    /** Loads JSON data during the existing plugin startup path, with the legacy config as fallback. */
    public void load(FileConfiguration config) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PixelRPG");
        if (plugin != null) {
            try {
                load(new JsonDataManager(plugin));
                return;
            } catch (RuntimeException ignored) {
                plugin.getLogger().warning("Falling back to legacy mob scaling config because mob-scaling.json could not be loaded.");
            }
        }
        playerParityMultiplier = config.getDouble("mob-scaling.player-parity-multiplier", 1.15);
        hpPerLevel = config.getDouble("mob-scaling.hp-per-level", 8.0);
        damagePerLevel = config.getDouble("mob-scaling.damage-per-level", 1.1);
        overworld = loadDimension(config, "mob-scaling.dimension.overworld", 1.0, 1.0, 0, 1);
        nether = loadDimension(config, "mob-scaling.dimension.nether", 1.30, 1.20, 0, 50);
        theEnd = loadDimension(config, "mob-scaling.dimension.the-end", 1.60, 1.40, 0, 75);
        nameplateDurationTicks = config.getInt("mob-scaling.nameplate-duration-ticks", 120);
        xpPerMaxHealth = config.getDouble("mob-scaling.xp-per-max-health", 0.6);
    }

    private DimensionModifier loadDimension(JsonObject parent, String key, double defaultHp, double defaultDmg, int defaultOffset, int defaultBaseLevel) {
        JsonObject object = parent == null ? null : parent.getAsJsonObject(key);
        if (object == null) return new DimensionModifier(defaultHp, defaultDmg, defaultOffset, defaultBaseLevel);
        return new DimensionModifier(number(object, "hpMultiplier", defaultHp), number(object, "damageMultiplier", defaultDmg), intValue(object, "levelOffset", defaultOffset), intValue(object, "baseLevel", defaultBaseLevel));
    }

    private DimensionModifier loadDimension(FileConfiguration config, String path, double defaultHp, double defaultDmg, int defaultOffset, int defaultBaseLevel) {
        return new DimensionModifier(config.getDouble(path + ".hp-multiplier", defaultHp), config.getDouble(path + ".damage-multiplier", defaultDmg), config.getInt(path + ".level-offset", defaultOffset), config.getInt(path + ".base-level", defaultBaseLevel));
    }

    private static double number(JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isNumber() ? object.get(key).getAsDouble() : fallback;
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) && object.get(key).isNumber() ? object.get(key).getAsInt() : fallback;
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
    public double getXpPerMaxHealth() { return xpPerMaxHealth; }
    public double getPlayerParityMultiplier() { return playerParityMultiplier; }
}
