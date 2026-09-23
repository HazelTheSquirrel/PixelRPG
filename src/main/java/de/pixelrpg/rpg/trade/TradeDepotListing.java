package de.pixelrpg.rpg.trade;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record TradeDepotListing(UUID id, UUID sellerId, ItemStack item, double price, long expiresAtMillis) {
    public TradeDepotListing {
        if (id == null || sellerId == null || item == null || item.isEmpty()) throw new IllegalArgumentException("Trade listing requires id, seller and item");
        if (!Double.isFinite(price) || price <= 0.0D) throw new IllegalArgumentException("Trade price must be positive and finite");
        item = item.clone();
    }

    public boolean expired(long now) {
        return expiresAtMillis <= now;
    }

    public ItemStack itemCopy() {
        return item.clone();
    }
}
