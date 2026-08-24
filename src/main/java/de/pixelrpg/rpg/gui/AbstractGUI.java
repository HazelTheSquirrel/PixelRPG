// src/main/java/de/pixelrpg/rpg/gui/AbstractGUI.java
package de.pixelrpg.rpg.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractGUI {

    private final Map<Integer, Consumer<InventoryClickEvent>> clickActions = new HashMap<>();
    private final int size;
    private final Component title;
    private Inventory inventory;

    protected AbstractGUI(int size, Component title) {
        this.size = size;
        this.title = title.color(NamedTextColor.BLACK);
    }

    public final Inventory build() {
        GUIHolder holder = new GUIHolder(this);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);
        this.inventory = inv;
        clickActions.clear();
        populate();
        return inv;
    }

    protected abstract void populate();

    protected final void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
        inventory.setItem(slot, item);
        if (onClick != null) {
            clickActions.put(slot, onClick);
        } else {
            clickActions.remove(slot);
        }
    }

    protected final void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public final void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Consumer<InventoryClickEvent> action = clickActions.get(event.getRawSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    public final void open(Player player) {
        player.openInventory(build());
    }

    protected void onClose(Player player) {
    }
}
