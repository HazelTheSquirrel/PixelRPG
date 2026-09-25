package de.pixelrpg.rpg.profession;

/**
 * The six profession material tiers defined by the RP progression model.
 */
public enum ProfessionTier {
    STARTER(1, 10, "Leder/Holz"),
    COPPER(11, 20, "Kupfer"),
    IRON(21, 30, "Eisen"),
    DIAMOND(31, 40, "Diamant"),
    NETHERITE(41, 50, "Netherite"),
    CUSTOM_ENDGAME(51, 60, "Custom-Endgame");

    private final int minLevel;
    private final int maxLevel;
    private final String material;

    ProfessionTier(int minLevel, int maxLevel, String material) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.material = material;
    }

    public int minLevel() { return minLevel; }
    public int maxLevel() { return maxLevel; }
    public String material() { return material; }

    public static ProfessionTier forLevel(int level) {
        int clamped = Math.clamp(level, Profession.MIN_LEVEL, Profession.MAX_LEVEL);
        return switch ((clamped - 1) / 10) {
            case 0 -> STARTER;
            case 1 -> COPPER;
            case 2 -> IRON;
            case 3 -> DIAMOND;
            case 4 -> NETHERITE;
            default -> CUSTOM_ENDGAME;
        };
    }
}
