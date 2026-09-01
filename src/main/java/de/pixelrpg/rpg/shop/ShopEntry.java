package de.pixelrpg.rpg.shop;

import org.bukkit.inventory.ItemStack;

/** Immutable shop offer with independent buy and sell prices. */
public record ShopEntry(ItemStack item, double buyPrice, double sellPrice) {
    public ShopEntry {
        if (item == null || item.isEmpty()) {
            throw new IllegalArgumentException("Shop item must not be empty");
        }
        if (!Double.isFinite(buyPrice) || buyPrice < 0.0D) {
            throw new IllegalArgumentException("Buy price must be finite and non-negative");
        }
        if (!Double.isFinite(sellPrice) || sellPrice < 0.0D) {
            throw new IllegalArgumentException("Sell price must be finite and non-negative");
        }
        item = item.clone();
    }

    /** Backwards-compatible constructor for existing callers; old shops used a 50% sell ratio. */
    public ShopEntry(ItemStack item, double buyPrice) {
        this(item, buyPrice, buyPrice * 0.50D);
    }

    /** Compatibility accessor for code that still treats the buy price as the shop price. */
    public double price() {
        return buyPrice;
    }
}
