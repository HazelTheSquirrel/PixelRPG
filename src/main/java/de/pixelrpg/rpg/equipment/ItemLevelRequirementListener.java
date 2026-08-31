package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Prevents under-level PixelRPG gear from being equipped or actively used. */
public final class ItemLevelRequirementListener implements Listener {
    private final PlayerProfileManager profileManager;

    public ItemLevelRequirementListener(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    // Zuständig dafür, dass unterleveltes PixelRPG-Equipment nicht per Inventarklick oder Shift-Klick angelegt wird.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack candidate = event.getCursor();
        if (isEquipmentSlot(event) && isBelowRequiredLevel(player, candidate)) {
            event.setCancelled(true);
            notifyRequiredLevel(player, candidate);
            return;
        }
        if (event.isShiftClick() && isEquipable(candidateFromClickedInventory(event)) && isBelowRequiredLevel(player, candidateFromClickedInventory(event))) {
            event.setCancelled(true);
            notifyRequiredLevel(player, candidateFromClickedInventory(event));
        }
    }

    // Zuständig dafür, dass unterleveltes Equipment nicht per Inventar-Drag in einen Rüstungs- oder Offhand-Slot gelangt.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack dragged = event.getOldCursor();
        if (!isBelowRequiredLevel(player, dragged)) return;
        if (event.getRawSlots().stream().anyMatch(this::isEquipmentRawSlot)) {
            event.setCancelled(true);
            notifyRequiredLevel(player, dragged);
        }
    }

    // Zuständig dafür, dass ein Spieler kein unterleveltes PixelRPG-Werkzeug oder Schwert in die aktive Haupthand wechselt.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        ItemStack next = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (!isBelowRequiredLevel(event.getPlayer(), next)) return;
        event.setCancelled(true);
        notifyRequiredLevel(event.getPlayer(), next);
    }

    // Zuständig dafür, dass unterleveltes Equipment nicht per F-Taste zwischen Haupt- und Nebenhand gewechselt wird.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (isBelowRequiredLevel(player, event.getMainHandItem()) || isBelowRequiredLevel(player, event.getOffHandItem())) {
            event.setCancelled(true);
            ItemStack blocked = isBelowRequiredLevel(player, event.getMainHandItem()) ? event.getMainHandItem() : event.getOffHandItem();
            notifyRequiredLevel(player, blocked);
        }
    }

    private boolean isEquipmentSlot(InventoryClickEvent event) {
        return event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR
                || event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.QUICKBAR && event.getRawSlot() == 45;
    }

    private boolean isEquipmentRawSlot(int rawSlot) {
        return rawSlot == 5 || rawSlot == 6 || rawSlot == 7 || rawSlot == 8 || rawSlot == 45;
    }

    private ItemStack candidateFromClickedInventory(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        return current == null ? new ItemStack(Material.AIR) : current;
    }

    private boolean isBelowRequiredLevel(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return false;
        Integer required = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (required == null || required <= 0) return false;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        return profile != null && profile.isRegistered() && profile.getLevel() < required;
    }

    private boolean isEquipable(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return false;
        String slot = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.equipmentSlot(), PersistentDataType.STRING);
        if (slot != null && !slot.isBlank()) return true;
        Material material = item.getType();
        return material.name().endsWith("_HELMET") || material.name().endsWith("_CHESTPLATE")
                || material.name().endsWith("_LEGGINGS") || material.name().endsWith("_BOOTS")
                || material.name().endsWith("_SWORD") || material.name().endsWith("_AXE")
                || material == Material.BOW || material == Material.CROSSBOW || material == Material.SHIELD;
    }

    private void notifyRequiredLevel(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;
        Integer required = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (required != null) player.sendActionBar(Component.text("Benötigt Level " + required, NamedTextColor.RED));
    }
}
