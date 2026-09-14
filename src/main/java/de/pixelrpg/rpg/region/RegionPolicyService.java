package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/** Domain policy for region gameplay flags. Paper event adapters delegate decisions here. */
public final class RegionPolicyService {
    private final RegionManager regions;
    private final RegionSpawnService spawnService;

    public RegionPolicyService(RegionManager regions, RegionSpawnService spawnService) {
        this.regions = regions;
        this.spawnService = spawnService;
    }

    public boolean allowsPvp(Player attacker, Player victim) {
        return regions.hasFlag(attacker.getLocation(), RegionFlag.PVP)
                && regions.hasFlag(victim.getLocation(), RegionFlag.PVP);
    }

    public boolean allowsMobDamage(EntityDamageEvent event) {
        return !(event.getEntity() instanceof Monster)
                || regions.hasFlag(event.getEntity().getLocation(), RegionFlag.MOB_DAMAGE);
    }

    public boolean allowsAnimalDamage(EntityDamageByEntityEvent event) {
        return !(event.getEntity() instanceof Animals)
                || regions.hasFlag(event.getEntity().getLocation(), RegionFlag.DAMAGE_ANIMALS);
    }

    public boolean allowsFallDamage(EntityDamageEvent event) {
        return event.getCause() != EntityDamageEvent.DamageCause.FALL
                || regions.hasFlag(event.getEntity().getLocation(), RegionFlag.FALL_DAMAGE);
    }

