package de.pixelrpg.rpg.guild;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class GuildBankHolder implements InventoryHolder {
    private final UUID guildId;
    private Inventory inventory;

    public GuildBankHolder(UUID guildId) { this.guildId = guildId; }
    public UUID guildId() { return guildId; }
    public void inventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
