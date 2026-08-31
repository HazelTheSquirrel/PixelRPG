package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumMap;
import java.util.Map;

/** Tracks the live vanilla equipment slots for stat calculation and installs equipment requirement guards. */
public final class EquipmentService implements Listener {
    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;

    public EquipmentService(PlayerProfileManager profileManager, StatEngine statEngine, ItemService ignoredItemService) {
        this.profileManager = profileManager;
        this.statEngine = statEngine;
        PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
        if (plugin != null) {
            plugin.getServer().getPluginManager().registerEvents(new ItemLevelRequirementListener(profileManager), plugin);
            plugin.getServer().getPluginManager().registerEvents(new ItemUsageRequirementListener(profileManager), plugin);
        }
    }

    /** Creates a defensive snapshot of the six live Minecraft equipment slots. */
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

    /** Copies the final live equipment state into the profile for persistence when the player leaves. */
    public void syncToProfile(Player player) {
        profileManager.getProfile(player.getUniqueId())
                .ifPresent(profile -> profile.setEquipment(snapshot(player)));
    }

    /** Restores the persisted equipment snapshot into the real Minecraft inventory after login. */
    public void restoreFromProfile(Player player) {
        profileManager.getProfile(player.getUniqueId()).ifPresent(profile -> {
            Map<EquipmentSlot, ItemStack> stored = profile.getEquipment();
            if (stored.isEmpty()) {
                refresh(player);
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

    /** Recalculates all equipment-derived stats from the player's current vanilla equipment slots. */
    public void refresh(Player player) {
        statEngine.recalculate(player);
    }

    private void put(Map<EquipmentSlot, ItemStack> map, EquipmentSlot slot, ItemStack item) {
        if (item != null && !item.isEmpty()) map.put(slot, item.clone());
    }

    private ItemStack copy(ItemStack item) { return item == null ? null : item.clone(); }

    // Zuständig für die Neuberechnung von Stats nach einer Änderung eines Rüstungs- oder aktiven Hand-Slots.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        Player player = event.getPlayer();
        int slot = event.getSlot();
        boolean equipmentSlotChanged = slot >= 36 && slot <= 40;
        boolean selectedMainHandChanged = slot >= 0 && slot <= 8 && player.getInventory().getHeldItemSlot() == slot;
        if (equipmentSlotChanged || selectedMainHandChanged) refreshNextTick(player);
    }

    // Zuständig für die Neuberechnung der Stats beim Wechsel des aktiven Hotbar-Slots.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        refreshNextTick(event.getPlayer());
    }

    // Zuständig für die Neuberechnung der Stats nach dem F-Tasten-Tausch zwischen Haupt- und Nebenhand.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        refreshNextTick(event.getPlayer());
    }

    // Zuständig für die Wiederherstellung des gespeicherten Equipments beim Login.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().getScheduler().runDelayed(
                PixelRPGPlugin.getInstance(),
                task -> restoreFromProfile(event.getPlayer()),
                null,
                1L
        );
    }

    // Zuständig für das Persistieren des aktuellen Equipments beim Logout.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        syncToProfile(event.getPlayer());
    }

    // Zuständig für die Neuberechnung der Stats nach einem Respawn.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        refreshNextTick(event.getPlayer());
    }

    private void refreshNextTick(Player player) {
        player.getScheduler().runDelayed(
                PixelRPGPlugin.getInstance(),
                task -> refresh(player),
                null,
                1L
        );
    }
}
