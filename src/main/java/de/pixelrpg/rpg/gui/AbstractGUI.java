package de.pixelrpg.rpg.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Base for PixelRPG inventory views with explicit per-slot actions. */
public abstract class AbstractGUI {
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private final int size;
    private final Component title;
    private Inventory inventory;

    protected AbstractGUI(int size, Component title) {
        if (size < 9 || size % 9 != 0) throw new IllegalArgumentException("GUI size must be a positive multiple of 9");
        this.size = size;
        this.title = title;
    }

    public final Inventory build() {
        GUIHolder holder = new GUIHolder(this);
        inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);
        actions.clear();
        populate();
        return inventory;
    }

    protected abstract void populate();

    protected final void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        if (action == null) actions.remove(slot);
        else actions.put(slot, action);
    }

    protected final void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public final void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) action.accept(event);
    }

    public final void open(Player player) {
        player.openInventory(build());
    }

    protected void onClose(Player player) {
    }
}
