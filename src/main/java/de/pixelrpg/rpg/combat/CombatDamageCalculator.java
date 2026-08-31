package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.balance.BalanceModel;
import de.pixelrpg.rpg.stats.StatEngine;

/** Central deterministic PixelRPG combat formulas. */
public final class CombatDamageCalculator {
    private static final double ARMOR_CONSTANT = 100.0D;
    private static final double MIN_DAMAGE = 0.1D;
    private CombatDamageCalculator() {}

    public static double rawPlayerDamage(double weaponDamage, StatEngine.CachedStats stats) {
        return Math.max(MIN_DAMAGE, weaponDamage + stats.attackPower());
    }

    public static double mitigate(double damage, double armor) {
        if (damage <= 0.0D) return 0.0D;
        double safeArmor = Math.clamp(armor, 0.0D, BalanceModel.MAX_ARMOR);
        return Math.max(MIN_DAMAGE, damage * (ARMOR_CONSTANT / (ARMOR_CONSTANT + safeArmor)));
    }

    public static double crit(double damage, boolean critical, StatEngine.CachedStats stats) {
        if (!critical || stats == null) return damage;
        return damage * Math.clamp(stats.critDamageMultiplier(), 2.0D, 3.0D);
    }

    public static double crit(double damage, boolean critical) { return critical ? damage * 2.0D : damage; }

    public static double lifesteal(double dealtDamage, double percent) {
        if (dealtDamage <= 0.0D || percent <= 0.0D) return 0.0D;
        return dealtDamage * Math.clamp(percent, 0.0D, BalanceModel.MAX_LIFESTEAL_PERCENT) / 100.0D;
    }

    public static double expectedDamage(double baseDamage, double critChance, double critMultiplier) {
        double c = Math.clamp(critChance / 100.0D, 0.0D, 1.0D);
        double m = Math.clamp(critMultiplier, 2.0D, 3.0D);
        return baseDamage * ((1.0D - c) + c * m);
    }

    public static double customDamageBeforeVanillaMitigation(double desiredFinalDamage, double rawVanillaDamage, double vanillaFinalDamage) {
        if (desiredFinalDamage <= 0.0D) return 0.0D;
        if (rawVanillaDamage <= 0.0D || vanillaFinalDamage <= 0.0D) return desiredFinalDamage;
        return desiredFinalDamage * (rawVanillaDamage / vanillaFinalDamage);
    }

    public static double mobGearMultiplier(double playerPower, double baselinePower, double minimum, double maximum) {
        if (baselinePower <= 0.0D) return 1.0D;
        return Math.clamp(playerPower / baselinePower, minimum, maximum);
    }

    public static double mobHealth(double baseHealth, int playerLevel, double ppi) {
        double level = BalanceModel.levelPower(playerLevel);
        double gear = 0.85D + 0.30D * Math.clamp(ppi, 0.0D, 1.0D);
        return baseHealth * (1.0D + 1.80D * level) * gear;
    }

    public static double mobDamage(double baseDamage, int playerLevel, double ppi) {
        double level = BalanceModel.levelPower(playerLevel);
        double gear = 0.90D + 0.20D * Math.clamp(ppi, 0.0D, 1.0D);
        return baseDamage * (1.0D + 1.20D * level) * gear;
    }
}
