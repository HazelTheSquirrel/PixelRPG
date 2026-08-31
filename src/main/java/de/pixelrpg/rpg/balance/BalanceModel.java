package de.pixelrpg.rpg.balance;

import de.pixelrpg.rpg.item.ItemRarity;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Centralized, unit-aware budget model for generated PixelRPG equipment. */
public final class BalanceModel {
    public static final double MAX_HP_BONUS = 180.0D;
    public static final double MAX_ARMOR = 200.0D;
    public static final double MAX_MOVEMENT_SPEED_PERCENT = 30.0D;
    public static final double MAX_REACH_BONUS = 0.5D;
    public static final double MAX_CRIT_CHANCE = 100.0D;
    public static final double MAX_CRIT_DAMAGE_BONUS_PERCENT = 100.0D;
    public static final double MAX_LIFESTEAL_PERCENT = 8.0D;
    public static final double MAX_ATTACK_POWER = 60.0D;

    private static final double MIN_ROLL_QUALITY = 0.75D;
    private static final double MAX_ROLL_QUALITY = 1.00D;

    private BalanceModel() {
    }

    /** Returns the target item power in the range 0.05..1.00 for item levels 1..99. */
    public static double levelPower(int itemLevel) {
        if (itemLevel <= 1) return 0.05D;
        if (itemLevel >= 99) return 1.00D;
        double progress = (itemLevel - 1.0D) / 98.0D;
        return 0.05D + 0.95D * Math.pow(progress, 1.35D);
    }

    /** Returns the normalized rarity budget used before stat-specific allocation. */
    public static double rarityBudget(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.35D;
            case UNCOMMON -> 0.50D;
            case RARE -> 0.68D;
            case EPIC -> 0.84D;
            case LEGENDARY, UNIQUE -> 1.00D;
        };
    }

    /** Returns the maximum number of stat lines permitted by the rarity. */
    public static int maxStatLines(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 2;
            case UNCOMMON -> 3;
            case RARE -> 4;
            case EPIC -> 5;
            case LEGENDARY, UNIQUE -> 8;
        };
    }

    /** Calculates the normalized budget available to one generated item. */
    public static double itemBudget(int itemLevel, ItemRarity rarity) {
        return levelPower(itemLevel) * rarityBudget(rarity);
    }

    /** Splits an item's normalized budget evenly across its selected stats. */
    public static double statBudgetShare(double itemBudget, int statCount) {
        if (statCount <= 0) return 0.0D;
        return itemBudget / statCount;
    }

    /** Applies a bounded quality roll without allowing a roll to exceed the allocated budget. */
    public static double rollQuality(double budgetShare) {
        if (budgetShare <= 0.0D) return 0.0D;
        return budgetShare * ThreadLocalRandom.current().nextDouble(MIN_ROLL_QUALITY, Math.nextUp(MAX_ROLL_QUALITY));
    }

    /** Converts a normalized stat share into the stat's native gameplay unit. */
    public static double value(EquipmentStat stat, double budgetShare) {
        return switch (stat) {
            case HP -> MAX_HP_BONUS * budgetShare;
            case ARMOR -> MAX_ARMOR * budgetShare;
            case MOVEMENT_SPEED -> MAX_MOVEMENT_SPEED_PERCENT * budgetShare;
            case REACH -> MAX_REACH_BONUS * budgetShare;
            case ATTACK_POWER -> MAX_ATTACK_POWER * budgetShare;
            case CRIT -> MAX_CRIT_CHANCE * budgetShare;
            case CRIT_DAMAGE -> MAX_CRIT_DAMAGE_BONUS_PERCENT * budgetShare;
            case LIFESTEAL -> MAX_LIFESTEAL_PERCENT * budgetShare;
        };
    }

    /** Returns the sensible stat pool for the equipment category. */
    public static Set<EquipmentStat> pool(boolean weapon, boolean armor, boolean shield) {
        EnumSet<EquipmentStat> result = EnumSet.noneOf(EquipmentStat.class);
        if (weapon) result.addAll(EnumSet.of(EquipmentStat.ATTACK_POWER, EquipmentStat.CRIT,
                EquipmentStat.CRIT_DAMAGE, EquipmentStat.LIFESTEAL, EquipmentStat.REACH, EquipmentStat.HP));
        if (armor) result.addAll(EnumSet.of(EquipmentStat.HP, EquipmentStat.ARMOR,
                EquipmentStat.MOVEMENT_SPEED, EquipmentStat.REACH, EquipmentStat.CRIT, EquipmentStat.LIFESTEAL));
        if (shield) result.addAll(EnumSet.of(EquipmentStat.HP, EquipmentStat.ARMOR,
                EquipmentStat.REACH, EquipmentStat.LIFESTEAL));
        return result;
    }

    public enum EquipmentStat {
        HP,
        ARMOR,
        MOVEMENT_SPEED,
        REACH,
        ATTACK_POWER,
        CRIT,
        CRIT_DAMAGE,
        LIFESTEAL
    }
}
