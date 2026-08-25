package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.trade.TradeDepotManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Inventory-based item selector for Trade Depot listings. */
public final class TradeDepotSellGUI extends AbstractGUI {
    private final Player viewer;
    private final TradeDepotManager manager;
    private final ItemAPI itemAPI;

    public TradeDepotSellGUI(Player viewer, TradeDepotManager manager) {
        super(54, Component.text("Handelsware auswählen", NamedTextColor.GOLD));
        this.viewer = viewer;
        this.manager = manager;
        this.itemAPI = Bukkit.getServicesManager().load(ItemAPI.class);
    }

    @Override
    protected void populate() {
        ItemStack[] contents = viewer.getInventory().getStorageContents();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = contents[slot];
            if (!isTradeable(item)) continue;

            ItemStack display = item.clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(Component.text("Klicken zum Auswählen", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Der gesamte Stapel wird angeboten.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            display.setItemMeta(meta);

            final int inventorySlot = slot;
            setItem(slot, display, event -> manager.openSellDialog(viewer, inventorySlot, item.clone()));
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("Handelsware auswählen", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        infoMeta.lore(List.of(
                Component.text("Wähle ein PixelRPG-Item aus deinem Inventar.", NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Rüstung, Vanilla-Items und nicht handelbare Items werden nicht angeboten.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        info.setItemMeta(infoMeta);
        setItem(49, info);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Zurück", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        close.setItemMeta(closeMeta);
        setItem(53, close, event -> manager.open(viewer));
    }

    private boolean isTradeable(ItemStack item) {
        return item != null && !item.isEmpty() && itemAPI != null && itemAPI.isRPGItem(item);
    }
}
