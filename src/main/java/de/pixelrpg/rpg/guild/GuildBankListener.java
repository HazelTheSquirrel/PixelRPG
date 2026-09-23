package de.pixelrpg.rpg.guild;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GuildBankListener implements Listener {
    private final GuildBankStorageService storage;
    private final GuildManager guilds;
    public GuildBankListener(GuildBankStorageService storage, GuildManager guilds) { this.storage = storage; this.guilds = guilds; }

    // Persistiert den gemeinsamen Gildenbankbestand beim Schließen des Bankinventars.
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuildBankHolder holder) storage.save(holder.guildId(), event.getInventory().getContents());
    }

    // Verhindert Änderungen durch Spieler, die nicht mehr Mitglied der geöffneten Gilde sind.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuildBankHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !guilds.isMember(holder.guildId(), player.getUniqueId())) event.setCancelled(true);
    }

    // Verhindert Drag-Änderungen durch Spieler, die nicht mehr Mitglied der Gilde sind.
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuildBankHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !guilds.isMember(holder.guildId(), player.getUniqueId())) event.setCancelled(true);
    }
}
