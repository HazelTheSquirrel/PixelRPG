package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.GearCategoryRegistry;
import de.pixelrpg.rpg.item.ItemCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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

    // Zuständig für sichere Klicks, Slot-Validierung und sofortiges Speichern im Companion-Equipment-Inventar.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof CompanionEquipmentHolder)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < SIZE) {
            if (!isEquipmentSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }

            ItemStack incoming = incomingItem(event, player);
            if (incoming != null && !incoming.getType().isAir() && !accepts(rawSlot, incoming)) {
                event.setCancelled(true);
                return;
            }

            schedulePersist(event.getView().getTopInventory());
            return;
        }

        if (event.isShiftClick()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) {
                event.setCancelled(true);
                return;
            }

            int targetSlot = findEmptyCompatibleEquipmentSlot(event.getView().getTopInventory(), clicked);
            if (targetSlot < 0) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            ItemStack equipped = clicked.clone();
            equipped.setAmount(1);
            event.getView().getTopInventory().setItem(targetSlot, equipped);
            if (clicked.getAmount() <= 1) event.setCurrentItem(null);
            else event.setCurrentItem(clicked.asQuantity(clicked.getAmount() - 1));
            schedulePersist(event.getView().getTopInventory());
        }
    }

    // Zuständig dafür, dass Drag-Aktionen nur passende Gegenstände in die jeweiligen Equipment-Slots bringen.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof CompanionEquipmentHolder)) return;

        for (var entry : event.getNewItems().entrySet()) {
            int slot = entry.getKey();
            if (slot < SIZE && (!isEquipmentSlot(slot) || !accepts(slot, entry.getValue()))) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getNewItems().keySet().stream().anyMatch(slot -> slot < SIZE && isEquipmentSlot(slot))) {
            schedulePersist(event.getView().getTopInventory());
        }
    }

    // Zuständig für das persistente Speichern der Companion-Ausrüstung beim Schließen.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof CompanionEquipmentHolder holder)) return;
        persistAndRefresh(event.getInventory(), holder);
    }

    private void schedulePersist(Inventory inventory) {
        if (inventory.getHolder() instanceof CompanionEquipmentHolder holder) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (inventory.getViewers().isEmpty()) return;
                persistAndRefresh(inventory, holder);
            });
        }
    }

    private void persistAndRefresh(Inventory inventory, CompanionEquipmentHolder holder) {
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
            if (entity instanceof LivingEntity living) {
                companionService.applyEquipmentToEntity(holder.playerId(), holder.companionId(), living);
            }
        });
    }

    private static ItemStack incomingItem(InventoryClickEvent event, Player player) {
        InventoryAction action = event.getAction();
        return switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> event.getCursor();
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                int hotbarButton = event.getHotbarButton();
                yield hotbarButton < 0 ? null : player.getInventory().getItem(hotbarButton);
            }
            default -> null;
        };
    }

    private static int findEquipmentSlot(ItemStack item) {
        if (item == null || item.getType().isAir()) return -1;
        for (int slot : new int[]{HELMET, CHESTPLATE, LEGGINGS, BOOTS, OFF_HAND, MAIN_HAND}) {
            if (accepts(slot, item)) return slot;
        }
        return -1;
    }

    private static int findEmptyCompatibleEquipmentSlot(Inventory inventory, ItemStack item) {
        for (int slot : new int[]{HELMET, CHESTPLATE, LEGGINGS, BOOTS, OFF_HAND, MAIN_HAND}) {
            if (inventory.getItem(slot) == null && accepts(slot, item)) return slot;
        }
        return -1;
    }

    private static boolean accepts(int slot, ItemStack item) {
        ItemCategory category = resolveCategory(item);
        if (category == null) return false;

        return switch (slot) {
            case HELMET -> category == ItemCategory.HELMET;
            case CHESTPLATE -> category == ItemCategory.CHESTPLATE;
            case LEGGINGS -> category == ItemCategory.LEGGINGS;
            case BOOTS -> category == ItemCategory.BOOTS;
            case OFF_HAND -> category == ItemCategory.SHIELD;
            case MAIN_HAND -> category == ItemCategory.MELEE_WEAPON
                    || category == ItemCategory.RANGED_WEAPON
                    || category == ItemCategory.TOOL;
            default -> false;
        };
    }

    private static ItemCategory resolveCategory(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String configured = meta.getPersistentDataContainer().get(RPGKeys.Item.category(), PersistentDataType.STRING);
            if (configured != null) {
                try {
                    return ItemCategory.valueOf(configured.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // Fall through to the material registry for malformed/unknown category data.
                }
            }
        }

        return GearCategoryRegistry.resolve(item.getType()).orElse(null);
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
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int slot = 0; slot < SIZE; slot++) inventory.setItem(slot, filler);

        setLabel(inventory, 1, "Helm");
        setLabel(inventory, 3, "Brustplatte");
        setLabel(inventory, 5, "Hose");
        setLabel(inventory, 7, "Schuhe");
        setLabel(inventory, 19, "Off Hand");
        setLabel(inventory, 21, "Main Hand");

        inventory.setItem(HELMET, null);
        inventory.setItem(CHESTPLATE, null);
        inventory.setItem(LEGGINGS, null);
        inventory.setItem(BOOTS, null);
        inventory.setItem(OFF_HAND, null);
        inventory.setItem(MAIN_HAND, null);
    }

    private static void setLabel(Inventory inventory, int slot, String label) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
}
