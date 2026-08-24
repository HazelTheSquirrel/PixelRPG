package de.pixelrpg.rpg.companion;

/** Immutable passive player-stat contribution of an active normal companion. */
public record CompanionPassiveStats(
        double hp,
        double armor,
        double movementSpeed,
        double reach,
        double damage,
        double crit,
        double critDamage,
        double lifesteal,
        double attackPower
) {
    public static final CompanionPassiveStats EMPTY = new CompanionPassiveStats(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
}
