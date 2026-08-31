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
    private double baseHp = 20.0D;
    private double baseDamage = 2.0D;
    private double minimumGearMultiplier = 0.85D;
    private double maximumGearMultiplier = 1.15D;

    public void load(JsonDataManager dataManager) {
        JsonObject root = dataManager.load("mob-scaling.json");
        nameplateDurationTicks = intValue(root, "nameplateDurationTicks", nameplateDurationTicks);
        xpPerMaxHealth = number(root, "xpPerMaxHealth", xpPerMaxHealth);
        playerParityMultiplier = number(root, "playerParityMultiplier", playerParityMultiplier);
        baseHp = number(root, "baseHp", baseHp);
        baseDamage = number(root, "baseDamage", baseDamage);
        minimumGearMultiplier = number(root, "minimumGearMultiplier", minimumGearMultiplier);
        maximumGearMultiplier = number(root, "maximumGearMultiplier", maximumGearMultiplier);
    }

    public void load(FileConfiguration config) {
        Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("PixelRPG");
        if (plugin != null) {
            try { load(new JsonDataManager(plugin)); return; }
            catch (RuntimeException ignored) { plugin.getLogger().warning("Falling back to legacy mob scaling config."); }
        }
        playerParityMultiplier = config.getDouble("mob-scaling.player-parity-multiplier", playerParityMultiplier);
        nameplateDurationTicks = config.getInt("mob-scaling.nameplate-duration-ticks", nameplateDurationTicks);
        xpPerMaxHealth = config.getDouble("mob-scaling.xp-per-max-health", xpPerMaxHealth);
        minimumGearMultiplier = config.getDouble("mob-scaling.minimum-gear-multiplier", minimumGearMultiplier);
        maximumGearMultiplier = config.getDouble("mob-scaling.maximum-gear-multiplier", maximumGearMultiplier);
    }

    private static double number(JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.get(key).getAsDouble() : fallback;
    }
    private static int intValue(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.get(key).getAsInt() : fallback;
    }
    public LevelBaseStats getBaseStats(int ignoredLevel) { return new LevelBaseStats(baseHp, baseDamage); }
    public int getNameplateDurationTicks() { return nameplateDurationTicks; }
    public double getXpPerMaxHealth() { return xpPerMaxHealth; }
    public double getPlayerParityMultiplier() { return playerParityMultiplier; }
    public double getMinimumGearMultiplier() { return minimumGearMultiplier; }
    public double getMaximumGearMultiplier() { return maximumGearMultiplier; }
}
