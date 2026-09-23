package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import java.util.Objects;

/** Pure region-policy adapter: all decisions resolve the current region and its effective flag. */
public final class RegionPolicyService {
    private final RegionManager regions; private final RegionSpawnService spawns;
    public RegionPolicyService(RegionManager regions,RegionSpawnService spawns){this.regions=Objects.requireNonNull(regions);this.spawns=Objects.requireNonNull(spawns);}
    public boolean allowsPvp(Player a,Player v){return allows(a,a.getLocation(),RegionFlag.PVP)&&allows(v,v.getLocation(),RegionFlag.PVP);}
    public boolean allowsMobDamage(EntityDamageEvent e){return !(e.getEntity() instanceof Monster)||regions.hasFlag(e.getEntity().getLocation(),RegionFlag.MOB_DAMAGE)||memberOrOwner(e.getEntity());}
    public boolean allowsAnimalDamage(EntityDamageByEntityEvent e){return !(e.getEntity() instanceof Animals)||regions.hasFlag(e.getEntity().getLocation(),RegionFlag.DAMAGE_ANIMALS)||e.getDamager() instanceof Player p&&memberOrOwner(p,e.getEntity().getLocation());}
    public boolean allowsFallDamage(EntityDamageEvent e){return e.getCause()!=EntityDamageEvent.DamageCause.FALL||regions.hasFlag(e.getEntity().getLocation(),RegionFlag.FALL_DAMAGE)||memberOrOwner(e.getEntity());}
    public boolean allowsMonsterSpawn(CreatureSpawnEvent e){Location l=e.getLocation();if(l.getWorld()==null||l.getWorld().getEnvironment()!=World.Environment.NORMAL)return true;if(spawns.isManagedSpawn(e.getEntity())||regions.isExplicitSpawnPoint(l,e.getEntityType().name()))return true;if(!regions.hasFlag(l,RegionFlag.MOB_SPAWNING)||regions.hasFlag(l,RegionFlag.DENY_SPAWN))return false;RegionFlag f=spawnFlag(e.getEntityType().name());return f==null||regions.hasFlag(l,f);}
    public boolean allowsBlockBreak(Player p,Location l){return allows(p,l,RegionFlag.BLOCK_BREAK);} public boolean allowsBlockPlace(Player p,Location l){return allows(p,l,RegionFlag.BLOCK_PLACE);}
    public boolean allowsEntityInteraction(Player p,Entity e){RegionFlag f=e instanceof ItemFrame?RegionFlag.ITEM_FRAME_USE:e instanceof ArmorStand?RegionFlag.ARMOR_STAND_USE:RegionFlag.ENTITY_INTERACTION;return allows(p,e.getLocation(),f);}
    public boolean allowsUse(Player p,Block b){RegionFlag f=interactionFlag(b.getType());return f==null||allows(p,b.getLocation(),f);}
    public boolean allowsContainerAccess(Player p,Block b){RegionFlag f=containerFlag(b.getType());return f==null||allows(p,b.getLocation(),f);}
    public boolean allowsItemDrop(Player p){return allows(p,p.getLocation(),RegionFlag.ITEM_DROP);} public boolean allowsItemPickup(Player p,Location l){return allows(p,l,RegionFlag.ITEM_PICKUP);}
    public boolean allowsFire(BlockSpreadEvent e){Material m=e.getNewState().getType();return (m!=Material.FIRE&&m!=Material.SOUL_FIRE)||regions.hasFlag(e.getBlock().getLocation(),RegionFlag.FIRE_SPREAD);}
    public boolean allowsFlow(BlockFromToEvent e){RegionFlag f=e.getBlock().getType()==Material.LAVA?RegionFlag.LAVA_FLOW:RegionFlag.WATER_FLOW;return !((e.getBlock().getType()==Material.LAVA||e.getBlock().getType()==Material.WATER))||regions.hasFlag(e.getToBlock().getLocation(),f);}
    public boolean allowsExplosion(Location l,Entity source){if(!regions.hasFlag(l,RegionFlag.EXPLOSION))return false;if(source instanceof Creeper&&!regions.hasFlag(l,RegionFlag.CREEPER_EXPLOSION))return false;if(source instanceof Fireball fb&&fb.getShooter() instanceof Ghast&&!regions.hasFlag(l,RegionFlag.GHAST_FIREBALL))return false;return true;}
    public boolean allowsTnt(Location l){return regions.hasFlag(l,RegionFlag.TNT)&&regions.hasFlag(l,RegionFlag.EXPLOSION);}
    public boolean allowsEnderman(Entity e,Location l){return !(e instanceof Enderman)||regions.hasFlag(l,RegionFlag.ENDERMAN_GRIEF);}
    public boolean allowsLightning(Location l){return regions.hasFlag(l,RegionFlag.LIGHTNING);} public boolean allowsCrop(Location l){return regions.hasFlag(l,RegionFlag.CROP_GROWTH);}
    public boolean allowsLeaf(Location l){return regions.hasFlag(l,RegionFlag.LEAF_DECAY);} public boolean allowsTrampling(Location l){return regions.hasFlag(l,RegionFlag.BLOCK_TRAMPLING);}
    public boolean allowsRespawnAnchor(Player p,Location l){return allows(p,l,RegionFlag.RESPAWN_ANCHORS);}
    public boolean allowsSleep(Player p,Location l){return allows(p,l,RegionFlag.SLEEP);} public boolean allowsPearl(Player p,Location l){return allows(p,l,RegionFlag.ENDERPEARL);}
    public boolean allowsChorus(Player p){return allows(p,p.getLocation(),RegionFlag.CHORUS_FRUIT_TELEPORT);}
    public boolean allowsRegen(EntityRegainHealthEvent e){boolean natural=e.getRegainReason()==EntityRegainHealthEvent.RegainReason.REGEN||e.getRegainReason()==EntityRegainHealthEvent.RegainReason.SATIATED;return !natural||regions.hasFlag(e.getEntity().getLocation(),RegionFlag.NATURAL_HEALTH_REGEN)||memberOrOwner(e.getEntity());}
    public boolean allowsHunger(FoodLevelChangeEvent e){if(!(e.getEntity() instanceof Player p)||e.getFoodLevel()>=p.getFoodLevel())return true;return allows(p,p.getLocation(),RegionFlag.NATURAL_HUNGER_DRAIN);}
    public boolean allowsEntry(Location l){return regions.hasFlag(l,RegionFlag.ENTRY);} public boolean allowsExit(Location l){return regions.hasFlag(l,RegionFlag.EXIT);}
    private boolean allows(Player p,Location l,RegionFlag f){return regions.find(l).map(r->r.isOwner(p.getUniqueId())||r.isMember(p.getUniqueId())||regions.hasFlag(l,f)).orElse(true);}
    private boolean memberOrOwner(Entity e){return e instanceof Player p&&memberOrOwner(p,e.getLocation());}
    private boolean memberOrOwner(Player p,Location l){return regions.find(l).map(r->r.isOwner(p.getUniqueId())||r.isMember(p.getUniqueId())).orElse(false);}
    private static RegionFlag spawnFlag(String t){return switch(t){case "BOGGED"->RegionFlag.SPAWN_BOGGED;case "PARCHED"->RegionFlag.SPAWN_PARCHED;case "SKELETON"->RegionFlag.SPAWN_SKELETON;case "STRAY"->RegionFlag.SPAWN_STRAY;case "CREEPER"->RegionFlag.SPAWN_CREEPER;case "ENDERMAN"->RegionFlag.SPAWN_ENDERMAN;case "GUARDIAN"->RegionFlag.SPAWN_GUARDIAN;case "ELDER_GUARDIAN"->RegionFlag.SPAWN_ELDER_GUARDIAN;case "BREEZE"->RegionFlag.SPAWN_BREEZE;case "CREAKING"->RegionFlag.SPAWN_CREAKING;case "PILLAGER"->RegionFlag.SPAWN_PILLAGER;case "EVOKER"->RegionFlag.SPAWN_EVOKER;case "VINDICATOR"->RegionFlag.SPAWN_VINDICATOR;case "RAVAGER"->RegionFlag.SPAWN_RAVAGER;case "WITCH"->RegionFlag.SPAWN_WITCH;case "SILVERFISH"->RegionFlag.SPAWN_SILVERFISH;case "SPIDER"->RegionFlag.SPAWN_SPIDER;case "CAVE_SPIDER"->RegionFlag.SPAWN_CAVE_SPIDER;case "PHANTOM"->RegionFlag.SPAWN_PHANTOM;case "SLIME"->RegionFlag.SPAWN_SLIME;case "WARDEN"->RegionFlag.SPAWN_WARDEN;case "ZOMBIE"->RegionFlag.SPAWN_ZOMBIE;case "DROWNED"->RegionFlag.SPAWN_DROWNED;case "HUSK"->RegionFlag.SPAWN_HUSK;case "ZOMBIE_VILLAGER"->RegionFlag.SPAWN_ZOMBIE_VILLAGER;case "ILLUSIONER"->RegionFlag.SPAWN_ILLUSIONER;default->null;};}
    private static RegionFlag interactionFlag(Material m){if(Tag.DOORS.isTagged(m))return RegionFlag.DOOR_USE;if(Tag.TRAPDOORS.isTagged(m))return RegionFlag.TRAPDOOR_USE;if(Tag.FENCE_GATES.isTagged(m))return RegionFlag.FENCE_GATE_USE;if(Tag.BUTTONS.isTagged(m))return RegionFlag.BUTTON_USE;if(Tag.PRESSURE_PLATES.isTagged(m))return RegionFlag.PRESSURE_PLATE_USE;if(m==Material.LEVER)return RegionFlag.LEVER_USE;return switch(m){case NOTE_BLOCK->RegionFlag.NOTE_BLOCK_USE;case JUKEBOX->RegionFlag.JUKEBOX_USE;case COMPOSTER->RegionFlag.COMPOSTER_USE;case LECTERN->RegionFlag.LECTERN_USE;case BEEHIVE->RegionFlag.BEEHIVE_USE;case BEE_NEST->RegionFlag.BEE_NEST_USE;case CAKE->RegionFlag.CAKE_USE;default->null;};}
    private static RegionFlag containerFlag(Material m){return switch(m){case CHEST,TRAPPED_CHEST->RegionFlag.CHEST_USE;case BARREL->RegionFlag.BARREL_USE;case HOPPER->RegionFlag.HOPPER_USE;case DROPPER->RegionFlag.DROPPER_USE;case DISPENSER->RegionFlag.DISPENSER_USE;case FURNACE->RegionFlag.FURNACE_USE;case BLAST_FURNACE->RegionFlag.BLAST_FURNACE_USE;case SMOKER->RegionFlag.SMOKER_USE;case BREWING_STAND->RegionFlag.BREWING_STAND_USE;case ENCHANTING_TABLE->RegionFlag.ENCHANTING_TABLE_USE;case CRAFTING_TABLE->RegionFlag.CRAFTING_TABLE_USE;case ANVIL,CHIPPED_ANVIL,DAMAGED_ANVIL->RegionFlag.ANVIL_USE;case GRINDSTONE->RegionFlag.GRINDSTONE_USE;case STONECUTTER->RegionFlag.STONECUTTER_USE;case LOOM->RegionFlag.LOOM_USE;case CARTOGRAPHY_TABLE->RegionFlag.CARTOGRAPHY_TABLE_USE;case SMITHING_TABLE->RegionFlag.SMITHING_TABLE_USE;default->m.name().endsWith("_SHULKER_BOX")?RegionFlag.SHULKER_BOX_USE:null;};}
}
