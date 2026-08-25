package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.ItemRarity;

import java.util.concurrent.ThreadLocalRandom;

/** Rolls the quality of a profession-crafted item; Legendary is hard-capped at 0.1%. */
public final class CraftingRarityRoller {
    private static final double COMMON = 65.0D;
    private static final double UNCOMMON = 23.0D;
    private static final double RARE = 9.0D;
    private static final double EPIC = 2.9D;
    private static final double LEGENDARY = 0.1D;

    private CraftingRarityRoller() { }

    public static ItemRarity roll(ItemRarity maximum) {
        if (maximum == null || maximum == ItemRarity.COMMON) return ItemRarity.COMMON;
        double total = 0.0D;
        if (maximum.ordinal() >= ItemRarity.COMMON.ordinal()) total += COMMON;
        if (maximum.ordinal() >= ItemRarity.UNCOMMON.ordinal()) total += UNCOMMON;
        if (maximum.ordinal() >= ItemRarity.RARE.ordinal()) total += RARE;
        if (maximum.ordinal() >= ItemRarity.EPIC.ordinal()) total += EPIC;
        if (maximum.ordinal() >= ItemRarity.LEGENDARY.ordinal()) total += LEGENDARY;

        double roll = ThreadLocalRandom.current().nextDouble(total);
        if ((roll -= COMMON) < 0.0D) return ItemRarity.COMMON;
        if (maximum.ordinal() >= ItemRarity.UNCOMMON.ordinal() && (roll -= UNCOMMON) < 0.0D) return ItemRarity.UNCOMMON;
        if (maximum.ordinal() >= ItemRarity.RARE.ordinal() && (roll -= RARE) < 0.0D) return ItemRarity.RARE;
        if (maximum.ordinal() >= ItemRarity.EPIC.ordinal() && (roll -= EPIC) < 0.0D) return ItemRarity.EPIC;
        if (maximum.ordinal() >= ItemRarity.LEGENDARY.ordinal() && (roll -= LEGENDARY) < 0.0D) return ItemRarity.LEGENDARY;
        return ItemRarity.COMMON;
    }
}
