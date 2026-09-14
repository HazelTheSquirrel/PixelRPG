package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
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
import org.bukkit.event.entity.LightningStrikeEvent;
import org.bukkit.event.entity.TNTPrimeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

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
        if (!regions.hasFlag(location, RegionFlag.MONSTER_SPAWN) && entity instanceof Monster) return false;
        return !regions.hasFlag(location, RegionFlag.DENY_SPAWN);
    }

    public boolean allowsBlockBreak(Location location) {
        return regions.hasFlag(location, RegionFlag.BLOCK_BREAK);
    }

    public boolean allowsBlockPlace(Location location) {
        return regions.hasFlag(location, RegionFlag.BLOCK_PLACE);
    }

    public boolean allowsInteract(Location location) {
        return regions.hasFlag(location, RegionFlag.INTERACT);
    }

    public boolean allowsUse(Location location) {
        return regions.hasFlag(location, RegionFlag.USE);
    }

    public boolean allowsContainerAccess(Location location) {
        return regions.hasFlag(location, RegionFlag.CHEST_ACCESS);
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
        return regions.hasFlag(location, RegionFlag.USE_ANVIL);
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
        return event.getRegainReason() != RegainReason.NATURAL
                || regions.hasFlag(event.getEntity().getLocation(), RegionFlag.NATURAL_HEALTH_REGEN);
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

    private static boolean isGhastFireball(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return false;
        return fireball.getShooter() instanceof Ghast;
    }

    public boolean isAnvil(Material material) {
        return Tag.ANVIL.isTagged(material);
    }
}
