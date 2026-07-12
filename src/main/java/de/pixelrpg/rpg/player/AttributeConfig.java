// src/main/java/de/pixelrpg/rpg/player/AttributeConfig.java (VOLLSTÄNDIG, ersetzt alte Datei)
package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.core.Rank;

public final class AttributeConfig {

    private AttributeConfig() {
    }

    private static final Rank[] RANK_REQUIREMENTS = {
            Rank.F, Rank.E, Rank.D, Rank.C, Rank.B, Rank.A, Rank.S
    };

    private static final double BASE_COST = 30.0;
    private static final double COST_MULTIPLIER = 1.40;
    private static final double SOULVIEW_COST = 500.0;
    private static double elytraPermitCost = 750.0;
    private static final double CLASS_DISCOUNT = 0.75;

    public static final double VITALITY_HP_PER_POINT = 4.0;
    public static final double AGILITY_SPEED_PER_POINT = 0.008;
    public static final double AGILITY_CRIT_PER_POINT = 1.5;
    public static final double PRECISION_DAMAGE_PER_POINT = 1.2;
    public static final double RANGE_BLOCK_PER_POINT = 0.15;
    public static final double RANGE_ENTITY_PER_POINT = 0.20;
    public static final double TOUGHNESS_ARMOR_PER_POINT = 2.0;

    public static void configureElytraCost(double cost) {
        elytraPermitCost = cost;
    }

    public static Rank rankRequirementForPoint(PlayerAttribute attribute, int currentPoints) {
        if (attribute == PlayerAttribute.SOULVIEW || attribute == PlayerAttribute.ELYTRA_PERMIT) {
            return Rank.S;
        }
        int index = Math.min(currentPoints, RANK_REQUIREMENTS.length - 1);
        return RANK_REQUIREMENTS[index];
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
            case SOULVIEW -> SOULVIEW_COST;
            case ELYTRA_PERMIT -> elytraPermitCost;
            default -> Math.round(BASE_COST * Math.pow(COST_MULTIPLIER, currentPoints) * 100.0) / 100.0;
        };

        if (isPrimaryFor(playerClass, attribute)) {
            base = Math.round(base * CLASS_DISCOUNT * 100.0) / 100.0;
        }
        return base;
    }

    public static double costFor(PlayerAttribute attribute, int currentPoints) {
        return costFor(attribute, currentPoints, PlayerClass.NONE);
    }
}