package de.pixelrpg.rpg.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class GUIListener implements Listener {

    // Zuständig für die Weiterleitung von Inventar-Klicks an das aktive PixelRPG-GUI.
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GUIHolder holder)) return;
        holder.getGui().handleClick(event);
    }

    // Zuständig für die Bereinigung eines PixelRPG-GUIs beim Schließen des Inventars.
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GUIHolder holder)) return;
        if (event.getPlayer() instanceof Player player) holder.getGui().onClose(player);
    }
}
