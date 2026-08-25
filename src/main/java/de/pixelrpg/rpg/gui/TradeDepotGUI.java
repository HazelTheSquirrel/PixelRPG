package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.trade.TradeDepotListing;
import de.pixelrpg.rpg.trade.TradeDepotManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class TradeDepotGUI extends AbstractGUI {
    private final Player viewer;
    private final TradeDepotManager manager;

    public TradeDepotGUI(Player viewer, TradeDepotManager manager, de.pixelrpg.rpg.player.PlayerProfileManager ignoredProfileManager) {
        super(54, Component.text("Handelsdepot", NamedTextColor.GOLD));
        this.viewer = viewer;
        this.manager = manager;
    }

    @Override
    protected void populate() {
        List<TradeDepotListing> listings = manager.listings();
        int slot = 0;
        for (TradeDepotListing listing : listings) {
            if (slot >= 45) break;
            ItemStack display = listing.itemCopy();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(Component.text("Preis: " + format(listing.price()) + " Gold", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Verbleibend: " + remaining(listing.expiresAtMillis()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            boolean own = listing.sellerId().equals(viewer.getUniqueId());
            lore.add(Component.text(own ? "Klicken zum Zurücknehmen" : "Klicken zum Kaufen", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            display.setItemMeta(meta);
            setItem(slot, display, event -> {
                boolean success = own
                        ? manager.cancel(viewer, listing.id())
                        : manager.purchase(viewer, listing.id());
                if (success) open(viewer);
            });
            slot++;
        }

        ItemStack sell = new ItemStack(Material.GOLD_INGOT);
        ItemMeta sellMeta = sell.getItemMeta();
        sellMeta.displayName(Component.text("Handelsware einstellen", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        sellMeta.lore(List.of(Component.text("Klicke hier und wähle anschließend ein Item aus deinem Inventar.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        sell.setItemMeta(sellMeta);
        setItem(49, sell, event -> manager.openSellSelection(viewer));

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Schließen", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        close.setItemMeta(closeMeta);
        setItem(53, close, event -> {
            viewer.closeInventory();
            viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
        });
    }

    private String remaining(long expiresAt) {
        long millis = Math.max(0L, expiresAt - System.currentTimeMillis());
        long days = millis / 86_400_000L;
        long hours = (millis % 86_400_000L) / 3_600_000L;
        return days + "d " + hours + "h";
    }

    private String format(double amount) {
        return String.format("%.2f", amount);
    }
}
