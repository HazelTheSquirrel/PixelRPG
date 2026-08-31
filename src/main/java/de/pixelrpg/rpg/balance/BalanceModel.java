package de.pixelrpg.rpg.balance;

import de.pixelrpg.rpg.item.ItemRarity;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Single source of truth for the PixelRPG v2 balance model.
 * All values are PixelRPG gameplay units; Minecraft conversion belongs to StatEngine.
 */
public final class BalanceModel {
    public static final double BASE_RPG_HP = 100.0D;
    public static final double MAX_RPG_HP = 200.0D;
    public static final double MAX_HP_BONUS = 100.0D;
    public static final double MAX_ARMOR = 20.0D;
    public static final double MAX_MOVEMENT_SPEED_PERCENT = 30.0D;
    public static final double MAX_REACH = 5.0D;
    public static final double BASE_ENTITY_REACH = 3.0D;
    public static final double BASE_BLOCK_REACH = 4.5D;
    public static final double MAX_REACH_BONUS = 2.0D;
    public static final double MAX_CRIT_CHANCE = 100.0D;
    public static final double MAX_CRIT_DAMAGE_BONUS_PERCENT = 100.0D;
    public static final double MAX_LIFESTEAL_PERCENT = 8.0D;
    public static final double MAX_ATTACK_POWER = 15.0D;
    private static final double MIN_ROLL_QUALITY = 0.85D;
    private static final double MAX_ROLL_QUALITY = 1.00D;

    private BalanceModel() {}

    public static int clampLevel(int level) { return Math.clamp(level, 1, 99); }

    /** P(L)=0.04+0.96*((L-1)/98)^1.15. */
    public static double levelPower(int level) {
        int safe = clampLevel(level);
        double x = (safe - 1.0D) / 98.0D;
        return 0.04D + 0.96D * Math.pow(x, 1.15D);
    }

    public static double rarityMultiplier(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.55D;
            case UNCOMMON -> 0.70D;
            case RARE -> 0.82D;
            case EPIC -> 0.92D;
            case LEGENDARY -> 1.00D;
            case UNIQUE -> 1.05D;
        };
    }

    public static double rarityBudget(ItemRarity rarity) { return rarityMultiplier(rarity); }

    public static int minStatLines(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON, UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC, LEGENDARY, UNIQUE -> 4;
        };
    }

    public static int maxStatLines(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 2;
            case UNCOMMON -> 3;
            case RARE -> 4;
            case EPIC, LEGENDARY, UNIQUE -> 5;
        };
    }

    public static double slotWeight(String profile) {
        return switch (profile) {
            case "WEAPON", "CHEST" -> 1.50D;
            case "LEGS" -> 1.25D;
            case "HELMET", "BOOTS" -> 1.15D;
            case "SHIELD" -> 1.45D;
            default -> 1.0D;
        };
    }

    public static double itemBudget(int itemLevel, ItemRarity rarity, double slotWeight) {
        return levelPower(itemLevel) * rarityMultiplier(rarity) * slotWeight;
    }

    public static double itemBudget(int itemLevel, ItemRarity rarity) {
        return itemBudget(itemLevel, rarity, 1.0D);
    }

    public static double statBudgetShare(double itemBudget, int statCount) {
        return statCount <= 0 ? 0.0D : itemBudget / statCount;
    }

    public static double rollQuality(double budgetShare) {
        if (budgetShare <= 0.0D) return 0.0D;
        return budgetShare * ThreadLocalRandom.current().nextDouble(MIN_ROLL_QUALITY, Math.nextUp(MAX_ROLL_QUALITY));
    }

    public static double value(EquipmentStat stat, double budgetShare) {
        return switch (stat) {
            case HP -> 100.0D * budgetShare;
            case ARMOR -> 20.0D * budgetShare;
            case MOVEMENT_SPEED -> 30.0D * budgetShare;
            case REACH -> 2.0D * budgetShare;
            case ATTACK_POWER -> 15.0D * budgetShare;
            case CRIT, CRIT_DAMAGE -> 100.0D * budgetShare;
            case LIFESTEAL -> 8.0D * budgetShare;
        };
    }

    public static double statCap(EquipmentStat stat) {
        return switch (stat) {
            case HP -> 100.0D;
            case ARMOR -> 20.0D;
            case MOVEMENT_SPEED -> 30.0D;
            case REACH -> 2.0D;
            case ATTACK_POWER -> 15.0D;
            case CRIT, CRIT_DAMAGE -> 100.0D;
            case LIFESTEAL -> 8.0D;
        };
    }

    public static double statCost(EquipmentStat stat, double value) { return value / statCap(stat); }
    public static boolean budgetValid(double budget, double used) { return used <= budget + 1.0E-9D; }

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

    public enum EquipmentStat { HP, ARMOR, MOVEMENT_SPEED, REACH, ATTACK_POWER, CRIT, CRIT_DAMAGE, LIFESTEAL }
}
