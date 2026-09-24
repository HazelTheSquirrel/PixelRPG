package de.pixelrpg.rpg.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Associates an inventory with its PixelRPG GUI controller. */
public final class GUIHolder implements InventoryHolder {
    private final AbstractGUI gui;
    private Inventory inventory;

    public GUIHolder(AbstractGUI gui) { this.gui = gui; }

    @Override public Inventory getInventory() { return inventory; }
    void setInventory(Inventory inventory) { this.inventory = inventory; }
    public AbstractGUI getGui() { return gui; }
}
