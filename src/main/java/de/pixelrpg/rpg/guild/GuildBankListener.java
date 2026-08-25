package de.pixelrpg.rpg.guild;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;

/** Persists guild bank contents when members close or interact with the shared storage. */
public final class GuildBankListener implements Listener {
    private final GuildBankStorageService storage;
    private final GuildManager guilds;
    public GuildBankListener(GuildBankStorageService storage, GuildManager guilds) { this.storage = storage; this.guilds = guilds; }

    // Saves the shared guild bank when its inventory is closed.
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuildBankHolder holder)) return;
        storage.save(holder.guildId(), event.getInventory().getContents());
    }

    // Prevents non-members from modifying a guild bank opened through a stale inventory.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuildBankHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !guilds.isMember(holder.guildId(), player.getUniqueId())) event.setCancelled(true);
    }

    // Prevents non-members from dragging items into a guild bank.
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuildBankHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !guilds.isMember(holder.guildId(), player.getUniqueId())) event.setCancelled(true);
    }
}
