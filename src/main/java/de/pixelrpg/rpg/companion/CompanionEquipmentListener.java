package de.pixelrpg.rpg.companion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/** Handles the player-facing equipment inventory for Mannequin companions. */
public final class CompanionEquipmentListener implements Listener {
    private static final int SIZE = 27;
    private static final int HELMET = 10;
    private static final int CHESTPLATE = 11;
    private static final int LEGGINGS = 12;
    private static final int BOOTS = 13;
    private static final int OFF_HAND = 15;
    private static final int MAIN_HAND = 16;

    private final CompanionService companionService;
    private final Plugin plugin;

    public CompanionEquipmentListener(Plugin plugin, CompanionService companionService) {
        this.plugin = plugin;
        this.companionService = companionService;
    }

    public void open(Player player, Companion companion) {
        CompanionEquipmentHolder holder = new CompanionEquipmentHolder(player.getUniqueId(), companion.id());
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                Component.text("Ausrüstung: ").append(Component.text(companion.name(), NamedTextColor.GOLD)));
        holder.inventory(inventory);
        fillBackground(inventory);
        CompanionEquipment equipment = companionService.getEquipment(player.getUniqueId(), companion.id());
        inventory.setItem(HELMET, equipment.helmet());
        inventory.setItem(CHESTPLATE, equipment.chestplate());
        inventory.setItem(LEGGINGS, equipment.leggings());
        inventory.setItem(BOOTS, equipment.boots());
        inventory.setItem(OFF_HAND, equipment.offHand());
        inventory.setItem(MAIN_HAND, equipment.mainHand());
        player.openInventory(inventory);
    }

    // Zuständig für sichere Klicks im Companion-Equipment-Inventar.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof CompanionEquipmentHolder)) return;
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < SIZE) {
            if (!isEquipmentSlot(rawSlot)) event.setCancelled(true);
            return;
        }
        if (event.isShiftClick()) event.setCancelled(true);
    }

    // Zuständig dafür, dass Drag-Aktionen keine Dekorations- oder gesperrten Slots verändern.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof CompanionEquipmentHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < SIZE && !isEquipmentSlot(slot))) event.setCancelled(true);
    }

    // Zuständig für das persistente Speichern der Companion-Ausrüstung beim Schließen.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof CompanionEquipmentHolder holder)) return;
        Inventory inventory = event.getInventory();
        companionService.setEquipment(holder.playerId(), holder.companionId(), new CompanionEquipment(
                item(inventory, HELMET),
                item(inventory, CHESTPLATE),
                item(inventory, LEGGINGS),
                item(inventory, BOOTS),
                item(inventory, MAIN_HAND),
                item(inventory, OFF_HAND)
        ));

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Companion active = companionService.getActive(holder.playerId());
            if (active == null || !active.id().equals(holder.companionId())) return;
            var entityId = companionService.getActiveEntity(holder.playerId());
            if (entityId == null) return;
            var entity = plugin.getServer().getEntity(entityId);
            if (entity instanceof org.bukkit.entity.LivingEntity living) {
                companionService.applyEquipmentToEntity(holder.playerId(), holder.companionId(), living);
            }
        });
    }

    private static boolean isEquipmentSlot(int slot) {
        return slot == HELMET || slot == CHESTPLATE || slot == LEGGINGS || slot == BOOTS || slot == OFF_HAND || slot == MAIN_HAND;
    }

    private static ItemStack item(Inventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        filler.setItemMeta(meta);
        for (int slot = 0; slot < SIZE; slot++) inventory.setItem(slot, filler);
        inventory.setItem(HELMET, null);
        inventory.setItem(CHESTPLATE, null);
        inventory.setItem(LEGGINGS, null);
        inventory.setItem(BOOTS, null);
        inventory.setItem(OFF_HAND, null);
        inventory.setItem(MAIN_HAND, null);
    }
}
