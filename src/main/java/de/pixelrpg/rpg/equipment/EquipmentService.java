package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemCategory;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Owns the six PixelRPG equipment slots while using the normal Minecraft inventory. */
public final class EquipmentService implements Listener {
    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final ItemService itemService;

    public EquipmentService(PlayerProfileManager profileManager, StatEngine statEngine, ItemService itemService) {
        this.profileManager = profileManager;
        this.statEngine = statEngine;
        this.itemService = itemService;
    }

    public Map<EquipmentSlot, ItemStack> snapshot(Player player) {
        Map<EquipmentSlot, ItemStack> result = new EnumMap<>(EquipmentSlot.class);
        PlayerInventory inventory = player.getInventory();
        put(result, EquipmentSlot.HELMET, inventory.getHelmet());
        put(result, EquipmentSlot.CHEST, inventory.getChestplate());
        put(result, EquipmentSlot.LEGS, inventory.getLeggings());
        put(result, EquipmentSlot.FEET, inventory.getBoots());
        put(result, EquipmentSlot.MAINHAND, inventory.getItemInMainHand());
        put(result, EquipmentSlot.OFFHAND, inventory.getItemInOffHand());
        return result;
    }

    public boolean canUseSlot(ItemStack item, EquipmentSlot slot) {
        if (item == null || item.isEmpty() || !itemService.isRPGItem(item)) return true;
        ItemCategory category = itemService.getCategory(item).orElse(null);
        if (category == null) return true;
        return switch (slot) {
            case HELMET -> category == ItemCategory.HELMET;
            case CHEST -> category == ItemCategory.CHESTPLATE;
            case LEGS -> category == ItemCategory.LEGGINGS;
            case FEET -> category == ItemCategory.BOOTS;
            case MAINHAND -> category == ItemCategory.MELEE_WEAPON || category == ItemCategory.RANGED_WEAPON || category == ItemCategory.TOOL;
            case OFFHAND -> category == ItemCategory.SHIELD;
        };
    }

    public boolean meetsRequiredLevel(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || !itemService.isRPGItem(item)) return true;
        return itemService.getRequiredLevel(item).map(required -> profileManager.getProfile(player.getUniqueId())
                .map(profile -> profile.getLevel() >= required).orElse(false)).orElse(true);
    }

    public void syncToProfile(Player player) {
        profileManager.getProfile(player.getUniqueId()).ifPresent(profile -> {
            profile.setEquipment(snapshot(player));
            profile.markDirty();
        });
    }

    public void restoreFromProfile(Player player) {
        profileManager.getProfile(player.getUniqueId()).ifPresent(profile -> {
            Map<EquipmentSlot, ItemStack> stored = profile.getEquipment();
            if (stored.isEmpty()) {
                syncToProfile(player);
                statEngine.recalculate(player);
                return;
            }
            PlayerInventory inventory = player.getInventory();
            inventory.setHelmet(copy(stored.get(EquipmentSlot.HELMET)));
            inventory.setChestplate(copy(stored.get(EquipmentSlot.CHEST)));
            inventory.setLeggings(copy(stored.get(EquipmentSlot.LEGS)));
            inventory.setBoots(copy(stored.get(EquipmentSlot.FEET)));
            inventory.setItemInMainHand(copy(stored.get(EquipmentSlot.MAINHAND)));
            inventory.setItemInOffHand(copy(stored.get(EquipmentSlot.OFFHAND)));
            statEngine.recalculate(player);
        });
    }

    public void refresh(Player player) {
        statEngine.recalculate(player);
        syncToProfile(player);
    }

    private void put(Map<EquipmentSlot, ItemStack> map, EquipmentSlot slot, ItemStack item) {
        if (item != null && !item.isEmpty()) map.put(slot, item.clone());
    }

    private ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private Optional<EquipmentSlot> slotForInventory(PlayerInventory inventory, int rawSlot) {
        if (rawSlot == 5) return Optional.of(EquipmentSlot.HELMET);
        if (rawSlot == 6) return Optional.of(EquipmentSlot.CHEST);
        if (rawSlot == 7) return Optional.of(EquipmentSlot.LEGS);
        if (rawSlot == 8) return Optional.of(EquipmentSlot.FEET);
        if (rawSlot == 45) return Optional.of(EquipmentSlot.OFFHAND);
        return Optional.empty();
    }

    /** Revalidates PixelRPG items placed into armor/offhand slots and refreshes stats after inventory changes. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getClickedInventory() instanceof PlayerInventory inventory)) return;
        slotForInventory(inventory, event.getSlot()).ifPresent(slot -> {
            ItemStack candidate = event.getCursor();
            if (candidate == null || candidate.isEmpty()) candidate = event.getCurrentItem();
            if (!canUseSlot(candidate, slot)) event.setCancelled(true);
        });
        player.getScheduler().runDelayed(de.pixelrpg.rpg.PixelRPGPlugin.getInstance(), task -> refresh(player), null, 1L);
    }

    /** Revalidates dragged PixelRPG equipment and refreshes the stat cache after a completed drag. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 0 || rawSlot >= player.getInventory().getSize()) continue;
            EquipmentSlot slot = switch (rawSlot) {
                case 5 -> EquipmentSlot.HELMET;
                case 6 -> EquipmentSlot.CHEST;
                case 7 -> EquipmentSlot.LEGS;
                case 8 -> EquipmentSlot.FEET;
                case 45 -> EquipmentSlot.OFFHAND;
                default -> null;
            };
            if (slot != null && !canUseSlot(event.getOldCursor(), slot)) {
                event.setCancelled(true);
                return;
            }
        }
        player.getScheduler().runDelayed(de.pixelrpg.rpg.PixelRPGPlugin.getInstance(), task -> refresh(player), null, 1L);
    }

    /** Restores persisted equipment after the player has joined and the vanilla inventory is available. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().getScheduler().runDelayed(de.pixelrpg.rpg.PixelRPGPlugin.getInstance(), task -> restoreFromProfile(event.getPlayer()), null, 1L);
    }

    /** Captures the final equipment state before the profile is deactivated and saved. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        syncToProfile(event.getPlayer());
    }

    /** Reapplies equipment-derived stats after vanilla respawn has rebuilt the inventory. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        event.getPlayer().getScheduler().runDelayed(de.pixelrpg.rpg.PixelRPGPlugin.getInstance(), task -> refresh(event.getPlayer()), null, 1L);
    }
}
