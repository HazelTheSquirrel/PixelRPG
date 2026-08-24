package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.stats.StatEngine;

/** Deterministic MMORPG damage calculations for the current Paper combat pipeline. */
public final class CombatDamageCalculator {
    private static final double ARMOR_CONSTANT = 100.0D;
    private static final double CRIT_MULTIPLIER = 2.0D;
    private static final double MIN_DAMAGE = 0.1D;

    private CombatDamageCalculator() {
    }

    public static double rawPlayerDamage(double vanillaDamage, StatEngine.CachedStats stats) {
        return Math.max(MIN_DAMAGE, vanillaDamage + stats.attackPower());
    }

    public static double mitigate(double damage, double armor) {
        if (damage <= 0.0D) return 0.0D;
        double safeArmor = Math.max(0.0D, armor);
        return Math.max(MIN_DAMAGE, damage * (ARMOR_CONSTANT / (ARMOR_CONSTANT + safeArmor)));
    }

    public static double crit(double damage, boolean critical) {
        return critical ? damage * CRIT_MULTIPLIER : damage;
    }

    public static double lifesteal(double dealtDamage, double percent) {
        return dealtDamage > 0.0D && percent > 0.0D ? dealtDamage * percent / 100.0D : 0.0D;
    }

    public static double customDamageBeforeVanillaMitigation(double desiredFinalDamage, double rawVanillaDamage,
                                                              double vanillaFinalDamage) {
        if (desiredFinalDamage <= 0.0D) return 0.0D;
        if (rawVanillaDamage <= 0.0D || vanillaFinalDamage <= 0.0D) return desiredFinalDamage;
        return desiredFinalDamage * (rawVanillaDamage / vanillaFinalDamage);
    }

    public static double mobGearMultiplier(double playerPower, double baselinePower, double minimum, double maximum) {
        if (baselinePower <= 0.0D) return 1.0D;
        double ratio = playerPower / baselinePower;
        return Math.max(minimum, Math.min(maximum, ratio));
    }
}
