package de.pixelrpg.rpg.balance;

import de.pixelrpg.rpg.stats.StatEngine;

/** Calculates weighted combat power from active, capped RPG stats. */
public final class PlayerPowerIndex {
    private PlayerPowerIndex() {}

    public static double calculate(StatEngine.CachedStats stats) {
        if (stats == null) return 0.0D;
        double hp = Math.clamp(stats.rpgHealthBonus() / BalanceModel.MAX_HP_BONUS, 0.0D, 1.0D);
        double armor = Math.clamp(stats.armor() / BalanceModel.MAX_ARMOR, 0.0D, 1.0D);
        double movement = Math.clamp(stats.movementSpeedBonus() / BalanceModel.MAX_MOVEMENT_SPEED_PERCENT, 0.0D, 1.0D);
        double reach = Math.clamp((stats.entityReach() - BalanceModel.BASE_ENTITY_REACH) / 2.0D, 0.0D, 1.0D);
        double attack = Math.clamp(stats.attackPower() / BalanceModel.MAX_ATTACK_POWER, 0.0D, 1.0D);
        double crit = Math.clamp(stats.critChance() / BalanceModel.MAX_CRIT_CHANCE, 0.0D, 1.0D);
        double critDamage = Math.clamp(stats.critDamageBonusPercent() / BalanceModel.MAX_CRIT_DAMAGE_BONUS_PERCENT, 0.0D, 1.0D);
        double lifesteal = Math.clamp(stats.lifestealBonus() / BalanceModel.MAX_LIFESTEAL_PERCENT, 0.0D, 1.0D);
        return 0.16D * hp + 0.16D * armor + 0.06D * movement + 0.05D * reach
                + 0.22D * attack + 0.14D * crit + 0.14D * critDamage + 0.07D * lifesteal;
    }

    public static double offensivePower(StatEngine.CachedStats stats) {
        if (stats == null) return 0.0D;
        double attack = Math.clamp(stats.attackPower() / BalanceModel.MAX_ATTACK_POWER, 0.0D, 1.0D);
        double crit = Math.clamp(stats.critChance() / 100.0D, 0.0D, 1.0D);
        double critDamage = Math.clamp(stats.critDamageBonusPercent() / 100.0D, 0.0D, 1.0D);
        double lifesteal = Math.clamp(stats.lifestealBonus() / 8.0D, 0.0D, 1.0D);
        return 0.45D * attack + 0.25D * crit + 0.20D * critDamage + 0.10D * lifesteal;
    }

    public static double defensivePower(StatEngine.CachedStats stats) {
        if (stats == null) return 0.0D;
        double hp = Math.clamp(stats.rpgHealthBonus() / 100.0D, 0.0D, 1.0D);
        double armor = Math.clamp(stats.armor() / 20.0D, 0.0D, 1.0D);
        double movement = Math.clamp(stats.movementSpeedBonus() / 30.0D, 0.0D, 1.0D);
        return 0.55D * hp + 0.35D * armor + 0.10D * movement;
    }

    public static double utilityPower(StatEngine.CachedStats stats) {
        if (stats == null) return 0.0D;
        double reach = Math.clamp((stats.entityReach() - BalanceModel.BASE_ENTITY_REACH) / 2.0D, 0.0D, 1.0D);
        double movement = Math.clamp(stats.movementSpeedBonus() / 30.0D, 0.0D, 1.0D);
        return 0.60D * reach + 0.40D * movement;
    }

    public static double groupPower(Iterable<StatEngine.CachedStats> stats) {
        double highest = 0.0D, sum = 0.0D;
        int count = 0;
        for (StatEngine.CachedStats value : stats) {
            double ppi = calculate(value);
            highest = Math.max(highest, ppi);
            sum += ppi;
            count++;
        }
        return count == 0 ? 0.0D : 0.70D * highest + 0.30D * (sum / count);
    }

    public static double gearMultiplier(StatEngine.CachedStats stats) {
        return 0.85D + 0.30D * calculate(stats);
    }
}
