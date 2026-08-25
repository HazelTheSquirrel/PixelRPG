package de.pixelrpg.rpg.dialogue;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Identifies a PixelRPG personal bank inventory and its current page. */
public final class BankInventoryHolder implements InventoryHolder {
    private final UUID playerId;
    private final int page;
    private Inventory inventory;

    public BankInventoryHolder(UUID playerId, int page) {
        this.playerId = playerId;
        this.page = page;
    }

    public UUID playerId() {
        return playerId;
    }

    public int page() {
        return page;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