    public boolean allowsMonsterSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        Location location = event.getLocation();
        boolean managed = spawnService.isManagedSpawn(entity)
                || regions.isExplicitSpawnPoint(location, event.getEntityType().name());
        if (managed) return true;
        if (!regions.hasFlag(location, RegionFlag.MOB_SPAWNING)) return false;
        return !regions.hasFlag(location, RegionFlag.DENY_SPAWN);
    }

    public boolean allowsBlockBreak(Location location) {
        return regions.hasFlag(location, RegionFlag.BLOCK_BREAK);
    }

    public boolean allowsBlockPlace(Location location) {
        return regions.hasFlag(location, RegionFlag.BLOCK_PLACE);
    }

    /** Resolves a clicked block to its fine-grained interaction permission. */
    public boolean allowsUse(Block block) {
        if (block == null) return true;
        RegionFlag flag = interactionFlag(block.getType());
        return flag == null || regions.hasFlag(block.getLocation(), flag);
    }

    /** Resolves a container block to its fine-grained container permission. */
    public boolean allowsContainerAccess(Block block) {
        if (block == null) return true;
        RegionFlag flag = containerFlag(block.getType());
        return flag == null || regions.hasFlag(block.getLocation(), flag);
    }

    public boolean allowsItemDrop(Location location) {
        return regions.hasFlag(location, RegionFlag.ITEM_DROP);
    }

    public boolean allowsItemPickup(Location location) {
        return regions.hasFlag(location, RegionFlag.ITEM_PICKUP);
    }

    public boolean allowsFireSpread(BlockSpreadEvent event) {
        Material type = event.getNewState().getType();
        return type != Material.FIRE && type != Material.SOUL_FIRE
                || regions.hasFlag(event.getBlock().getLocation(), RegionFlag.FIRE_SPREAD);
    }

    public boolean allowsLavaFlow(BlockFromToEvent event) {
        return event.getBlock().getType() != Material.LAVA
                || regions.hasFlag(event.getToBlock().getLocation(), RegionFlag.LAVA_FLOW);
    }

    public boolean allowsWaterFlow(BlockFromToEvent event) {
        return event.getBlock().getType() != Material.WATER
                || regions.hasFlag(event.getToBlock().getLocation(), RegionFlag.WATER_FLOW);
    }

    public boolean allowsEntityExplosion(EntityExplodeEvent event) {
        Location location = event.getLocation();
        if (!regions.hasFlag(location, RegionFlag.EXPLOSION)) return false;
        if (event.getEntity() instanceof Creeper && !regions.hasFlag(location, RegionFlag.CREEPER_EXPLOSION)) return false;
        return !isGhastFireball(event) || regions.hasFlag(location, RegionFlag.GHAST_FIREBALL);
    }

    public boolean allowsBlockExplosion(BlockExplodeEvent event) {
        return regions.hasFlag(event.getBlock().getLocation(), RegionFlag.EXPLOSION);
    }

    public boolean allowsTnt(Location location) {
        return regions.hasFlag(location, RegionFlag.TNT) && regions.hasFlag(location, RegionFlag.EXPLOSION);
    }

    public boolean allowsEndermanGrief(EntityChangeBlockEvent event) {
        return !(event.getEntity() instanceof Enderman)
                || regions.hasFlag(event.getBlock().getLocation(), RegionFlag.ENDERMAN_GRIEF);
    }

    public boolean allowsLightning(Location location) {
        return regions.hasFlag(location, RegionFlag.LIGHTNING);
    }

    public boolean allowsCropGrowth(BlockGrowEvent event) {
        return regions.hasFlag(event.getBlock().getLocation(), RegionFlag.CROP_GROWTH);
    }

    public boolean allowsLeafDecay(Location location) {
        return regions.hasFlag(location, RegionFlag.LEAF_DECAY);
    }

    public boolean allowsTrampling(EntityInteractEvent event) {
        return regions.hasFlag(event.getBlock().getLocation(), RegionFlag.BLOCK_TRAMPLING);
    }

    public boolean allowsAnvil(Location location) {
        return regions.hasFlag(location, RegionFlag.ANVIL_USE);
    }

    public boolean allowsRespawnAnchor(Location location) {
        return regions.hasFlag(location, RegionFlag.RESPAWN_ANCHORS);
    }

    public boolean allowsSleep(Location location) {
        return regions.hasFlag(location, RegionFlag.SLEEP);
    }

    public boolean allowsEnderPearl(Location location) {
        return regions.hasFlag(location, RegionFlag.ENDERPEARL);
    }

    public boolean allowsChorusFruit(Location location) {
        return regions.hasFlag(location, RegionFlag.CHORUS_FRUIT_TELEPORT);
    }

    public boolean allowsNaturalRegen(EntityRegainHealthEvent event) {
        RegainReason reason = event.getRegainReason();
        boolean natural = reason == RegainReason.REGEN || reason == RegainReason.SATIATED;
        return !natural || regions.hasFlag(event.getEntity().getLocation(), RegionFlag.NATURAL_HEALTH_REGEN);
    }

    public boolean allowsNaturalHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getFoodLevel() >= player.getFoodLevel()) return true;
        return regions.hasFlag(player.getLocation(), RegionFlag.NATURAL_HUNGER_DRAIN);
    }

    public boolean allowsEntry(Location location) {
        return regions.hasFlag(location, RegionFlag.ENTRY);
    }

    public boolean allowsExit(Location location) {
        return regions.hasFlag(location, RegionFlag.EXIT);
    }

    private static RegionFlag interactionFlag(Material material) {
        if (Tag.DOORS.isTagged(material)) return RegionFlag.DOOR_USE;
        if (Tag.TRAPDOORS.isTagged(material)) return RegionFlag.TRAPDOOR_USE;
        if (Tag.FENCE_GATES.isTagged(material)) return RegionFlag.FENCE_GATE_USE;
        if (Tag.BUTTONS.isTagged(material)) return RegionFlag.BUTTON_USE;
        if (Tag.PRESSURE_PLATES.isTagged(material)) return RegionFlag.PRESSURE_PLATE_USE;
        if (material == Material.LEVER) return RegionFlag.LEVER_USE;
        return null;
    }

    private static RegionFlag containerFlag(Material material) {
        return switch (material) {
            case CHEST, TRAPPED_CHEST -> RegionFlag.CHEST_USE;
            case BARREL -> RegionFlag.BARREL_USE;
            case HOPPER -> RegionFlag.HOPPER_USE;
            case DROPPER -> RegionFlag.DROPPER_USE;
            case DISPENSER -> RegionFlag.DISPENSER_USE;
            case FURNACE -> RegionFlag.FURNACE_USE;
            case BLAST_FURNACE -> RegionFlag.BLAST_FURNACE_USE;
            case SMOKER -> RegionFlag.SMOKER_USE;
            case BREWING_STAND -> RegionFlag.BREWING_STAND_USE;
            case ENCHANTING_TABLE -> RegionFlag.ENCHANTING_TABLE_USE;
            case CRAFTING_TABLE -> RegionFlag.CRAFTING_TABLE_USE;
            case ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> RegionFlag.ANVIL_USE;
            default -> material.name().endsWith("_SHULKER_BOX") ? RegionFlag.SHULKER_BOX_USE : null;
        };
    }

    private static boolean isGhastFireball(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return false;
        return fireball.getShooter() instanceof Ghast;
    }
}
