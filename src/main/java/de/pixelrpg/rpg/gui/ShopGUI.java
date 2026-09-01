package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.economy.Money;
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
import java.util.Locale;

public final class ShopGUI extends AbstractGUI {
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

    @Override
    protected void populate() {
        List<ShopEntry> entries = shopManager.getEntries(npcId);
        int slot = 0;
        for (ShopEntry entry : entries) {
            if (slot >= 54) break;
            ItemStack display = entry.item().clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Kaufen: " + format(entry.buyPrice()) + " Gold", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Verkaufen: " + format(entry.sellPrice()) + " Gold", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Linksklick: Kaufen", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Rechtsklick: Verkaufen", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            display.setItemMeta(meta);
            setItem(slot, display, event -> handleShopClick(event, entry));
            slot++;
        }
    }

    private void handleShopClick(InventoryClickEvent event, ShopEntry entry) {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;

        if (event.isRightClick()) {
            long payoutMinorUnits = Money.fromMajor(entry.sellPrice());
            if (!removeOneMatchingItem(viewer, entry.item())) {
                viewer.sendMessage(Component.text("Du hast dieses Item nicht im Inventar.", NamedTextColor.RED));
                return;
            }
            profile.setMoneyMinorUnits(safeAdd(profile.getMoneyMinorUnits(), payoutMinorUnits));
            viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 0.9f);
            viewer.sendMessage(Component.text("Item für " + format(Money.toMajor(payoutMinorUnits)) + " Gold verkauft.", NamedTextColor.GREEN));
            return;
        }

        long priceMinorUnits = Money.fromMajor(entry.buyPrice());
        if (!canFit(viewer, entry.item())) {
            viewer.sendMessage(Component.text("Dein Inventar ist voll.", NamedTextColor.RED));
            return;
        }
        if (profile.getMoneyMinorUnits() < priceMinorUnits) {
            viewer.sendMessage(Component.text("Du hast nicht genügend Gold.", NamedTextColor.RED));
            return;
        }

        profile.setMoneyMinorUnits(profile.getMoneyMinorUnits() - priceMinorUnits);
        viewer.getInventory().addItem(entry.item().clone());
        viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        viewer.sendMessage(Component.text("Gekauft!", NamedTextColor.GREEN));
    }

    private boolean canFit(Player player, ItemStack incoming) {
        int remaining = incoming.getAmount();
        for (ItemStack current : player.getInventory().getStorageContents()) {
            if (current == null || current.isEmpty()) {
                remaining -= incoming.getMaxStackSize();
                if (remaining <= 0) return true;
                continue;
            }
            if (!current.isSimilar(incoming)) continue;
            remaining -= Math.max(0, current.getMaxStackSize() - current.getAmount());
            if (remaining <= 0) return true;
        }
        return false;
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

    private long safeAdd(long current, long delta) {
        return delta > 0L && current > Long.MAX_VALUE - delta ? Long.MAX_VALUE : current + delta;
    }

    private String format(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
