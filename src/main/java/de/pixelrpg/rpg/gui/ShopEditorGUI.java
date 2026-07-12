// src/main/java/de/pixelrpg/rpg/gui/ShopEditorGUI.java
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.shop.ShopEntry;
import de.pixelrpg.rpg.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ShopEditorGUI implements Listener {

    private final ShopManager shopManager;

    public final class EditorHolder implements InventoryHolder {
        private final String npcId;
        private Inventory inventory;

        private EditorHolder(String npcId) {
            this.npcId = npcId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public ShopEditorGUI(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void open(Player player, String npcId) {
        EditorHolder holder = new EditorHolder(npcId);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Shop Editor: " + npcId, NamedTextColor.DARK_GREEN));
        holder.inventory = inv;

        int slot = 0;
        for (ShopEntry entry : shopManager.getEntries(npcId)) {
            if (slot >= 54) {
                break;
            }
            inv.setItem(slot, applyPriceTag(entry.item().clone(), entry.price()));
            slot++;
        }

        player.openInventory(inv);
    }

    private ItemStack applyPriceTag(ItemStack item, double price) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(RPGKeys.Item.shopPriceTag(), PersistentDataType.DOUBLE, price);

        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.removeIf(line -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(line).startsWith("Price: "));
        lore.add(Component.text("Price: " + String.format("%.2f", price) + " Gold", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Left-click: +1 | Shift-left: +10", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right-click: -1 | Shift-right: -10", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EditorHolder holder)) {
            return;
        }
        if (event.getClickedInventory() != holder.getInventory()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || event.getCursor().getType() != Material.AIR) {
            return;
        }

        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.SHIFT_LEFT
                && click != ClickType.RIGHT && click != ClickType.SHIFT_RIGHT) {
            return;
        }

        event.setCancelled(true);
        ItemMeta meta = clicked.getItemMeta();
        double current = meta.getPersistentDataContainer()
                .getOrDefault(RPGKeys.Item.shopPriceTag(), PersistentDataType.DOUBLE, 0.0);

        double delta = switch (click) {
            case LEFT -> 1.0;
            case SHIFT_LEFT -> 10.0;
            case RIGHT -> -1.0;
            case SHIFT_RIGHT -> -10.0;
            default -> 0.0;
        };

        double updated = Math.max(0.0, current + delta);
        event.getInventory().setItem(event.getSlot(), applyPriceTag(clicked, updated));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof EditorHolder holder)) {
            return;
        }

        List<ShopEntry> entries = new ArrayList<>();
        for (ItemStack stack : event.getInventory().getContents()) {
            if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
                continue;
            }
            ItemStack clean = stack.clone();
            ItemMeta meta = clean.getItemMeta();
            double price = meta.getPersistentDataContainer()
                    .getOrDefault(RPGKeys.Item.shopPriceTag(), PersistentDataType.DOUBLE, 0.0);
            meta.getPersistentDataContainer().remove(RPGKeys.Item.shopPriceTag());

            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.removeIf(line -> {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                return plain.startsWith("Price: ") || plain.startsWith("Left-click:") || plain.startsWith("Right-click:");
            });
            meta.lore(lore);
            clean.setItemMeta(meta);

            entries.add(new ShopEntry(clean, price));
        }

        shopManager.replaceEntries(holder.npcId, entries);
    }
}