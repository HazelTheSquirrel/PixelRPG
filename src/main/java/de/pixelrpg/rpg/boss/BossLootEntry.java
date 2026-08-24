package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.item.ItemRarity;

public record BossLootEntry(
        String material,
        double chancePercent,
        ItemRarity rarity
) {
    public BossLootEntry {
        chancePercent = Math.max(0.0D, Math.min(100.0D, chancePercent));
        rarity = rarity == null ? ItemRarity.RARE : rarity;
    }
}
