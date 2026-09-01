package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.shop.ShopEntry;
import de.pixelrpg.rpg.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.util.Locale;

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
            if (slot >= 54) break;
            inv.setItem(slot, applyPriceTags(entry.item().clone(), entry.buyPrice(), entry.sellPrice()));
            slot++;
        }
        player.openInventory(inv);
    }

    private ItemStack applyPriceTags(ItemStack item, double buyPrice, double sellPrice) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(RPGKeys.Item.shopBuyPriceTag(), PersistentDataType.DOUBLE, buyPrice);
        meta.getPersistentDataContainer().set(RPGKeys.Item.shopSellPriceTag(), PersistentDataType.DOUBLE, sellPrice);

        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        stripPriceLore(lore);
        lore.add(Component.text("Kaufpreis: " + format(buyPrice) + " Gold", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Verkaufspreis: " + format(sellPrice) + " Gold", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Linksklick: Kaufpreis +1 | Shift: +10", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Rechtsklick: Verkaufspreis +1 | Shift: +10", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void stripPriceLore(List<Component> lore) {
        lore.removeIf(line -> {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            return plain.startsWith("Preis: ") || plain.startsWith("Price: ")
                    || plain.startsWith("Kaufpreis: ") || plain.startsWith("Verkaufspreis: ")
                    || plain.startsWith("Left-click:") || plain.startsWith("Right-click:")
                    || plain.startsWith("Linksklick:") || plain.startsWith("Rechtsklick:");
        });
    }

    // Zuständig für die getrennte Anpassung von Kauf- und Verkaufspreis im Shop-Editor.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EditorHolder holder)) return;
        if (event.getClickedInventory() != holder.getInventory()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.isEmpty() || event.getCursor().getType() != Material.AIR) return;

        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.SHIFT_LEFT
                && click != ClickType.RIGHT && click != ClickType.SHIFT_RIGHT) return;

        event.setCancelled(true);
        ItemMeta meta = clicked.getItemMeta();
        double buyPrice = meta.getPersistentDataContainer()
                .getOrDefault(RPGKeys.Item.shopBuyPriceTag(), PersistentDataType.DOUBLE, 0.0D);
        double sellPrice = meta.getPersistentDataContainer()
                .getOrDefault(RPGKeys.Item.shopSellPriceTag(), PersistentDataType.DOUBLE, buyPrice * 0.50D);
        double delta = click.isShiftClick() ? 10.0D : 1.0D;

        if (click.isLeftClick()) buyPrice += delta;
        else sellPrice += delta;

        event.getInventory().setItem(event.getSlot(), applyPriceTags(clicked, buyPrice, sellPrice));
    }

    // Zuständig für das Speichern des Shop-Bestands samt unabhängigen Kauf- und Verkaufspreisen beim Schließen.
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof EditorHolder holder)) return;

        List<ShopEntry> entries = new ArrayList<>();
        for (ItemStack stack : event.getInventory().getContents()) {
            if (stack == null || stack.isEmpty() || !stack.hasItemMeta()) continue;

            ItemStack clean = stack.clone();
            ItemMeta meta = clean.getItemMeta();
            double buyPrice = meta.getPersistentDataContainer()
                    .getOrDefault(RPGKeys.Item.shopBuyPriceTag(), PersistentDataType.DOUBLE, 0.0D);
            double sellPrice = meta.getPersistentDataContainer()
                    .getOrDefault(RPGKeys.Item.shopSellPriceTag(), PersistentDataType.DOUBLE, buyPrice * 0.50D);
            meta.getPersistentDataContainer().remove(RPGKeys.Item.shopBuyPriceTag());
            meta.getPersistentDataContainer().remove(RPGKeys.Item.shopSellPriceTag());
            meta.getPersistentDataContainer().remove(RPGKeys.Item.shopPriceTag());

            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            stripPriceLore(lore);
            meta.lore(lore);
            clean.setItemMeta(meta);

            entries.add(new ShopEntry(clean, Math.max(0.0D, buyPrice), Math.max(0.0D, sellPrice)));
        }
        shopManager.replaceEntries(holder.npcId, entries);
    }

    private String format(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
