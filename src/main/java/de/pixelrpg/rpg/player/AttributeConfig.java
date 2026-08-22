package de.pixelrpg.rpg.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.configuration.file.FileConfiguration;

public final class AttributeConfig {
    private static int[] levelRequirements = {1, 10, 20, 30, 40, 50, 60};
    private static double baseCost = 30.0;
    private static double costMultiplier = 1.40;
    private static double soulviewCost = 500.0;
    private static double elytraPermitCost = 750.0;
    private static double classDiscount = 0.75;

    public static double VITALITY_HP_PER_POINT = 4.0;
    public static double AGILITY_SPEED_PER_POINT = 0.008;
    public static double AGILITY_CRIT_PER_POINT = 1.5;
    public static double PRECISION_DAMAGE_PER_POINT = 1.2;
    public static double RANGE_BLOCK_PER_POINT = 0.15;
    public static double RANGE_ENTITY_PER_POINT = 0.20;
    public static double TOUGHNESS_ARMOR_PER_POINT = 2.0;

    private AttributeConfig() { }

    /** Loads attribute calculation values from the user-editable JSON data file. */
    public static void load(JsonDataManager dataManager) {
        JsonObject root = dataManager.load("attributes.json");
        JsonObject cost = root.getAsJsonObject("cost");
        JsonObject scaling = root.getAsJsonObject("pointScaling");

        baseCost = number(cost, "base", baseCost);
        costMultiplier = number(cost, "multiplier", costMultiplier);
        classDiscount = number(cost, "classDiscount", classDiscount);
        soulviewCost = number(cost, "soulview", soulviewCost);
        elytraPermitCost = number(cost, "elytraPermit", elytraPermitCost);

        VITALITY_HP_PER_POINT = number(scaling, "vitalityHp", VITALITY_HP_PER_POINT);
        AGILITY_SPEED_PER_POINT = number(scaling, "agilitySpeed", AGILITY_SPEED_PER_POINT);
        AGILITY_CRIT_PER_POINT = number(scaling, "agilityCrit", AGILITY_CRIT_PER_POINT);
        PRECISION_DAMAGE_PER_POINT = number(scaling, "precisionDamage", PRECISION_DAMAGE_PER_POINT);
        RANGE_BLOCK_PER_POINT = number(scaling, "rangeBlock", RANGE_BLOCK_PER_POINT);
        RANGE_ENTITY_PER_POINT = number(scaling, "rangeEntity", RANGE_ENTITY_PER_POINT);
        TOUGHNESS_ARMOR_PER_POINT = number(scaling, "toughnessArmor", TOUGHNESS_ARMOR_PER_POINT);

        JsonArray requirements = root.getAsJsonArray("levelRequirements");
        if (requirements != null && !requirements.isEmpty()) {
            levelRequirements = requirements.asList().stream().mapToInt(element -> element.getAsInt()).toArray();
        }
    }

    /** Compatibility loader for the legacy main config; JSON should be preferred. */
    public static void load(FileConfiguration config) {
        baseCost = config.getDouble("attributes.base-cost", baseCost);
        costMultiplier = config.getDouble("attributes.cost-multiplier", costMultiplier);
        classDiscount = config.getDouble("attributes.class-discount", classDiscount);
        soulviewCost = config.getDouble("attributes.soulview-cost", soulviewCost);
        VITALITY_HP_PER_POINT = config.getDouble("attributes.per-point.vitality-hp", VITALITY_HP_PER_POINT);
        AGILITY_SPEED_PER_POINT = config.getDouble("attributes.per-point.agility-speed", AGILITY_SPEED_PER_POINT);
        AGILITY_CRIT_PER_POINT = config.getDouble("attributes.per-point.agility-crit", AGILITY_CRIT_PER_POINT);
        PRECISION_DAMAGE_PER_POINT = config.getDouble("attributes.per-point.precision-damage", PRECISION_DAMAGE_PER_POINT);
        RANGE_BLOCK_PER_POINT = config.getDouble("attributes.per-point.range-block", RANGE_BLOCK_PER_POINT);
        RANGE_ENTITY_PER_POINT = config.getDouble("attributes.per-point.range-entity", RANGE_ENTITY_PER_POINT);
        TOUGHNESS_ARMOR_PER_POINT = config.getDouble("attributes.per-point.toughness-armor", TOUGHNESS_ARMOR_PER_POINT);
    }

    private static double number(JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isNumber() ? object.get(key).getAsDouble() : fallback;
    }

    public static void configureElytraCost(double cost) { elytraPermitCost = cost; }

    public static int levelRequirementForPoint(PlayerAttribute attribute, int currentPoints) {
        if (attribute == PlayerAttribute.SOULVIEW || attribute == PlayerAttribute.ELYTRA_PERMIT) return 60;
        return levelRequirements[Math.min(Math.max(0, currentPoints), levelRequirements.length - 1)];
    }

    public static boolean isPrimaryFor(PlayerClass playerClass, PlayerAttribute attribute) {
        return switch (playerClass) {
            case WARRIOR -> attribute == PlayerAttribute.TOUGHNESS || attribute == PlayerAttribute.VITALITY;
            case RANGER -> attribute == PlayerAttribute.AGILITY || attribute == PlayerAttribute.RANGE;
            case ROGUE -> attribute == PlayerAttribute.AGILITY || attribute == PlayerAttribute.PRECISION;
            case HEALER -> attribute == PlayerAttribute.VITALITY;
            case MAGE -> attribute == PlayerAttribute.PRECISION;
            default -> false;
        };
    }

    public static double costFor(PlayerAttribute attribute, int currentPoints, PlayerClass playerClass) {
        double base = switch (attribute) {
            case SOULVIEW -> soulviewCost;
            case ELYTRA_PERMIT -> elytraPermitCost;
            default -> Math.round(baseCost * Math.pow(costMultiplier, currentPoints) * 100.0) / 100.0;
        };
        if (isPrimaryFor(playerClass, attribute)) base = Math.round(base * classDiscount * 100.0) / 100.0;
        return base;
    }

    public static double costFor(PlayerAttribute attribute, int currentPoints) {
        return costFor(attribute, currentPoints, PlayerClass.NONE);
    }
}
