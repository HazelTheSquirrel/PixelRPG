// src/main/java/de/pixelrpg/rpg/gui/ItemFusionGUI.java
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.ItemFusionService;
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

public final class ItemFusionGUI implements Listener {

    private static final int SLOT_A = 10;
    private static final int SLOT_B = 12;
    private static final int SLOT_RESULT = 16;
    private static final int SLOT_BUTTON = 14;

    private final PlayerProfileManager profileManager;
    private final ItemEconomyConfig economyConfig;

    public static final class FusionHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public ItemFusionGUI(PlayerProfileManager profileManager, ItemEconomyConfig economyConfig) {
        this.profileManager = profileManager;
        this.economyConfig = economyConfig;
    }

    public void open(Player player) {
        FusionHolder holder = new FusionHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Item Fusion", NamedTextColor.DARK_PURPLE));
        holder.inventory = inv;

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 27; i++) {
            if (i != SLOT_A && i != SLOT_B && i != SLOT_RESULT && i != SLOT_BUTTON) {
                inv.setItem(i, filler);
            }
        }

        inv.setItem(SLOT_BUTTON, buildButton());
        player.openInventory(inv);
    }

    private ItemStack buildButton() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Fuse", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Combines two identical items", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("into the next rarity tier.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Cost: " + economyConfig.getFusionCost() + " Gold", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof FusionHolder)) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize() && slot != SLOT_A && slot != SLOT_B) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FusionHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = holder.getInventory();

        if (event.getClick().isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == topInventory) {
            int slot = event.getSlot();
            if (slot == SLOT_BUTTON) {
                event.setCancelled(true);
                processFusion(player, topInventory);
                return;
            }
            if (slot != SLOT_A && slot != SLOT_B && slot != SLOT_RESULT) {
                event.setCancelled(true);
                return;
            }
            if (slot == SLOT_RESULT && (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof FusionHolder)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        for (int slot : new int[]{SLOT_A, SLOT_B, SLOT_RESULT}) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item).values()
                        .forEach(remainder -> player.getWorld().dropItemNaturally(player.getLocation(), remainder));
            }
        }
    }

    private void processFusion(Player player, Inventory topInventory) {
        ItemStack itemA = topInventory.getItem(SLOT_A);
        ItemStack itemB = topInventory.getItem(SLOT_B);

        if (itemA == null || itemB == null || itemA.getType() == Material.AIR || itemB.getType() == Material.AIR) {
            player.sendMessage(Component.text("Place two items in the left slots.", NamedTextColor.RED));
            return;
        }

        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        double cost = economyConfig.getFusionCost();
        if (!profile.removeMoney(cost)) {
            player.sendMessage(Component.text("You need " + cost + " gold for fusion.", NamedTextColor.RED));
            return;
        }

        ItemFusionService.FusionOutcome outcome = ItemFusionService.fuse(itemA, itemB);
        switch (outcome.result()) {
            case SUCCESS -> {
                topInventory.setItem(SLOT_A, null);
                topInventory.setItem(SLOT_B, null);
                topInventory.setItem(SLOT_RESULT, outcome.resultItem());
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                player.sendMessage(Component.text("Fusion successful!", NamedTextColor.GREEN));
            }
            case MISMATCHED_ITEMS -> {
                profile.addMoney(cost);
                player.sendMessage(Component.text("Both items must be identical, identified, same rarity.", NamedTextColor.RED));
            }
            case ALREADY_MAX_RARITY -> {
                profile.addMoney(cost);
                player.sendMessage(Component.text("Legendary items cannot be fused further.", NamedTextColor.RED));
            }
            case SOULBOUND -> {
                profile.addMoney(cost);
                player.sendMessage(Component.text("Soulbound items cannot be fused.", NamedTextColor.RED));
            }
            case NOT_IDENTIFIED -> {
                profile.addMoney(cost);
                player.sendMessage(Component.text("Both items must be identified first.", NamedTextColor.RED));
            }
        }
    }
}