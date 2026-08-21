// src/main/java/de/pixelrpg/rpg/item/ItemService.java
package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class ItemService implements ItemAPI {

    @Override
    public Optional<ItemStack> createItem(Material material, ItemRarity rarity, int itemLevel) {
        return RPGItemBuilder.createItem(material, rarity, itemLevel);
    }

    @Override
    public boolean isRPGItem(ItemStack item) {
        return getItemId(item).isPresent();
    }

    @Override
    public boolean isGuildItem(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        return Boolean.TRUE.equals(item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN));
    }

    @Override
    public Optional<String> getItemId(ItemStack item) {
        if (!item.hasItemMeta()) return Optional.empty();
        String value = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.itemId(), PersistentDataType.STRING);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    @Override
    public Optional<ItemRarity> getRarity(ItemStack item) {
        if (!item.hasItemMeta()) return Optional.empty();
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.rarity(), PersistentDataType.STRING);
        if (raw == null) return Optional.empty();
        try {
            return Optional.of(ItemRarity.valueOf(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ItemCategory> getCategory(ItemStack item) {
        if (!item.hasItemMeta()) return Optional.empty();
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.category(), PersistentDataType.STRING);
        if (raw == null) return Optional.empty();
        try {
            return Optional.of(ItemCategory.valueOf(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
