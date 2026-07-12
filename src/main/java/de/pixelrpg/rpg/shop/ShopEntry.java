// src/main/java/de/pixelrpg/rpg/shop/ShopEntry.java
package de.pixelrpg.rpg.shop;

import org.bukkit.inventory.ItemStack;

public record ShopEntry(ItemStack item, double price) {
}