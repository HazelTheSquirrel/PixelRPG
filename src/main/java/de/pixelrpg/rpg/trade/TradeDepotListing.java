package de.pixelrpg.rpg.trade;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

/** Immutable trade-depot listing snapshot. */
public record TradeDepotListing(UUID id, UUID sellerId, ItemStack item, double price, long expiresAtMillis) {
    public TradeDepotListing {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sellerId, "sellerId");
        if (item == null || item.isEmpty()) throw new IllegalArgumentException("item must not be empty");
        if (!Double.isFinite(price) || price <= 0.0D) throw new IllegalArgumentException("price must be positive");
        item = item.clone();
    }

    @Override public ItemStack item() { return item.clone(); }
    public boolean expired(long nowMillis) { return expiresAtMillis <= nowMillis; }
}
