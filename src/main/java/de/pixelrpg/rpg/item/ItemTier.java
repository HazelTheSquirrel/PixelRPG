package de.pixelrpg.rpg.item;

/**
 * Deterministic six-tier item progression shared by equipment, boss rewards and
 * future item definitions. Tiers never gate world regions; they only describe
 * item power and acquisition progression.
 */
public enum ItemTier {
    STARTER(1, 10, "Starter"),
    COPPER(11, 20, "Kupfer"),
    IRON(21, 30, "Eisen"),
    DIAMOND(31, 40, "Diamant"),
    NETHERITE(41, 50, "Netherite"),
    CUSTOM_ENDGAME(51, 60, "Custom-Endgame");

    private final int minLevel;
    private final int maxLevel;
    private final String displayName;

    ItemTier(int minLevel, int maxLevel, String displayName) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.displayName = displayName;
    }

    public int minLevel() { return minLevel; }
    public int maxLevel() { return maxLevel; }
    public String displayName() { return displayName; }

    public static ItemTier forLevel(int level) {
        int clamped = Math.clamp(level, 1, 60);
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
