package de.pixelrpg.rpg.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** PixelRPG item rarity tiers. Unique is reserved for one-of-a-kind items and is never rolled randomly. */
public enum ItemRarity {

    COMMON(Component.text("Common", NamedTextColor.GRAY), 1.00, 70),
    UNCOMMON(Component.text("Uncommon", NamedTextColor.GREEN), 1.10, 20),
    RARE(Component.text("Rare", NamedTextColor.BLUE), 1.22, 8),
    EPIC(Component.text("Epic", NamedTextColor.LIGHT_PURPLE), 1.38, 2),
    LEGENDARY(Component.text("Legendary", NamedTextColor.GOLD), 1.60, 0),
    UNIQUE(Component.text("Unique", NamedTextColor.RED), 1.60, 0);

    private final Component displayName;
    private final double statMultiplier;
    private final int dropWeight;

    ItemRarity(Component displayName, double statMultiplier, int dropWeight) {
        this.displayName = displayName;
        this.statMultiplier = statMultiplier;
        this.dropWeight = dropWeight;
    }

    public Component displayName() {
        return displayName;
    }

    public double getStatMultiplier() {
        return statMultiplier;
    }

    public int getDropWeight() {
        return dropWeight;
    }

    public boolean isMax() {
        return this == UNIQUE;
    }

    public boolean isUnique() {
        return this == UNIQUE;
    }

    public ItemRarity next() {
        int nextOrdinal = ordinal() + 1;
        ItemRarity[] values = values();
        return nextOrdinal < values.length ? values[nextOrdinal] : this;
    }

    public static ItemRarity rollRandom() {
        return rollRandomUpTo(LEGENDARY);
    }

    public static ItemRarity rollRandomUpTo(ItemRarity maxRarity) {
        int totalWeight = 0;
        for (ItemRarity rarity : values()) {
            if (rarity.ordinal() <= maxRarity.ordinal()) totalWeight += rarity.dropWeight;
        }
        if (totalWeight <= 0) return COMMON;

        int roll = (int) (Math.random() * totalWeight);
        int cumulative = 0;
        for (ItemRarity rarity : values()) {
            if (rarity.ordinal() > maxRarity.ordinal() || rarity.dropWeight <= 0) continue;
            cumulative += rarity.dropWeight;
            if (roll < cumulative) return rarity;
        }
        return COMMON;
    }
}
