package de.pixelrpg.rpg.region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Runtime integration for region editing, rules and enter/leave transitions. */
public final class RegionListener implements Listener {
    private final RegionManager regions;
    private final RegionEditor editor;
    private final Map<UUID, UUID> currentRegions = new HashMap<>();

    public RegionListener(RegionManager regions, RegionEditor editor) {
        this.regions = regions;
        this.editor = editor;
    }

    /** Handles admin clicks with the temporary polygon creation tool. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegionToolUse(PlayerInteractEvent event) {
        if (!editor.isTool(event.getItem()) || !editor.isEditing(event.getPlayer().getUniqueId())) return;
        if (!event.getAction().isRightClick() || event.getClickedBlock() == null) return;
        event.setCancelled(true);
        editor.addPoint(event.getPlayer(), event.getClickedBlock().getLocation());
    }

    /** Prevents PvP when either participant is inside a region that explicitly disables PvP. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!regions.hasFlag(victim.getLocation(), RegionFlag.PVP)
                || !regions.hasFlag(attacker.getLocation(), RegionFlag.PVP)) {
            event.setCancelled(true);
        }
    }

    /** Prevents monster spawning inside regions that explicitly disable monster spawns. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMonsterSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        if (!regions.hasFlag(event.getLocation(), RegionFlag.MONSTER_SPAWN)) event.setCancelled(true);
    }

    /** Prevents block breaking inside regions that explicitly disable it. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!regions.hasFlag(event.getBlock().getLocation(), RegionFlag.BLOCK_BREAK)) event.setCancelled(true);
    }

    /** Prevents block placement inside regions that explicitly disable it. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!regions.hasFlag(event.getBlock().getLocation(), RegionFlag.BLOCK_PLACE)) event.setCancelled(true);
    }

    /** Detects region enter and leave transitions for players. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) return;
        updateTransition(event.getPlayer(), event.getTo());
    }

    /** Detects a region transition when a player changes worlds. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updateTransition(event.getPlayer(), event.getPlayer().getLocation());
    }

    /** Clears transition state when a player leaves the server. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        currentRegions.remove(event.getPlayer().getUniqueId());
    }

    private void updateTransition(Player player, Location location) {
        UUID oldId = currentRegions.get(player.getUniqueId());
        UUID newId = regions.find(location).map(PixelRegion::id).orElse(null);
        if (Objects.equals(oldId, newId)) return;

        if (oldId != null) {
            regions.get(oldId).ifPresent(region -> showRegionTitle(
                    player,
                    region.name(),
                    region.leaveMessage(),
                    false
            ));
        }

        if (newId != null) {
            regions.get(newId).ifPresent(region -> showRegionTitle(
                    player,
                    region.name(),
                    region.enterMessage(),
                    true
            ));
        }

        if (newId == null) currentRegions.remove(player.getUniqueId());
        else currentRegions.put(player.getUniqueId(), newId);
    }

    private static void showRegionTitle(Player player, String regionName, String message, boolean entering) {
        String title = message == null || message.isBlank()
                ? (entering ? regionName : "Verlassen")
                : message;
        String subtitle = message == null || message.isBlank()
                ? (entering ? "" : regionName)
                : regionName;

        player.showTitle(Title.title(Component.text(title), Component.text(subtitle)));
    }

    private static boolean sameBlock(Location a, Location b) {
        return a.getWorld() == b.getWorld()
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
