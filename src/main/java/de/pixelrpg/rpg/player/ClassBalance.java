// src/main/java/de/pixelrpg/rpg/player/ClassBalance.java (VOLLSTÄNDIG, ersetzt alte Datei — jetzt live aus Attributpunkten berechnet statt fixer Mini-Werte)
package de.pixelrpg.rpg.player;

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

    public static ClassBalance of(PlayerProfile profile) {
        if (profile == null) {
            return NEUTRAL;
        }

        PlayerClass playerClass = profile.getPlayerClass();
        int vitality = profile.getAttributePoints(PlayerAttribute.VITALITY);
        int agility = profile.getAttributePoints(PlayerAttribute.AGILITY);
        int precision = profile.getAttributePoints(PlayerAttribute.PRECISION);
        int range = profile.getAttributePoints(PlayerAttribute.RANGE);
        int toughness = profile.getAttributePoints(PlayerAttribute.TOUGHNESS);

        return switch (playerClass) {
            case WARRIOR -> new ClassBalance(
                    10.0 + toughness * 1.8,
                    8.0 + vitality * 1.2,
                    0,
                    1.15 + toughness * 0.015,
                    1.0,
                    1.0,
                    1.0,
                    0,
                    1.0
            );
            case RANGER -> new ClassBalance(
                    0,
                    2.0 + vitality * 0.5,
                    0.03 + agility * 0.002,
                    1.0,
                    1.20 + (agility + range) * 0.015,
                    1.0,
                    1.0,
                    5.0 + range * 0.8,
                    1.0
            );
            case ROGUE -> new ClassBalance(
                    0,
                    0,
                    0.02 + agility * 0.002,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    15.0 + (agility + precision) * 1.0,
                    1.6 + precision * 0.03
            );
            case HEALER -> new ClassBalance(
                    3.0 + toughness * 0.5,
                    10.0 + vitality * 1.5,
                    0,
                    1.0,
                    1.0,
                    1.0,
                    1.35 + vitality * 0.025,
                    0,
                    1.0
            );
            case MAGE -> new ClassBalance(
                    -2.0,
                    -2.0 + vitality * 0.5,
                    0,
                    1.0,
                    1.0,
                    1.25 + precision * 0.025,
                    1.0,
                    0,
                    1.0
            );
            default -> NEUTRAL;
        };
    }
}