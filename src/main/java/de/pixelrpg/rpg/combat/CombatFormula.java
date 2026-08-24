package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.stats.StatEngine;

/** Central MMORPG combat formulas used by player and mob damage. */
public final class CombatFormula {
    private static final double CRIT_DAMAGE_MULTIPLIER = 2.0D;
    private static final double ARMOR_CONSTANT = 100.0D;
    private static final double MIN_DAMAGE = 0.1D;

    private CombatFormula() {
    }

    public static double armorMitigation(double armor) {
        double safeArmor = Math.max(0.0D, armor);
        return ARMOR_CONSTANT / (ARMOR_CONSTANT + safeArmor);
    }

    public static double applyArmor(double damage, double armor) {
        if (damage <= 0.0D) return 0.0D;
        return Math.max(MIN_DAMAGE, damage * armorMitigation(armor));
    }

    public static double offensiveDamage(double vanillaDamage, StatEngine.CachedStats stats) {
        return Math.max(MIN_DAMAGE, vanillaDamage + stats.damage() + stats.attackPower());
    }

    public static double critDamage(double damage, boolean critical) {
        return critical ? damage * CRIT_DAMAGE_MULTIPLIER : damage;
    }

    public static double lifesteal(double dealtDamage, double percent) {
        if (dealtDamage <= 0.0D || percent <= 0.0D) return 0.0D;
        return dealtDamage * (percent / 100.0D);
    }

    public static double levelProgress(int level) {
        int safeLevel = Math.max(1, Math.min(99, level));
        return (safeLevel - 1.0D) / 98.0D;
    }
}
