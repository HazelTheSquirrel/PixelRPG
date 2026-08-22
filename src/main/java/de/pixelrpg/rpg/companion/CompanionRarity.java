package de.pixelrpg.rpg.companion;

/** Rarity controls the companion's fixed power budget and progression curve. */
public enum CompanionRarity {
    COMMON(1.00, 1.00),
    UNCOMMON(1.10, 1.10),
    RARE(1.25, 1.25),
    EPIC(1.50, 1.50),
    LEGENDARY(2.00, 2.00),
    UNIQUE(1.75, 1.75);

    private final double statMultiplier;
    private final double experienceMultiplier;

    CompanionRarity(double statMultiplier, double experienceMultiplier) {
        this.statMultiplier = statMultiplier;
        this.experienceMultiplier = experienceMultiplier;
    }

    public double statMultiplier() {
        return statMultiplier;
    }

    public double experienceMultiplier() {
        return experienceMultiplier;
    }

    public boolean isUnique() {
        return this == UNIQUE;
    }
}
