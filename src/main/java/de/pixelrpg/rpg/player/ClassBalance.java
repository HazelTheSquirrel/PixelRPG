package de.pixelrpg.rpg.player;

import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;

public record ClassBalance(double armorBonus, double healthBonus, double speedBonus, double meleeDamageMultiplier, double rangedDamageMultiplier, double spellDamageMultiplier, double healMultiplier, double critChanceBonus, double critDamageMultiplier) {
    private static final ClassBalance NEUTRAL = new ClassBalance(0, 0, 0, 1.0, 1.0, 1.0, 1.0, 0, 1.0);
    private record ClassFormula(double armorBase, double armorPerToughness, double healthBase, double healthPerVitality, double speedBase, double speedPerAgility, double meleeMultBase, double meleeMultPerToughness, double rangedMultBase, double rangedMultPerPoint, double spellMultBase, double spellMultPerPrecision, double healMultBase, double healMultPerVitality, double critChanceBase, double critChancePerPoint, double critDamageMultBase, double critDamageMultPerPrecision) { }
    private static final Map<PlayerClass, ClassFormula> FORMULAS = new EnumMap<>(PlayerClass.class);
    static {
        FORMULAS.put(PlayerClass.WARRIOR, new ClassFormula(5.0, 0.0, 10.0, 0.0, 0.0, 0.0, 1.05, 0.0, 0.95, 0.0, 0.90, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0));
        FORMULAS.put(PlayerClass.RANGER, new ClassFormula(0.0, 0.0, 3.0, 0.0, 0.0025, 0.0, 0.95, 0.0, 1.05, 0.0, 1.0, 0.0, 1.0, 0.0, 3.0, 0.0, 1.0, 0.0));
        FORMULAS.put(PlayerClass.ROGUE, new ClassFormula(0.0, 0.0, 0.0, 0.0, 0.002, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 5.0, 0.0, 1.15, 0.0));
        FORMULAS.put(PlayerClass.HEALER, new ClassFormula(3.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.95, 0.0, 0.95, 0.0, 1.0, 0.0, 1.15, 0.0, 0.0, 0.0, 1.0, 0.0));
        FORMULAS.put(PlayerClass.MAGE, new ClassFormula(-2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.85, 0.0, 1.0, 0.0, 1.15, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0));
    }

    /** Loads class calculation formulas from the user-editable JSON data file. */
    public static void load(JsonDataManager dataManager) {
        JsonObject root = dataManager.load("class-balance.json");
        for (PlayerClass playerClass : PlayerClass.values()) {
            if (playerClass == PlayerClass.NONE) continue;
            JsonObject object = root.getAsJsonObject(playerClass.name().toLowerCase());
            if (object != null) loadClass(object, playerClass);
        }
    }

    private static void loadClass(JsonObject json, PlayerClass playerClass) {
        ClassFormula existing = FORMULAS.get(playerClass);
        FORMULAS.put(playerClass, new ClassFormula(
                number(json, "armorBase", existing.armorBase()),
                number(json, "armorPerToughness", existing.armorPerToughness()),
                number(json, "healthBase", existing.healthBase()),
                number(json, "healthPerVitality", existing.healthPerVitality()),
                number(json, "speedBase", existing.speedBase()),
                number(json, "speedPerAgility", existing.speedPerAgility()),
                number(json, "meleeMultBase", existing.meleeMultBase()),
                number(json, "meleeMultPerToughness", existing.meleeMultPerToughness()),
                number(json, "rangedMultBase", existing.rangedMultBase()),
                number(json, "rangedMultPerPoint", existing.rangedMultPerPoint()),
                number(json, "spellMultBase", existing.spellMultBase()),
                number(json, "spellMultPerPrecision", existing.spellMultPerPrecision()),
                number(json, "healMultBase", existing.healMultBase()),
                number(json, "healMultPerVitality", existing.healMultPerVitality()),
                number(json, "critChanceBase", existing.critChanceBase()),
                number(json, "critChancePerPoint", existing.critChancePerPoint()),
                number(json, "critDamageMultBase", existing.critDamageMultBase()),
                number(json, "critDamageMultPerPrecision", existing.critDamageMultPerPrecision())
        ));
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() && json.getAsJsonPrimitive(key).isNumber()
                ? json.getAsJsonPrimitive(key).getAsDouble() : fallback;
    }

