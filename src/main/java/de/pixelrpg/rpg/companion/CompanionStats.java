package de.pixelrpg.rpg.companion;

/** Immutable final runtime stats for a companion. */
public record CompanionStats(
        double health,
        double damage,
        double movementSpeed,
        double armor,
        double critChance,
        double critDamage,
        double lifesteal,
        double abilityDamage,
        double mana
) {
    public CompanionStats(double health, double damage, double movementSpeed) {
        this(health, damage, movementSpeed, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public CompanionStats add(CompanionStats other) {
        return new CompanionStats(
                health + other.health,
                damage + other.damage,
                movementSpeed + other.movementSpeed,
                armor + other.armor,
                critChance + other.critChance,
                critDamage + other.critDamage,
                lifesteal + other.lifesteal,
                abilityDamage + other.abilityDamage,
                mana + other.mana
        );
    }

    public CompanionStats multiply(double multiplier) {
        return new CompanionStats(
                health * multiplier,
                damage * multiplier,
                movementSpeed * multiplier,
                armor * multiplier,
                critChance * multiplier,
                critDamage * multiplier,
                lifesteal * multiplier,
                abilityDamage * multiplier,
                mana * multiplier
        );
    }
}
