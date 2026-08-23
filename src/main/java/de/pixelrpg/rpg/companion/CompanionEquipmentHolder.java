package de.pixelrpg.rpg.companion;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;
import java.util.UUID;

/** Identifies the inventory belonging to one player's companion. */
public final class CompanionEquipmentHolder implements InventoryHolder {
    private final UUID playerId;
    private final String companionId;
    private Inventory inventory;

    public CompanionEquipmentHolder(UUID playerId, String companionId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.companionId = Objects.requireNonNull(companionId, "companionId");
    }

    public UUID playerId() {
        return playerId;
    }

    public String companionId() {
        return companionId;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
