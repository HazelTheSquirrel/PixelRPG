package de.pixelrpg.rpg.balance;

import de.pixelrpg.rpg.stats.StatEngine;

/** Converts the currently active RPG equipment stats into one bounded scaling index. */
public final class PlayerPowerIndex {
    private PlayerPowerIndex() {
    }

    /** Returns 0.0 for an unequipped RPG player and 1.0 for a cap-level normal build. */
    public static double calculate(StatEngine.CachedStats stats) {
        if (stats == null) return 0.0D;

        double hp = Math.clamp((stats.maxHealth() - 20.0D) / BalanceModel.MAX_HP_BONUS, 0.0D, 1.0D);
        double armor = Math.clamp(stats.armor() / BalanceModel.MAX_ARMOR, 0.0D, 1.0D);
        double movement = Math.clamp(stats.movementSpeedBonus() / BalanceModel.MAX_MOVEMENT_SPEED_PERCENT, 0.0D, 1.0D);
        double reach = Math.clamp(stats.reach() / BalanceModel.MAX_REACH_BONUS, 0.0D, 1.0D);
        double crit = Math.clamp(stats.critChance() / BalanceModel.MAX_CRIT_CHANCE, 0.0D, 1.0D);
        double critDamage = Math.clamp(stats.critDamageBonusPercent() / BalanceModel.MAX_CRIT_DAMAGE_BONUS_PERCENT, 0.0D, 1.0D);
        double lifesteal = Math.clamp(stats.lifestealBonus() / BalanceModel.MAX_LIFESTEAL_PERCENT, 0.0D, 1.0D);
        double attack = Math.clamp(stats.attackPower() / BalanceModel.MAX_ATTACK_POWER, 0.0D, 1.0D);

        return (hp + armor + movement + reach + crit + critDamage + lifesteal + attack) / 8.0D;
    }

    /** Maps the bounded power index into the documented 0.75–1.25 monster gear multiplier. */
    public static double gearMultiplier(StatEngine.CachedStats stats) {
        return 0.75D + calculate(stats) * 0.50D;
    }
}
