// src/main/java/de/pixelrpg/rpg/combat/gem/SupportGemModifiers.java
package de.pixelrpg.rpg.combat.gem;

public record SupportGemModifiers(
        double damageMultiplier,
        double cooldownMultiplier,
        boolean applyBurn,
        boolean applySlow,
        double aoeRadiusBonus,
        double lifestealPercent
) {

    public static final SupportGemModifiers NEUTRAL =
            new SupportGemModifiers(1.0, 1.0, false, false, 0.0, 0.0);

    public SupportGemModifiers combine(SupportGemModifiers other) {
        return new SupportGemModifiers(
                this.damageMultiplier * other.damageMultiplier,
                this.cooldownMultiplier * other.cooldownMultiplier,
                this.applyBurn || other.applyBurn,
                this.applySlow || other.applySlow,
                this.aoeRadiusBonus + other.aoeRadiusBonus,
                this.lifestealPercent + other.lifestealPercent
        );
    }
}