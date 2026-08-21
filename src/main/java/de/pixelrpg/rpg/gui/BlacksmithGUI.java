package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.SoulboundService;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Complex blacksmith input is kept as an inventory operation; item identification no longer exists. */
public final class BlacksmithGUI implements Listener {
    private static final int ITEM_SLOT = 13;
    private static final int SOULBOUND_BUTTON = 15;

    private final PlayerProfileManager profileManager;
    private final ItemEconomyConfig economyConfig;
    private final LanguageManager lang;

    public static final class BlacksmithHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public BlacksmithGUI(PlayerProfileManager profileManager, ItemEconomyConfig economyConfig) {
        this.profileManager = profileManager;
        this.economyConfig = economyConfig;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    public void open(Player player) {
        BlacksmithHolder holder = new BlacksmithHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27,
                lang.get("blacksmith.gui-title").color(NamedTextColor.DARK_GRAY));
        holder.inventory = inventory;

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot != ITEM_SLOT && slot != SOULBOUND_BUTTON) inventory.setItem(slot, filler);
        }

        inventory.setItem(SOULBOUND_BUTTON, buildSoulbindButton());
        player.openInventory(inventory);
    }

    private ItemStack buildSoulbindButton() {
        ItemStack item = new ItemStack(Material.SOUL_SAND);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("blacksmith.soulbind-button")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                lang.get("blacksmith.soulbind-desc").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Required Level: " + economyConfig.getSoulboundMinLevel(), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    // Verhindert das Verschieben von GUI-Dekorationen und Schaltflächen per Drag.
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder)) return;
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize() && slot != ITEM_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Verarbeitet ausschließlich die Seelenbindung im Schmiedemenü.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = holder.getInventory();
        if (event.getClick().isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != top) return;

        int slot = event.getSlot();
        if (slot == SOULBOUND_BUTTON) {
            event.setCancelled(true);
            processSoulbound(player, top);
            return;
        }
        if (slot != ITEM_SLOT) event.setCancelled(true);
    }

    private void processSoulbound(Player player, Inventory inventory) {
        ItemStack target = inventory.getItem(ITEM_SLOT);
        if (target == null || target.getType() == Material.AIR || !target.hasItemMeta()) {
            lang.send(player, "blacksmith.place-identified");
            return;
        }

        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        int requiredLevel = economyConfig.getSoulboundMinLevel();
        if (profile.getLevel() < requiredLevel) {
            lang.send(player, "blacksmith.soulbound-requires-level", "level", String.valueOf(requiredLevel));
            return;
        }

        double cost = economyConfig.getSoulboundCost();
        if (!profile.removeMoney(cost)) {
            lang.send(player, "blacksmith.need-gold-soulbind", "cost", String.valueOf(cost));
            return;
        }

        switch (SoulboundService.apply(target)) {
            case SUCCESS -> {
                inventory.setItem(ITEM_SLOT, target);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                lang.send(player, "blacksmith.now-soulbound");
            }
            case ALREADY_SOULBOUND -> {
                profile.addMoney(cost);
                lang.send(player, "blacksmith.already-soulbound");
            }
            case NOT_IDENTIFIED -> {
                profile.addMoney(cost);
                lang.send(player, "blacksmith.only-identified-soulbind");
            }
        }
    }

    // Gibt ein beim Schließen des Schmiedemenüs noch eingelegtes Item sicher an den Spieler zurück.
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        ItemStack leftover = event.getInventory().getItem(ITEM_SLOT);
        if (leftover == null || leftover.getType() == Material.AIR) return;
        event.getInventory().setItem(ITEM_SLOT, null);
        player.getInventory().addItem(leftover).values()
                .forEach(remainder -> player.getWorld().dropItemNaturally(player.getLocation(), remainder));
    }
}
