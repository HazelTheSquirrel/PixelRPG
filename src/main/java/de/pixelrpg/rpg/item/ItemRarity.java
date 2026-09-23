package de.pixelrpg.rpg.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum ItemRarity {
    COMMON(Component.text("Gewöhnlich", NamedTextColor.GRAY), 1.00, 70),
    UNCOMMON(Component.text("Ungewöhnlich", NamedTextColor.GREEN), 1.10, 20),
    RARE(Component.text("Selten", NamedTextColor.BLUE), 1.22, 8),
    EPIC(Component.text("Episch", NamedTextColor.LIGHT_PURPLE), 1.38, 2),
    LEGENDARY(Component.text("Legendär", NamedTextColor.GOLD), 1.60, 0),
    UNIQUE(Component.text("Einzigartig", NamedTextColor.RED), 1.60, 0);

    private final Component displayName;
    private final double statMultiplier;
    private final int dropWeight;

    ItemRarity(Component displayName, double statMultiplier, int dropWeight) {
        this.displayName = displayName; this.statMultiplier = statMultiplier; this.dropWeight = dropWeight;
    }
    public Component displayName() { return displayName; }
    public double getStatMultiplier() { return statMultiplier; }
    public int getDropWeight() { return dropWeight; }
    public boolean isUnique() { return this == UNIQUE; }
    public static ItemRarity rollRandom() { return rollRandomUpTo(LEGENDARY); }
    public static ItemRarity rollRandomUpTo(ItemRarity max) {
        int total = 0;
        for (ItemRarity rarity : values()) if (rarity.ordinal() <= max.ordinal()) total += rarity.dropWeight;
        if (total <= 0) return COMMON;
        int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (ItemRarity rarity : values()) {
            if (rarity.ordinal() > max.ordinal() || rarity.dropWeight <= 0) continue;
            cumulative += rarity.dropWeight;
            if (roll < cumulative) return rarity;
        }
        return COMMON;
    }
}