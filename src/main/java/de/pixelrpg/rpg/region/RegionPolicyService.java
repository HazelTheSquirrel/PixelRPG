package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

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

    public boolean allowsMonsterSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) return true;
        if (spawnService.isManagedSpawn(event.getEntity())
                || regions.isExplicitSpawnPoint(event.getLocation(), event.getEntityType().name())) return true;
        return regions.hasFlag(event.getLocation(), RegionFlag.MONSTER_SPAWN);
    }

    public boolean allowsBlockBreak(Location location) {
        return regions.hasFlag(location, RegionFlag.BLOCK_BREAK);
    }

    public boolean allowsBlockPlace(Location location) {
        return regions.hasFlag(location, RegionFlag.BLOCK_PLACE);
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

    public boolean allowsEntityExplosion(EntityExplodeEvent event) {
        Location location = event.getLocation();
        if (!regions.hasFlag(location, RegionFlag.EXPLOSION)) return false;
        if (event.getEntity() instanceof Creeper && !regions.hasFlag(location, RegionFlag.CREEPER_EXPLOSION)) return false;
        return !isGhastFireball(event) || regions.hasFlag(location, RegionFlag.GHAST_FIREBALL);
    }

    public boolean allowsBlockExplosion(BlockExplodeEvent event) {
        return regions.hasFlag(event.getBlock().getLocation(), RegionFlag.EXPLOSION);
    }

    public boolean allowsEndermanGrief(EntityChangeBlockEvent event) {
        return !(event.getEntity() instanceof Enderman)
                || regions.hasFlag(event.getBlock().getLocation(), RegionFlag.ENDERMAN_GRIEF);
    }

    private static boolean isGhastFireball(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return false;
        return fireball.getShooter() instanceof Ghast;
    }
}
