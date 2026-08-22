package de.pixelrpg.rpg.dialogue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/** Handles navigation and persistence for the two-page personal bank inventory. */
public final class BankInventoryListener implements Listener {
    private static final int BACK_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final BankStorageService storage;

    public BankInventoryListener(BankStorageService storage) {
        this.storage = storage;
    }

    /** Handles bank navigation buttons and prevents players from moving them. */
    @EventHandler
    public void onBankInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BankInventoryHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;

        if (rawSlot == BACK_SLOT && holder.page() == 1) {
            event.setCancelled(true);
            openPage(player, holder.playerId(), 0);
            return;
        }

        if (rawSlot == NEXT_SLOT && holder.page() == 0) {
            event.setCancelled(true);
            openPage(player, holder.playerId(), 1);
        }
    }

    /** Saves the player's bank contents when the custom bank inventory closes. */
    @EventHandler
    public void onBankInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BankInventoryHolder holder)) return;
        storage.save(holder.playerId(), collectContents(holder.playerId(), event.getInventory(), holder.page()));
    }

    private void openPage(Player player, UUID playerId, int page) {
        ItemStack[] contents = storage.load(playerId);
        BankInventoryHolder holder = new BankInventoryHolder(playerId, page);
        var inventory = Bukkit.createInventory(holder, BankStorageService.PAGE_SIZE,
                Component.text("Bankfach – Seite " + (page + 1), NamedTextColor.GOLD));
        holder.inventory(inventory);

        int offset = page * BankStorageService.PAGE_SIZE;
        for (int slot = 0; slot < BankStorageService.PAGE_SIZE; slot++) {
            inventory.setItem(slot, contents[offset + slot]);
        }

        if (page == 1) inventory.setItem(BACK_SLOT, navigationHead("MHF_ArrowLeft", "Zurück"));
        if (page == 0) inventory.setItem(NEXT_SLOT, navigationHead("MHF_ArrowRight", "Weiter"));

        player.openInventory(inventory);
    }

    private ItemStack[] collectContents(UUID playerId, org.bukkit.inventory.Inventory inventory, int page) {
        ItemStack[] contents = storage.load(playerId);
        int offset = page * BankStorageService.PAGE_SIZE;
        for (int slot = 0; slot < BankStorageService.PAGE_SIZE; slot++) {
            if (page == 0 && slot == NEXT_SLOT) continue;
            if (page == 1 && slot == BACK_SLOT) continue;
            contents[offset + slot] = inventory.getItem(slot);
        }
        return contents;
    }

    private ItemStack navigationHead(String profileName, String displayName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        var profile = Bukkit.createProfile(profileName);
        meta.setPlayerProfile(profile);
        meta.displayName(Component.text(displayName, NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }
}
