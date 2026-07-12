// src/main/java/de/pixelrpg/rpg/item/ItemRarity.java (VOLLSTÄNDIG, ersetzt alte Datei — Sockelanzahl neu gestuft: 0/1/2/3)
package de.pixelrpg.rpg.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum ItemRarity {

    COMMON(Component.text("Common", NamedTextColor.GRAY), 1.0, 0, 70),
    RARE(Component.text("Rare", NamedTextColor.GREEN), 1.4, 1, 22),
    EPIC(Component.text("Epic", NamedTextColor.LIGHT_PURPLE), 2.0, 2, 7),
    LEGENDARY(Component.text("Legendary", NamedTextColor.GOLD), 3.0, 3, 1);

    private final Component displayName;
    private final double statMultiplier;
    private final int socketCount;
    private final int dropWeight;

    ItemRarity(Component displayName, double statMultiplier, int socketCount, int dropWeight) {
        this.displayName = displayName;
        this.statMultiplier = statMultiplier;
        this.socketCount = socketCount;
        this.dropWeight = dropWeight;
    }

    public Component displayName() {
        return displayName;
    }

    public double getStatMultiplier() {
        return statMultiplier;
    }

    public int getSocketCount() {
        return socketCount;
    }

    public int getDropWeight() {
        return dropWeight;
    }

    public ItemRarity next() {
        int nextOrdinal = ordinal() + 1;
        ItemRarity[] values = values();
        return nextOrdinal < values.length ? values[nextOrdinal] : this;
    }

    public boolean isMax() {
        return this == LEGENDARY;
    }

    public static ItemRarity rollRandom() {
        return rollRandomUpTo(LEGENDARY);
    }

    public static ItemRarity rollRandomUpTo(ItemRarity maxRarity) {
        int totalWeight = 0;
        for (ItemRarity rarity : values()) {
            if (rarity.ordinal() <= maxRarity.ordinal()) {
                totalWeight += rarity.dropWeight;
            }
        }
        int roll = (int) (Math.random() * totalWeight);
        int cumulative = 0;
        for (ItemRarity rarity : values()) {
            if (rarity.ordinal() > maxRarity.ordinal()) {
                continue;
            }
            cumulative += rarity.dropWeight;
            if (roll < cumulative) {
                return rarity;
            }
        }
        return COMMON;
    }
}