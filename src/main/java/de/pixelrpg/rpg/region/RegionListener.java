package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

/** Thin Paper adapter for region editor input, policy decisions and transitions. */
public final class RegionListener implements Listener {
    private final RegionManager regions;
    private final RegionEditor editor;
    private final RegionPolicyService policy;
    private final RegionTransitionService transitions;

    public RegionListener(RegionManager regions, RegionEditor editor, RegionSpawnService spawnService) {
        this.regions = Objects.requireNonNull(regions);
        this.editor = Objects.requireNonNull(editor);
        this.policy = new RegionPolicyService(regions, spawnService);
        this.transitions = new RegionTransitionService(regions);
    }

    /** Handles admin clicks with the temporary polygon creation tool. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegionToolUse(PlayerInteractEvent event) {
        if (!editor.isTool(event.getItem()) || !editor.isEditing(event.getPlayer().getUniqueId())) return;
        if (!event.getAction().isRightClick() || event.getClickedBlock() == null) return;
        event.setCancelled(true);
        editor.addPoint(event.getPlayer(), event.getClickedBlock().getLocation());
    }

    /** Handles admin clicks with the temporary hostile-mob spawn arrow. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnToolUse(PlayerInteractEvent event) {
        if (!editor.isSpawnTool(event.getItem()) || !editor.isEditing(event.getPlayer().getUniqueId())) return;
        if (!event.getAction().isRightClick() || event.getClickedBlock() == null) return;
        event.setCancelled(true);
        editor.addSpawnPoint(event.getPlayer(), event.getClickedBlock().getLocation());
    }

    /** Applies the region PvP policy to player-versus-player damage. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            if (!policy.allowsPvp(attacker, victim)) event.setCancelled(true);
        }
    }

    /** Applies the region monster-spawn policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMonsterSpawn(CreatureSpawnEvent event) {
        if (!policy.allowsMonsterSpawn(event)) event.setCancelled(true);
    }

    /** Applies the region block-break policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!policy.allowsBlockBreak(event.getBlock().getLocation())) event.setCancelled(true);
    }

    /** Applies the region block-place policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!policy.allowsBlockPlace(event.getBlock().getLocation())) event.setCancelled(true);
    }

    /** Applies the region fire-spread policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent event) {
        if (!policy.allowsFireSpread(event)) event.setCancelled(true);
    }

    /** Applies the region lava-flow policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLavaFlow(BlockFromToEvent event) {
        if (!policy.allowsLavaFlow(event)) event.setCancelled(true);
    }

    /** Applies the region entity-explosion policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (!policy.allowsEntityExplosion(event)) event.setCancelled(true);
    }

    /** Applies the region block-explosion policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (!policy.allowsBlockExplosion(event)) event.setCancelled(true);
    }

    /** Applies the region Enderman-grief policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEndermanGrief(EntityChangeBlockEvent event) {
        if (!policy.allowsEndermanGrief(event)) event.setCancelled(true);
    }

    /** Detects region enter and leave transitions for players. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) return;
        transitions.update(event.getPlayer(), event.getTo());
    }

    /** Detects a region transition when a player changes worlds. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        transitions.update(event.getPlayer(), event.getPlayer().getLocation());
    }

    /** Clears transition state when a player leaves the server. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        transitions.clear(event.getPlayer().getUniqueId());
    }

    private static boolean sameBlock(Location a, Location b) {
        return a.getWorld() == b.getWorld()
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
