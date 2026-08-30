package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemCategory;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot as BukkitEquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Validates equipment placement while leaving successful inventory movement entirely to Minecraft. */
public final class EquipmentService implements Listener {
    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final ItemService itemService;
    private final EquipmentSetService equipmentSetService;

    public EquipmentService(PlayerProfileManager profileManager, StatEngine statEngine, ItemService itemService) {
        this.profileManager = profileManager;
        this.statEngine = statEngine;
        this.itemService = itemService;
        this.equipmentSetService = new EquipmentSetService(PixelRPGPlugin.getInstance());
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

    /** Checks whether an item is allowed to occupy the supplied PixelRPG equipment slot. */
    public boolean canUseSlot(ItemStack item, EquipmentSlot slot) {
        if (item == null || item.isEmpty() || !itemService.isRPGItem(item)) return true;
        if (!item.hasItemMeta()) return false;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        String explicitSlot = pdc.get(RPGKeys.Item.equipmentSlot(), PersistentDataType.STRING);
        if (explicitSlot != null && !explicitSlot.isBlank()) {
            try {
                return EquipmentSlot.valueOf(explicitSlot.trim().toUpperCase()) == slot;
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        ItemCategory category = itemService.getCategory(item).orElse(null);
        if (category == null) return false;

        return switch (slot) {
            case HELMET -> category == ItemCategory.HELMET;
            case CHEST -> category == ItemCategory.CHESTPLATE;
            case LEGS -> category == ItemCategory.LEGGINGS;
            case FEET -> category == ItemCategory.BOOTS;
            case MAINHAND -> category == ItemCategory.MELEE_WEAPON
                    || category == ItemCategory.RANGED_WEAPON
                    || category == ItemCategory.TOOL;
            case OFFHAND -> category == ItemCategory.SHIELD;
        };
    }

    public boolean meetsRequiredLevel(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || !itemService.isRPGItem(item)) return true;
        return itemService.getRequiredLevel(item)
                .map(required -> profileManager.getProfile(player.getUniqueId())
                        .map(profile -> profile.getLevel() >= required)
                        .orElse(false))
                .orElse(true);
    }

    public void syncToProfile(Player player) {
        profileManager.getProfile(player.getUniqueId()).ifPresent(profile -> profile.setEquipment(snapshot(player)));
    }

    public void restoreFromProfile(Player player) {
        profileManager.getProfile(player.getUniqueId()).ifPresent(profile -> {
            Map<EquipmentSlot, ItemStack> stored = profile.getEquipment();
            if (stored.isEmpty()) {
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
            refresh(player);
        });
    }

    public void refresh(Player player) {
        int playerLevel = profileManager.getProfile(player.getUniqueId())
                .map(profile -> profile.getLevel())
                .orElse(1);
        equipmentSetService.applyArmorTrims(player, Math.clamp(playerLevel, 1, 99));
        statEngine.recalculate(player);
    }

    private void put(Map<EquipmentSlot, ItemStack> map, EquipmentSlot slot, ItemStack item) {
        if (item != null && !item.isEmpty()) map.put(slot, item.clone());
    }

    private ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private Optional<EquipmentSlot> slotForPlayerInventory(int slot) {
        return switch (slot) {
            case 36 -> Optional.of(EquipmentSlot.FEET);
            case 37 -> Optional.of(EquipmentSlot.LEGS);
            case 38 -> Optional.of(EquipmentSlot.CHEST);
            case 39 -> Optional.of(EquipmentSlot.HELMET);
            case 40 -> Optional.of(EquipmentSlot.OFFHAND);
            default -> Optional.empty();
        };
    }

    private boolean validate(ItemStack item, EquipmentSlot target) {
        return item == null || item.isEmpty() || canUseSlot(item, target);
    }

    /** Validates F-key mainhand/offhand swaps against their actual destination slots. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!validate(event.getMainHandItem(), EquipmentSlot.OFFHAND)
                || !validate(event.getOffHandItem(), EquipmentSlot.MAINHAND)) {
            cancelAndResync(event.getPlayer());
        }
    }

    /** Validates every inventory click that can put an item into a protected equipment slot. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            ItemStack clicked = event.getCurrentItem();
            if (!validate(player.getInventory().getItemInOffHand(), EquipmentSlot.MAINHAND)
                    || !validate(clicked, EquipmentSlot.OFFHAND)) {
                cancelAndResync(player);
            }
            return;
        }

        Optional<EquipmentSlot> target = event.getClickedInventory() instanceof PlayerInventory
                ? slotForPlayerInventory(event.getSlot())
                : Optional.empty();

        if (target.isPresent()) {
            ItemStack incoming = incomingItemForClick(event, player);
            if (!validate(incoming, target.get())) {
                cancelAndResync(player);
                return;
            }
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getClickedInventory() instanceof PlayerInventory) {
            ItemStack source = event.getCurrentItem();
            Optional<EquipmentSlot> autoEquipTarget = vanillaAutoEquipTarget(source);
            if (autoEquipTarget.isPresent() && !validate(source, autoEquipTarget.get())) {
                cancelAndResync(player);
            }
        }
    }

    /** Validates every destination slot affected by an inventory drag before Minecraft applies the drag. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            Optional<EquipmentSlot> target = equipmentSlotForRawSlot(player, entry.getKey(), event.getView());
            if (target.isPresent() && !validate(entry.getValue(), target.get())) {
                cancelAndResync(player);
                return;
            }
        }
    }

    /** Repairs any invalid PixelRPG item that nevertheless reaches an equipment slot through an unmodelled vanilla action. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        Optional<EquipmentSlot> target = slotForPlayerInventory(event.getSlot());
        if (target.isEmpty()) return;

        ItemStack newItem = event.getNewItemStack();
        if (newItem.isEmpty() || canUseSlot(newItem, target.get())) return;

        Player player = event.getPlayer();
        player.getScheduler().runDelayed(PixelRPGPlugin.getInstance(), task -> {
            ItemStack current = player.getInventory().getItem(event.getSlot());
            if (current.isEmpty() || canUseSlot(current, target.get())) return;

            ItemStack rejected = current.clone();
            player.getInventory().setItem(event.getSlot(), null);
            returnToInventory(player, rejected);
            player.updateInventory();
        }, null, 1L);
    }

    private ItemStack incomingItemForClick(InventoryClickEvent event, Player player) {
        return switch (event.getAction()) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> event.getCursor();
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                int button = event.getHotbarButton();
                yield button >= 0 ? player.getInventory().getItem(button) : player.getInventory().getItemInOffHand();
            }
            case MOVE_TO_OTHER_INVENTORY -> event.getCurrentItem();
            default -> null;
        };
    }

    private Optional<EquipmentSlot> vanillaAutoEquipTarget(ItemStack item) {
        if (item == null || item.isEmpty()) return Optional.empty();
        if (itemService.isRPGItem(item)) {
            Optional<EquipmentSlot> explicit = explicitEquipmentSlot(item);
            if (explicit.isPresent()) return explicit;
        }

        BukkitEquipmentSlot vanillaSlot = item.getType().getEquipmentSlot();
        return switch (vanillaSlot) {
            case HEAD -> Optional.of(EquipmentSlot.HELMET);
            case CHEST -> Optional.of(EquipmentSlot.CHEST);
            case LEGS -> Optional.of(EquipmentSlot.LEGS);
            case FEET -> Optional.of(EquipmentSlot.FEET);
            case HAND -> Optional.of(EquipmentSlot.MAINHAND);
            case OFF_HAND -> Optional.of(EquipmentSlot.OFFHAND);
            default -> Optional.empty();
        };
    }

    private Optional<EquipmentSlot> explicitEquipmentSlot(ItemStack item) {
        if (!item.hasItemMeta()) return Optional.empty();
        String explicit = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.equipmentSlot(), PersistentDataType.STRING);
        if (explicit == null || explicit.isBlank()) return Optional.empty();
        try {
            return Optional.of(EquipmentSlot.valueOf(explicit.trim().toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Optional<EquipmentSlot> equipmentSlotForRawSlot(Player player, int rawSlot, org.bukkit.inventory.InventoryView view) {
        if (rawSlot < 0 || rawSlot >= view.countSlots()) return Optional.empty();
        if (view.getInventory(rawSlot) != player.getInventory()) return Optional.empty();
        return slotForPlayerInventory(view.convertSlot(rawSlot));
    }

    private void cancelAndResync(Player player) {
        if (player.getOpenInventory() != null) {
            player.getScheduler().runDelayed(PixelRPGPlugin.getInstance(), task -> player.updateInventory(), null, 1L);
        }
    }

    private void cancelAndResync(InventoryClickEvent event) {
        event.setCancelled(true);
        cancelAndResync((Player) event.getWhoClicked());
    }

    private void returnToInventory(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (!leftovers.isEmpty()) {
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    /** Restores persisted equipment after the player has joined and the vanilla inventory is available. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().getScheduler().runDelayed(PixelRPGPlugin.getInstance(), task -> restoreFromProfile(event.getPlayer()), null, 1L);
    }

    /** Captures the final live equipment state before the player profile is deactivated and saved. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        syncToProfile(event.getPlayer());
    }

    /** Recalculates equipment-derived stats after vanilla respawn has rebuilt the player inventory. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        event.getPlayer().getScheduler().runDelayed(PixelRPGPlugin.getInstance(), task -> refresh(event.getPlayer()), null, 1L);
    }
}
