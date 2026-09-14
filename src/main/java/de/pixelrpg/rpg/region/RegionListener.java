package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.LightningStrikeEvent;

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

    /** Handles admin clicks with the temporary living-entity spawn arrow. */
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
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker
                && !policy.allowsPvp(attacker, victim)) event.setCancelled(true);
    }

    /** Applies the region mob-damage policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobDamage(EntityDamageEvent event) {
        if (!policy.allowsMobDamage(event)) event.setCancelled(true);
    }

    /** Applies the region animal-damage policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnimalDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Animals && !policy.allowsAnimalDamage(event)) event.setCancelled(true);
    }

    /** Applies the region fall-damage policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!policy.allowsFallDamage(event)) event.setCancelled(true);
    }

    /** Applies the region living-entity spawn policies. */
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

    /** Applies the region entity-interaction policy using the current entity-interaction event. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (!policy.allowsInteract(event.getRightClicked().getLocation())) event.setCancelled(true);
    }

    /** Applies the region block/item-use policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockUse(PlayerInteractEvent event) {
        if (editor.isTool(event.getItem()) || editor.isSpawnTool(event.getItem())) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!policy.allowsUse(block.getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (Tag.ANVIL.isTagged(block.getType()) && !policy.allowsAnvil(block.getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (block.getType() == Material.RESPAWN_ANCHOR && !policy.allowsRespawnAnchor(block.getLocation())) {
            event.setCancelled(true);
        }
    }

    /** Applies the region container-access policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof org.bukkit.inventory.BlockInventoryHolder)) return;
        Location location = event.getInventory().getLocation();
        if (location != null && !policy.allowsContainerAccess(location)) event.setCancelled(true);
    }

    /** Applies the region item-drop policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!policy.allowsItemDrop(event.getPlayer().getLocation())) event.setCancelled(true);
    }

    /** Applies the region item-pickup policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && !policy.allowsItemPickup(event.getItem().getLocation())) {
            event.setCancelled(true);
        }
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

    /** Applies the region water-flow policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWaterFlow(BlockFromToEvent event) {
        if (!policy.allowsWaterFlow(event)) event.setCancelled(true);
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

    /** Applies the region TNT priming policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTntPrime(TNTPrimeEvent event) {
        if (!policy.allowsTnt(event.getBlock().getLocation())) event.setCancelled(true);
    }

    /** Applies the region Enderman-grief policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEndermanGrief(EntityChangeBlockEvent event) {
        if (!policy.allowsEndermanGrief(event)) event.setCancelled(true);
    }

    /** Applies the region lightning policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLightning(LightningStrikeEvent event) {
        if (!policy.allowsLightning(event.getLightning().getLocation())) event.setCancelled(true);
    }

    /** Applies the region crop-growth policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCropGrowth(BlockGrowEvent event) {
        if (!policy.allowsCropGrowth(event)) event.setCancelled(true);
    }

    /** Applies the region leaf-decay policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!policy.allowsLeafDecay(event.getBlock().getLocation())) event.setCancelled(true);
    }

    /** Applies the region farmland and block-trampling policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockTrampling(EntityInteractEvent event) {
        if (!policy.allowsTrampling(event)) event.setCancelled(true);
    }

    /** Applies the region sleep policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSleep(PlayerBedEnterEvent event) {
        if (!policy.allowsSleep(event.getBed().getLocation())) event.setCancelled(true);
    }

    /** Applies the region chorus-fruit teleport policy without relying on the deprecated teleport cause. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChorusFruitConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.CHORUS_FRUIT
                && !policy.allowsChorusFruit(event.getPlayer().getLocation())) event.setCancelled(true);
    }

    /** Applies the region ender-pearl teleport policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && event.getTo() != null
                && !policy.allowsEnderPearl(event.getTo())) event.setCancelled(true);
    }

    /** Applies the region natural-health-regen policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNaturalRegen(EntityRegainHealthEvent event) {
        if (!policy.allowsNaturalRegen(event)) event.setCancelled(true);
    }

    /** Applies the region natural-hunger-drain policy. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNaturalHunger(FoodLevelChangeEvent event) {
        if (!policy.allowsNaturalHunger(event)) event.setCancelled(true);
    }

    /** Enforces region entry and exit permissions before a player crosses a region boundary. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegionBoundary(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) return;
        PixelRegion from = regions.find(event.getFrom()).orElse(null);
        PixelRegion to = regions.find(event.getTo()).orElse(null);
        if (from != null && to != null && !from.id().equals(to.id()) && !policy.allowsExit(event.getFrom())) {
            event.setCancelled(true);
            return;
        }
        if (from != null && to != null && !from.id().equals(to.id()) && !policy.allowsEntry(event.getTo())) {
            event.setCancelled(true);
        }
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