    /** Loads JSON data during the existing plugin startup path, with the legacy config as fallback. */
    public static void load(FileConfiguration config) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PixelRPG");
        if (plugin != null) {
            try {
                load(new JsonDataManager(plugin));
                return;
            } catch (RuntimeException ignored) {
                plugin.getLogger().warning("Falling back to legacy class balance config because class-balance.json could not be loaded.");
            }
        }
        loadClassConfig(config, "warrior", PlayerClass.WARRIOR);
        loadClassConfig(config, "ranger", PlayerClass.RANGER);
        loadClassConfig(config, "rogue", PlayerClass.ROGUE);
        loadClassConfig(config, "healer", PlayerClass.HEALER);
        loadClassConfig(config, "mage", PlayerClass.MAGE);
    }

    private static void loadClassConfig(FileConfiguration config, String path, PlayerClass playerClass) {
        String base = "class-balance." + path + ".";
        if (!config.contains("class-balance." + path)) return;
        ClassFormula existing = FORMULAS.get(playerClass);
        FORMULAS.put(playerClass, new ClassFormula(
                config.getDouble(base + "armor-base", existing.armorBase()),
                config.getDouble(base + "armor-per-toughness", existing.armorPerToughness()),
                config.getDouble(base + "health-base", existing.healthBase()),
                config.getDouble(base + "health-per-vitality", existing.healthPerVitality()),
                config.getDouble(base + "speed-base", existing.speedBase()),
                config.getDouble(base + "speed-per-agility", existing.speedPerAgility()),
                config.getDouble(base + "melee-mult-base", existing.meleeMultBase()),
                config.getDouble(base + "melee-mult-per-toughness", existing.meleeMultPerToughness()),
                config.getDouble(base + "ranged-mult-base", existing.rangedMultBase()),
                config.getDouble(base + "ranged-mult-per-point", existing.rangedMultPerPoint()),
                config.getDouble(base + "spell-mult-base", existing.spellMultBase()),
                config.getDouble(base + "spell-mult-per-precision", existing.spellMultPerPrecision()),
                config.getDouble(base + "heal-mult-base", existing.healMultBase()),
                config.getDouble(base + "heal-mult-per-vitality", existing.healMultPerVitality()),
                config.getDouble(base + "crit-chance-base", existing.critChanceBase()),
                config.getDouble(base + "crit-chance-per-point", existing.critChancePerPoint()),
                config.getDouble(base + "crit-damage-mult-base", existing.critDamageMultBase()),
                config.getDouble(base + "crit-damage-mult-per-precision", existing.critDamageMultPerPrecision())
        ));
    }

    public static ClassBalance of(PlayerProfile profile) {
        if (profile == null) return NEUTRAL;
        PlayerClass playerClass = profile.getPlayerClass();
        ClassFormula formula = FORMULAS.get(playerClass);
        if (formula == null) return NEUTRAL;
        int vitality = profile.getAttributePoints(PlayerAttribute.VITALITY);
        int agility = profile.getAttributePoints(PlayerAttribute.AGILITY);
        int precision = profile.getAttributePoints(PlayerAttribute.PRECISION);
        int range = profile.getAttributePoints(PlayerAttribute.RANGE);
        int toughness = profile.getAttributePoints(PlayerAttribute.TOUGHNESS);
        double armor = formula.armorBase() + toughness * formula.armorPerToughness();
        double health = formula.healthBase() + vitality * formula.healthPerVitality();
        double speed = formula.speedBase() + agility * formula.speedPerAgility();
        double meleeMult = formula.meleeMultBase() + toughness * formula.meleeMultPerToughness();
        double rangedMult = formula.rangedMultBase() + (agility + range) * formula.rangedMultPerPoint();
        double spellMult = formula.spellMultBase() + precision * formula.spellMultPerPrecision();
        double healMult = formula.healMultBase() + vitality * formula.healMultPerVitality();
        double critChance = formula.critChanceBase() + (playerClass == PlayerClass.RANGER ? range * formula.critChancePerPoint() : (agility + precision) * formula.critChancePerPoint());
        double critDamageMult = formula.critDamageMultBase() + precision * formula.critDamageMultPerPrecision();
        return new ClassBalance(armor, health, speed, meleeMult, rangedMult, spellMult, healMult, critChance, critDamageMult);
    }
}
