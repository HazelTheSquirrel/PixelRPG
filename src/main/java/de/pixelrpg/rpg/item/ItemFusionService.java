// src/main/java/de/pixelrpg/rpg/item/ItemFusionService.java
package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class ItemFusionService {

    public enum Result {
        SUCCESS,
        NOT_IDENTIFIED,
        MISMATCHED_ITEMS,
        ALREADY_MAX_RARITY,
        SOULBOUND
    }

    private ItemFusionService() {
    }

    public record FusionOutcome(Result result, ItemStack resultItem) {
    }

    public static FusionOutcome fuse(ItemStack itemA, ItemStack itemB) {
        if (!itemA.hasItemMeta() || !itemB.hasItemMeta()) {
            return new FusionOutcome(Result.NOT_IDENTIFIED, null);
        }

        var pdcA = itemA.getItemMeta().getPersistentDataContainer();
        var pdcB = itemB.getItemMeta().getPersistentDataContainer();

        boolean identifiedA = Boolean.TRUE.equals(pdcA.get(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN));
        boolean identifiedB = Boolean.TRUE.equals(pdcB.get(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN));
        if (!identifiedA || !identifiedB) {
            return new FusionOutcome(Result.NOT_IDENTIFIED, null);
        }

        if (SoulboundService.isSoulbound(itemA) || SoulboundService.isSoulbound(itemB)) {
            return new FusionOutcome(Result.SOULBOUND, null);
        }

        if (itemA.getType() != itemB.getType()) {
            return new FusionOutcome(Result.MISMATCHED_ITEMS, null);
        }

        String rarityRawA = pdcA.get(RPGKeys.Item.rarity(), PersistentDataType.STRING);
        String rarityRawB = pdcB.get(RPGKeys.Item.rarity(), PersistentDataType.STRING);
        if (rarityRawA == null || !rarityRawA.equals(rarityRawB)) {
            return new FusionOutcome(Result.MISMATCHED_ITEMS, null);
        }

        ItemRarity currentRarity = ItemRarity.valueOf(rarityRawA);
        if (currentRarity.isMax()) {
            return new FusionOutcome(Result.ALREADY_MAX_RARITY, null);
        }

        Integer levelA = pdcA.getOrDefault(RPGKeys.Item.itemRank(), PersistentDataType.INTEGER, 1);
        Integer levelB = pdcB.getOrDefault(RPGKeys.Item.itemRank(), PersistentDataType.INTEGER, 1);
        int fusedLevel = Math.max(levelA, levelB) + 1;

        ItemRarity nextRarity = currentRarity.next();
        Optional<ItemStack> unidentified = RPGItemBuilder.createUnidentified(itemA.getType(), nextRarity, fusedLevel);
        if (unidentified.isEmpty()) {
            return new FusionOutcome(Result.MISMATCHED_ITEMS, null);
        }

        ItemStack result = RPGItemBuilder.identify(unidentified.get());
        return new FusionOutcome(Result.SUCCESS, result);
    }
}