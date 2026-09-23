package de.pixelrpg.rpg.companion;

public enum CompanionRarity {
    COMMON(1.00,1.00), UNCOMMON(1.10,1.15), RARE(1.22,1.35), EPIC(1.38,1.60), LEGENDARY(1.58,2.00), UNIQUE(1.50,2.00);
    private final double statMultiplier, experienceMultiplier;
    CompanionRarity(double statMultiplier, double experienceMultiplier) { this.statMultiplier=statMultiplier; this.experienceMultiplier=experienceMultiplier; }
    public double statMultiplier(){return statMultiplier;} public double experienceMultiplier(){return experienceMultiplier;} public boolean isUnique(){return this==UNIQUE;}
}
