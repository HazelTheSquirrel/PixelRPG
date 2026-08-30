package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemCategory;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Validates PixelRPG equipment placement while leaving all actual inventory movement to Minecraft.
 * The live PlayerInventory remains the source of truth for the six stat-bearing equipment slots.
 */
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

    /**
     * Checks whether an item is allowed to occupy the supplied PixelRPG equipment slot.
     * Vanilla items are deliberately accepted here so Minecraft remains responsible for all
     * native equip rules; PixelRPG only adds restrictions to identified PixelRPG items.
     */
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
            refresh(player);
        });
    }

    /** Recalculates equipment-derived state after Minecraft has completed an allowed equipment transaction. */
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

    private Optional<EquipmentSlot> pixelRpgSlot(org.bukkit.inventory.EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> Optional.of(EquipmentSlot.HELMET);
            case CHEST -> Optional.of(EquipmentSlot.CHEST);
            case LEGS -> Optional.of(EquipmentSlot.LEGS);
            case FEET -> Optional.of(EquipmentSlot.FEET);
            case HAND -> Optional.of(EquipmentSlot.MAINHAND);
            case OFF_HAND -> Optional.of(EquipmentSlot.OFFHAND);
            default -> Optional.empty();
        };
    }

    private boolean validate(ItemStack item, EquipmentSlot target) {
        return item == null || item.isEmpty() || canUseSlot(item, target);
    }

    private boolean validateHandSwap(PlayerSwapWithEquipmentSlotEvent event) {
        EquipmentSlot target = pixelRpgSlot(event.getSlot()).orElse(null);
        if (target == null) return true;

        if (!validate(event.getItemInHand(), target)) return false;

        // The item currently in the equipment slot is moved back into the hand.
        // Determine the actual hand from the live inventory because the Paper event intentionally
        // exposes only that the source is one of the two hand slots.
        PlayerInventory inventory = event.getPlayer().getInventory();
        if (inventory.getItemInMainHand().isSimilar(event.getItemInHand())) {
            return validate(event.getItemToSwap(), EquipmentSlot.MAINHAND);
        }
        return validate(event.getItemToSwap(), EquipmentSlot.OFFHAND);
    }

    /** Validates every Paper equipment-slot swap before Minecraft applies the swap. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapWithEquipmentSlot(PlayerSwapWithEquipmentSlotEvent event) {
        if (!validateHandSwap(event)) event.setCancelled(true);
    }

    /** Validates the native F-key mainhand/offhand transaction without manually changing either slot. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!validate(event.getMainHandItem(), EquipmentSlot.MAINHAND)
                || !validate(event.getOffHandItem(), EquipmentSlot.OFFHAND)) {
            event.setCancelled(true);
        }
    }

    /** Validates inventory clicks that can place, swap, or hotbar-swap an item into an equipment slot. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        EquipmentSlot clickedEquipmentSlot = null;
        if (event.getClickedInventory() instanceof PlayerInventory) {
            clickedEquipmentSlot = slotForPlayerInventory(event.getSlot()).orElse(null);
        }

        if (event.getClick().isKeyboardClick() && event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
            ItemStack clicked = event.getCurrentItem();
            if (clickedEquipmentSlot != null) {
                if (!validate(player.getInventory().getItemInOffHand(), clickedEquipmentSlot)
                        || !validate(clicked, EquipmentSlot.OFFHAND)) {
                    event.setCancelled(true);
                    return;
                }
            } else if (!validate(clicked, EquipmentSlot.OFFHAND)) {
                event.setCancelled(true);
                return;
            }
            return;
        }

        if (clickedEquipmentSlot != null) {
            ItemStack incoming = switch (event.getAction()) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> event.getCursor();
                case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                    int button = event.getHotbarButton();
                    yield button >= 0 ? player.getInventory().getItem(button) : player.getInventory().getItemInOffHand();
                }
                default -> null;
            };

            if (incoming != null && !incoming.isEmpty() && !validate(incoming, clickedEquipmentSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getClickedInventory() != player.getInventory()) {
            ItemStack source = event.getCurrentItem();
            if (source != null && !source.isEmpty() && itemService.isRPGItem(source)) {
                Optional<EquipmentSlot> vanillaTarget = pixelRpgSlot(source.getType().getEquipmentSlot());
                Optional<EquipmentSlot> pixelTarget = equipmentSlotFor(source);
                if (vanillaTarget.isPresent() && pixelTarget.isPresent() && vanillaTarget.get() != pixelTarget.get()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /** Validates every raw slot affected by a drag before any of the drag changes are applied. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            Optional<EquipmentSlot> target = equipmentSlotForRawSlot(player, entry.getKey(), event.getView());
            if (target.isPresent() && !validate(entry.getValue(), target.get())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private Optional<EquipmentSlot> equipmentSlotForRawSlot(Player player, int rawSlot, org.bukkit.inventory.InventoryView view) {
        if (view.getInventory(rawSlot) != player.getInventory()) return Optional.empty();
        return slotForPlayerInventory(view.convertSlot(rawSlot));
    }

    private Optional<EquipmentSlot> equipmentSlotFor(ItemStack item) {
        if (item == null || item.isEmpty() || !itemService.isRPGItem(item)) return Optional.empty();
        if (!item.hasItemMeta()) return Optional.empty();
        String explicit = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.equipmentSlot(), PersistentDataType.STRING);
        if (explicit == null || explicit.isBlank()) return Optional.empty();
        try {
            return Optional.of(EquipmentSlot.valueOf(explicit.trim().toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    /** Recalculates stats after a non-cancelled inventory click has completed on the next tick. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClickRefresh(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!equipmentTransactionMayAffectEquipment(event)) return;
        scheduleRefresh(player);
    }

    /** Recalculates stats after a non-cancelled inventory drag has completed on the next tick. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDragRefresh(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getNewItems().keySet().stream().noneMatch(raw -> equipmentSlotForRawSlot(player, raw, event.getView()).isPresent())) return;
        scheduleRefresh(player);
    }

    /** Recalculates stats after a successful mainhand/offhand swap has completed. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItemsRefresh(PlayerSwapHandItemsEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    /** Recalculates stats after a successful equipment-slot swap has completed. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapWithEquipmentSlotRefresh(PlayerSwapWithEquipmentSlotEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    private boolean equipmentTransactionMayAffectEquipment(InventoryClickEvent event) {
        if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) return true;
        if (event.getClickedInventory() instanceof PlayerInventory
                && slotForPlayerInventory(event.getSlot()).isPresent()) return true;
        return event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getClickedInventory() != null
                && !(event.getClickedInventory() instanceof PlayerInventory)
                && event.getCurrentItem() != null
                && !event.getCurrentItem().isEmpty()
                && equipmentSlotFor(event.getCurrentItem()).isPresent();
    }

    private void scheduleRefresh(Player player) {
        player.getScheduler().runDelayed(PixelRPGPlugin.getInstance(), task -> refresh(player), null, 1L);
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
