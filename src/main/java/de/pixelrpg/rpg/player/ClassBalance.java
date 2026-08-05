package de.pixelrpg.rpg.player;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

public record ClassBalance(
        double armorBonus,
        double healthBonus,
        double speedBonus,
        double meleeDamageMultiplier,
        double rangedDamageMultiplier,
        double spellDamageMultiplier,
        double healMultiplier,
        double critChanceBonus,
        double critDamageMultiplier
) {

    private static final ClassBalance NEUTRAL = new ClassBalance(0, 0, 0, 1.0, 1.0, 1.0, 1.0, 0, 1.0);

    private record ClassFormula(
            double armorBase, double armorPerToughness,
            double healthBase, double healthPerVitality,
            double speedBase, double speedPerAgility,
            double meleeMultBase, double meleeMultPerToughness,
            double rangedMultBase, double rangedMultPerPoint,
            double spellMultBase, double spellMultPerPrecision,
            double healMultBase, double healMultPerVitality,
            double critChanceBase, double critChancePerPoint,
            double critDamageMultBase, double critDamageMultPerPrecision
    ) {
    }

    private static final Map<PlayerClass, ClassFormula> FORMULAS = new EnumMap<>(PlayerClass.class);

    static {
        // Sinnvolle Defaults, identisch zu den vorherigen hartcodierten Werten,
        // werden beim Fehlen der Config-Sektion verwendet.
        FORMULAS.put(PlayerClass.WARRIOR, new ClassFormula(
                10.0, 1.8, 8.0, 1.2, 0.0, 0.0,
                1.15, 0.015, 1.0, 0.0, 1.0, 0.0,
                1.0, 0.0, 0.0, 0.0, 1.0, 0.0));
        FORMULAS.put(PlayerClass.RANGER, new ClassFormula(
                0.0, 0.0, 2.0, 0.5, 0.03, 0.002,
                1.0, 0.0, 1.20, 0.015, 1.0, 0.0,
                1.0, 0.0, 5.0, 0.8, 1.0, 0.0));
        FORMULAS.put(PlayerClass.ROGUE, new ClassFormula(
                0.0, 0.0, 0.0, 0.0, 0.02, 0.002,
                1.0, 0.0, 1.0, 0.0, 1.0, 0.0,
                1.0, 0.0, 15.0, 1.0, 1.6, 0.03));
        FORMULAS.put(PlayerClass.HEALER, new ClassFormula(
                3.0, 0.5, 10.0, 1.5, 0.0, 0.0,
                1.0, 0.0, 1.0, 0.0, 1.0, 0.0,
                1.35, 0.025, 0.0, 0.0, 1.0, 0.0));
        FORMULAS.put(PlayerClass.MAGE, new ClassFormula(
                -2.0, 0.0, -2.0, 0.5, 0.0, 0.0,
                1.0, 0.0, 1.0, 0.0, 1.25, 0.025,
                1.0, 0.0, 0.0, 0.0, 1.0, 0.0));
    }

    /**
     * Lädt Klassen-Balancing-Formeln aus der Config-Sektion "class-balance".
     * Fehlt ein Wert, bleibt der bereits gesetzte Default (siehe static-Block)
     * bestehen, sodass ein unvollständiger Config-Abschnitt nicht zu 0-Werten führt.
     */
    public static void load(FileConfiguration config) {
        loadClass(config, "warrior", PlayerClass.WARRIOR);
        loadClass(config, "ranger", PlayerClass.RANGER);
        loadClass(config, "rogue", PlayerClass.ROGUE);
        loadClass(config, "healer", PlayerClass.HEALER);
        loadClass(config, "mage", PlayerClass.MAGE);
    }

    private static void loadClass(FileConfiguration config, String path, PlayerClass playerClass) {
        String base = "class-balance." + path + ".";
        if (!config.contains(path.equals("warrior") ? "class-balance.warrior" : base.substring(0, base.length() - 1))) {
            return;
        }
        ClassFormula existing = FORMULAS.get(playerClass);

        ClassFormula updated = new ClassFormula(
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
        );
        FORMULAS.put(playerClass, updated);
    }

    public static ClassBalance of(PlayerProfile profile) {
        if (profile == null) {
            return NEUTRAL;
        }

        PlayerClass playerClass = profile.getPlayerClass();
        ClassFormula formula = FORMULAS.get(playerClass);
        if (formula == null) {
            return NEUTRAL;
        }

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
        double critChance = formula.critChanceBase() + (
                playerClass == PlayerClass.RANGER ? range * formula.critChancePerPoint()
                        : (agility + precision) * formula.critChancePerPoint()
        );
        double critDamageMult = formula.critDamageMultBase() + precision * formula.critDamageMultPerPrecision();

        return new ClassBalance(armor, health, speed, meleeMult, rangedMult, spellMult, healMult, critChance, critDamageMult);
    }
}