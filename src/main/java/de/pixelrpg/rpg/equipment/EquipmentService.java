package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
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

public final class EquipmentService implements Listener {
    private final JavaPlugin plugin;
    private final PlayerProfileManager profiles;
    private final StatEngine stats;

    public EquipmentService(JavaPlugin plugin, PlayerProfileManager profiles, StatEngine stats) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.stats = stats;
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

    public void refresh(Player player) {
        stats.recalculate(player);
    }

    public void syncToProfile(Player player) {
        profiles.getProfile(player.getUniqueId()).ifPresent(profile -> profile.setEquipment(snapshot(player)));
    }

    public void restoreFromProfile(Player player) {
        profiles.getProfile(player.getUniqueId()).ifPresent(profile -> {
            Map<EquipmentSlot, ItemStack> stored = profile.getEquipment();
            if (!stored.isEmpty()) {
                PlayerInventory inventory = player.getInventory();
                inventory.setHelmet(copy(stored.get(EquipmentSlot.HELMET)));
                inventory.setChestplate(copy(stored.get(EquipmentSlot.CHEST)));
                inventory.setLeggings(copy(stored.get(EquipmentSlot.LEGS)));
                inventory.setBoots(copy(stored.get(EquipmentSlot.FEET)));
                inventory.setItemInMainHand(copy(stored.get(EquipmentSlot.MAINHAND)));
                inventory.setItemInOffHand(copy(stored.get(EquipmentSlot.OFFHAND)));
            }
            refresh(player);
        });
    }

    /** Recalculates character stats after a relevant inventory slot changes. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        Player player = event.getPlayer();
        int slot = event.getSlot();
        if ((slot >= 36 && slot <= 40)
                || (slot >= 0 && slot <= 8 && player.getInventory().getHeldItemSlot() == slot)) {
            player.getScheduler().runDelayed(plugin, task -> refresh(player), null, 1L);
        }
    }

    /** Recalculates character stats after the active hotbar slot changes. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        event.getPlayer().getScheduler().runDelayed(plugin,
                task -> refresh(event.getPlayer()), null, 1L);
    }

    /** Recalculates character stats after a main-hand/off-hand swap. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        event.getPlayer().getScheduler().runDelayed(plugin,
                task -> refresh(event.getPlayer()), null, 1L);
    }

    /** Recalculates and restores the persisted equipment state after login. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().getScheduler().runDelayed(plugin,
                task -> restoreFromProfile(event.getPlayer()), null, 1L);
    }

    /** Captures the live equipment state before the profile is persisted on logout. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        syncToProfile(event.getPlayer());
        profiles.saveProfileAsync(event.getPlayer().getUniqueId());
    }

    /** Recalculates character stats after respawn. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        event.getPlayer().getScheduler().runDelayed(plugin,
                task -> refresh(event.getPlayer()), null, 1L);
    }

    private static ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private static void put(Map<EquipmentSlot, ItemStack> result, EquipmentSlot slot, ItemStack item) {
        if (item != null && !item.isEmpty()) result.put(slot, item.clone());
    }
}
