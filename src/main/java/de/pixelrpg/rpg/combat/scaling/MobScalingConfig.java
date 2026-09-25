package de.pixelrpg.rpg.combat.scaling;

import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class MobScalingConfig {
    public record LevelBaseStats(double hp, double damage) { }

    private int nameplateDurationTicks = 120;
    private double xpPerMaxHealth = 0.6D;
    private double playerParityMultiplier = 1.0D;
    private double hpPerLevel = 8.0D;
    private double damagePerLevel = 1.1D;
    private double minimumGearMultiplier = 0.75D;
    private double maximumGearMultiplier = 1.25D;

    /** Loads the player-parity mob scaling curve from the user-editable JSON data file. */
    public void load(JsonDataManager dataManager) {
        JsonObject root = dataManager.load("mob-scaling.json");
        nameplateDurationTicks = intValue(root, "nameplateDurationTicks", nameplateDurationTicks);
        xpPerMaxHealth = number(root, "xpPerMaxHealth", xpPerMaxHealth);
        playerParityMultiplier = number(root, "playerParityMultiplier", playerParityMultiplier);
        hpPerLevel = number(root, "hpPerLevel", hpPerLevel);
        damagePerLevel = number(root, "damagePerLevel", damagePerLevel);
        minimumGearMultiplier = number(root, "minimumGearMultiplier", minimumGearMultiplier);
        maximumGearMultiplier = number(root, "maximumGearMultiplier", maximumGearMultiplier);
    }

    /** Loads the JSON curve through the existing plugin startup path, with legacy config as a safe fallback. */
    public void load(FileConfiguration config) {
        Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("PixelRPG");
        if (plugin != null) {
            try {
                load(new JsonDataManager(plugin));
                return;
            } catch (RuntimeException ignored) {
                plugin.getLogger().warning("Falling back to legacy mob scaling config because mob-scaling.json could not be loaded.");
            }
        }
        playerParityMultiplier = config.getDouble("mob-scaling.player-parity-multiplier", playerParityMultiplier);
        hpPerLevel = config.getDouble("mob-scaling.hp-per-level", hpPerLevel);
        damagePerLevel = config.getDouble("mob-scaling.damage-per-level", damagePerLevel);
        nameplateDurationTicks = config.getInt("mob-scaling.nameplate-duration-ticks", nameplateDurationTicks);
        xpPerMaxHealth = config.getDouble("mob-scaling.xp-per-max-health", xpPerMaxHealth);
        minimumGearMultiplier = config.getDouble("mob-scaling.minimum-gear-multiplier", minimumGearMultiplier);
        maximumGearMultiplier = config.getDouble("mob-scaling.maximum-gear-multiplier", maximumGearMultiplier);
    }

    private static double number(JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber()
                ? object.get(key).getAsDouble() : fallback;
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber()
                ? object.get(key).getAsInt() : fallback;
    }

    public LevelBaseStats getBaseStats(int level) {
        int safeLevel = Math.max(1, Math.min(60, level));
        return new LevelBaseStats(20.0D + safeLevel * hpPerLevel, 2.0D + safeLevel * damagePerLevel);
    }

    public int getNameplateDurationTicks() {
        return nameplateDurationTicks;
    }

    public double getXpPerMaxHealth() {
        return xpPerMaxHealth;
    }

    public double getPlayerParityMultiplier() {
        return playerParityMultiplier;
    }

    public double getMinimumGearMultiplier() {
        return minimumGearMultiplier;
    }

    public double getMaximumGearMultiplier() {
        return maximumGearMultiplier;
    }
}
