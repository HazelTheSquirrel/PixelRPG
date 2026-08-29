package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.shop.ShopEntry;
import de.pixelrpg.rpg.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ShopGUI extends AbstractGUI {
    private static final double ADMIN_SELL_RATIO = 0.50D;
    private final Player viewer;
    private final String npcId;
    private final ShopManager shopManager;
    private final PlayerProfileManager profileManager;

    public ShopGUI(Player viewer, String npcId, ShopManager shopManager, PlayerProfileManager profileManager) {
        super(54, Component.text("Shop", NamedTextColor.GOLD));
        this.viewer = viewer;
        this.npcId = npcId;
        this.shopManager = shopManager;
        this.profileManager = profileManager;
    }

    @Override protected void populate() {
        List<ShopEntry> entries = shopManager.getEntries(npcId);
        int slot = 0;
        for (ShopEntry entry : entries) {
            if (slot >= 54) break;
            ItemStack display = entry.item().clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Preis: " + entry.price() + " Gold", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Linksklick: Kaufen", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Rechtsklick: Verkaufen für " + format(entry.price() * ADMIN_SELL_RATIO) + " Gold", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            display.setItemMeta(meta);
            setItem(slot, display, event -> handleShopClick(event, entry));
            slot++;
        }
    }

    private void handleShopClick(InventoryClickEvent event, ShopEntry entry) {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) return;
        if (event.isRightClick()) {
            if (!removeOneMatchingItem(viewer, entry.item())) {
                viewer.sendMessage(Component.text("Du hast dieses Item nicht im Inventar.", NamedTextColor.RED));
                return;
            }
            double payout = entry.price() * ADMIN_SELL_RATIO;
            profile.addMoney(payout);
            viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 0.9f);
            viewer.sendMessage(Component.text("Item für " + format(payout) + " Gold verkauft.", NamedTextColor.GREEN));
            return;
        }
        if (!profile.removeMoney(entry.price())) {
            viewer.sendMessage(Component.text("Du hast nicht genügend Gold.", NamedTextColor.RED));
            return;
        }
        viewer.getInventory().addItem(entry.item().clone()).values().forEach(remainder -> viewer.getWorld().dropItemNaturally(viewer.getLocation(), remainder));
        viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        viewer.sendMessage(Component.text("Gekauft!", NamedTextColor.GREEN));
    }

    private boolean removeOneMatchingItem(Player player, ItemStack template) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack current = contents[slot];
            if (current == null || current.isEmpty() || !current.isSimilar(template)) continue;
            if (current.getAmount() == 1) player.getInventory().setItem(slot, null);
            else {
                ItemStack updated = current.clone();
                updated.setAmount(updated.getAmount() - 1);
                player.getInventory().setItem(slot, updated);
            }
            return true;
        }
        return false;
    }

    private String format(double amount) { return String.format("%.2f", amount); }
}
