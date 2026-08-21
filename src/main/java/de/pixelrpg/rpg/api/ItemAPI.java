// src/main/java/de/pixelrpg/rpg/api/ItemAPI.java
package de.pixelrpg.rpg.api;

import de.pixelrpg.rpg.item.ItemCategory;
import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface ItemAPI {

    Optional<ItemStack> createUnidentifiedItem(Material material, ItemRarity rarity, int itemLevel);

    ItemStack identify(ItemStack unidentifiedItem);

    boolean isIdentified(ItemStack item);

    boolean isGuildItem(ItemStack item);

    Optional<String> getItemId(ItemStack item);

    Optional<ItemRarity> getRarity(ItemStack item);

    Optional<ItemCategory> getCategory(ItemStack item);
}
