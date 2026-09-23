package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.LightningStrikeEvent;
import java.util.Objects;

/** Thin Paper event adapter delegating region decisions to policy and transition services. */
public final class RegionListener implements Listener {
    private final RegionManager regions; private final RegionPolicyService policy; private final RegionTransitionService transitions;
    public RegionListener(RegionManager regions,RegionPolicyService policy,RegionTransitionService transitions){this.regions=Objects.requireNonNull(regions);this.policy=Objects.requireNonNull(policy);this.transitions=Objects.requireNonNull(transitions);}
    // Applies PvP region policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onPvp(EntityDamageByEntityEvent e){if(e.getEntity() instanceof Player v&&e.getDamager() instanceof Player a&&!policy.allowsPvp(a,v))e.setCancelled(true);}
    // Applies monster damage policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onMobDamage(EntityDamageEvent e){if(!policy.allowsMobDamage(e))e.setCancelled(true);}
    // Applies animal damage policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onAnimalDamage(EntityDamageByEntityEvent e){if(e.getEntity() instanceof Animals&&!policy.allowsAnimalDamage(e))e.setCancelled(true);}
    // Applies fall damage policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onFall(EntityDamageEvent e){if(!policy.allowsFallDamage(e))e.setCancelled(true);}
    // Applies natural mob spawning policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onSpawn(CreatureSpawnEvent e){if(!policy.allowsMonsterSpawn(e))e.setCancelled(true);}
    // Applies block-break policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBreak(BlockBreakEvent e){if(!policy.allowsBlockBreak(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    // Applies block-place policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onPlace(BlockPlaceEvent e){if(!policy.allowsBlockPlace(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    // Applies entity-interaction policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onEntityInteract(PlayerInteractAtEntityEvent e){if(!policy.allowsEntityInteraction(e.getPlayer(),e.getRightClicked()))e.setCancelled(true);}
    // Applies block-use policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBlockUse(PlayerInteractEvent e){Block b=e.getClickedBlock();if(b!=null&&!policy.allowsUse(e.getPlayer(),b))e.setCancelled(true);if(b!=null&&b.getType()==Material.RESPAWN_ANCHOR&&!policy.allows(e.getPlayer(),b.getLocation(),RegionFlag.RESPAWN_ANCHORS))e.setCancelled(true);}
    // Applies container-access policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onInventoryOpen(InventoryOpenEvent e){if(e.getPlayer() instanceof Player p&&e.getInventory().getHolder() instanceof org.bukkit.inventory.BlockInventoryHolder h&&!policy.allowsContainerAccess(p,h.getBlock()))e.setCancelled(true);}
    // Applies item-drop policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onDrop(PlayerDropItemEvent e){if(!policy.allowsItemDrop(e.getPlayer()))e.setCancelled(true);}
    // Applies item-pickup policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onPickup(EntityPickupItemEvent e){if(e.getEntity() instanceof Player p&&!policy.allowsItemPickup(p,e.getItem().getLocation()))e.setCancelled(true);}
    // Applies fire-spread policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onFire(BlockSpreadEvent e){if(!policy.allowsFire(e))e.setCancelled(true);}
    // Applies water/lava flow policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onFlow(BlockFromToEvent e){if(!policy.allowsFlow(e))e.setCancelled(true);}
    // Applies entity and block explosion policies.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onEntityExplosion(EntityExplodeEvent e){if(!policy.allowsExplosion(e.getLocation(),e.getEntity()))e.setCancelled(true);}
    // Applies block explosion policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBlockExplosion(BlockExplodeEvent e){if(!regions.hasFlag(e.getBlock().getLocation(),RegionFlag.EXPLOSION))e.setCancelled(true);}
    // Applies TNT policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onTnt(TNTPrimeEvent e){if(!policy.allowsTnt(e.getBlock().getLocation()))e.setCancelled(true);}
    // Applies Enderman griefing policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onEnderman(EntityChangeBlockEvent e){if(!policy.allowsEnderman(e.getEntity(),e.getBlock().getLocation()))e.setCancelled(true);}
    // Applies lightning policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onLightning(LightningStrikeEvent e){if(!policy.allowsLightning(e.getLightning().getLocation()))e.setCancelled(true);}
    // Applies crop-growth policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onCrop(BlockGrowEvent e){if(!policy.allowsCrop(e.getBlock().getLocation()))e.setCancelled(true);}
    // Applies leaf-decay policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onLeaf(LeavesDecayEvent e){if(!policy.allowsLeaf(e.getBlock().getLocation()))e.setCancelled(true);}
    // Applies trampling policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onTrample(EntityInteractEvent e){if(e.getEntity() instanceof Player p&&regions.find(e.getBlock().getLocation()).map(r->r.isOwner(p.getUniqueId())||r.isMember(p.getUniqueId())).orElse(false))return;if(!policy.allowsTrampling(e.getBlock().getLocation()))e.setCancelled(true);}
    // Applies sleep policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onSleep(PlayerBedEnterEvent e){if(!policy.allowsSleep(e.getPlayer(),e.getBed().getLocation()))e.setCancelled(true);}
    // Applies chorus teleport policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onChorus(PlayerItemConsumeEvent e){if(e.getItem().getType()==Material.CHORUS_FRUIT&&!policy.allowsChorus(e.getPlayer()))e.setCancelled(true);}
    // Applies teleport and ender-pearl policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onTeleport(PlayerTeleportEvent e){if(e.getTo()==null){e.setCancelled(true);return;}if(e.getCause()==PlayerTeleportEvent.TeleportCause.ENDER_PEARL&&!policy.allowsPearl(e.getPlayer(),e.getTo())){e.setCancelled(true);return;}if(!boundaryAllowed(e.getFrom(),e.getTo()))e.setCancelled(true);}
    // Applies natural health regeneration policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onRegen(EntityRegainHealthEvent e){if(!policy.allowsRegen(e))e.setCancelled(true);}
    // Applies natural hunger policy.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onHunger(FoodLevelChangeEvent e){if(!policy.allowsHunger(e))e.setCancelled(true);}
    // Enforces region entry and exit permissions at movement boundaries.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBoundary(PlayerMoveEvent e){if(e.getTo()==null||sameBlock(e.getFrom(),e.getTo()))return;if(regions.find(e.getTo()).map(r->r.isOwner(e.getPlayer().getUniqueId())||r.isMember(e.getPlayer().getUniqueId())).orElse(false))return;if(!boundaryAllowed(e.getFrom(),e.getTo()))e.setCancelled(true);}
    // Detects region transitions after movement.
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onMove(PlayerMoveEvent e){if(e.getTo()!=null&&!sameBlock(e.getFrom(),e.getTo()))transitions.update(e.getPlayer(),e.getTo());}
    // Detects transitions after a world change.
    @EventHandler(priority=EventPriority.MONITOR) public void onWorldChange(PlayerChangedWorldEvent e){transitions.update(e.getPlayer(),e.getPlayer().getLocation());}
    // Clears transition state when a player quits.
    @EventHandler public void onQuit(PlayerQuitEvent e){transitions.clear(e.getPlayer().getUniqueId());}
    private boolean boundaryAllowed(Location a,Location b){PixelRegion from=regions.find(a).orElse(null),to=regions.find(b).orElse(null);if(from==null||to==null||from.id().equals(to.id()))return true;return policy.allowsExit(a)&&policy.allowsEntry(b);}
    private static boolean sameBlock(Location a,Location b){return a.getWorld()==b.getWorld()&&a.getBlockX()==b.getBlockX()&&a.getBlockY()==b.getBlockY()&&a.getBlockZ()==b.getBlockZ();}
}
