package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MobLevelScalingListener implements Listener {
    private static final long LEVEL_CACHE_TTL_MILLIS = 3000L;
    private static final double SCAN_RADIUS = 48.0;
    private record CachedLevel(int level, boolean noScaling, long expiresAtMillis) { }
    private final GuildAPI guildAPI;
    private final MobScalingConfig scalingConfig;
    private final Map<Long, CachedLevel> nearbyLevelCache = new ConcurrentHashMap<>();

    public MobLevelScalingListener(GuildAPI guildAPI, MobScalingConfig scalingConfig) { this.guildAPI = guildAPI; this.scalingConfig = scalingConfig; }

    // Zuständig für die Level-Skalierung von Monstern beim Spawn.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMonsterSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.getPersistentDataContainer().has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)) return;
        CachedLevel cached = computeAverageNearbyLevelCached(monster);
        if (cached.noScaling()) return;
        RegionDangerProvider regionProvider = resolveRegionProvider();
        int regionMin = Math.max(Level.MIN_LEVEL, regionProvider.getMinLevel(monster.getLocation()));
        int regionMax = Math.min(Level.MAX_NORMAL_LEVEL, regionProvider.getMaxLevel(monster.getLocation()));
        if (regionMin > regionMax) { int tmp = regionMin; regionMin = regionMax; regionMax = tmp; }
        MobScalingConfig.DimensionModifier dimension = scalingConfig.getDimensionModifier(monster.getWorld().getEnvironment());
        int targetLevel = cached.level() + ThreadLocalRandom.current().nextInt(-1, 2) + dimension.levelOffset();
        targetLevel = Math.max(regionMin, Math.min(targetLevel, regionMax));
        targetLevel = Math.max(Level.MIN_LEVEL, Math.min(targetLevel, Level.MAX_NORMAL_LEVEL));
        MobScalingConfig.LevelBaseStats stats = scalingConfig.getBaseStats(targetLevel);
        double maxHp = stats.hp() * dimension.hpMultiplier() * scalingConfig.getPlayerParityMultiplier();
        double damage = stats.damage() * dimension.damageMultiplier() * scalingConfig.getPlayerParityMultiplier();
        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) { hp.setBaseValue(maxHp); monster.setHealth(maxHp); }
        AttributeInstance attackDamage = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.setBaseValue(damage);
        monster.getPersistentDataContainer().set(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER, targetLevel);
        if (monster instanceof Zombie || monster instanceof Skeleton) applyVisualGear(monster, targetLevel);
    }

    private CachedLevel computeAverageNearbyLevelCached(Monster monster) {
        long key = packChunkKey(monster.getWorld().getName(), monster.getLocation().getBlockX() >> 4, monster.getLocation().getBlockZ() >> 4);
        long now = System.currentTimeMillis();
        CachedLevel cached = nearbyLevelCache.get(key);
        if (cached != null && cached.expiresAtMillis() > now) return cached;
        CachedLevel computed = computeAverageNearbyLevel(monster, now);
        nearbyLevelCache.put(key, computed);
        return computed;
    }

    private long packChunkKey(String worldName, int chunkX, int chunkZ) {
        long worldHash = worldName.hashCode() & 0xFFFFL;
        return (worldHash << 48) | (((long) chunkX & 0xFFFFFFL) << 24) | ((long) chunkZ & 0xFFFFFFL);
    }

    private CachedLevel computeAverageNearbyLevel(Monster monster, long now) {
        int total = 0, count = 0;
        for (Player player : monster.getLocation().getNearbyPlayers(SCAN_RADIUS)) {
            if (guildAPI.isRegistered(player.getUniqueId())) { total += guildAPI.getLevel(player.getUniqueId()); count++; }
        }
        if (count == 0) return new CachedLevel(Level.MIN_LEVEL, true, now + LEVEL_CACHE_TTL_MILLIS);
        int average = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, Math.round((float) total / count)));
        return new CachedLevel(average, false, now + LEVEL_CACHE_TTL_MILLIS);
    }

    private RegionDangerProvider resolveRegionProvider() {
        RegionDangerProvider provider = Bukkit.getServicesManager().load(RegionDangerProvider.class);
        return provider != null ? provider : new DefaultRegionDangerProvider();
    }

    private void applyVisualGear(Monster monster, int level) {
        if (monster.getEquipment() == null) return;
        ItemStack chest, legs = null, weapon;
        if (level >= 75) { chest = new ItemStack(Material.NETHERITE_CHESTPLATE); legs = new ItemStack(Material.NETHERITE_LEGGINGS); weapon = new ItemStack(Material.NETHERITE_SWORD); }
        else if (level >= 50) { chest = new ItemStack(Material.DIAMOND_CHESTPLATE); legs = new ItemStack(Material.DIAMOND_LEGGINGS); weapon = new ItemStack(Material.DIAMOND_SWORD); }
        else if (level >= 25) { chest = new ItemStack(Material.IRON_CHESTPLATE); legs = new ItemStack(Material.IRON_LEGGINGS); weapon = new ItemStack(Material.IRON_SWORD); }
        else { chest = new ItemStack(Material.LEATHER_CHESTPLATE); weapon = new ItemStack(Material.STONE_SWORD); }
        monster.getEquipment().setChestplate(chest);
        monster.getEquipment().setLeggings(legs);
        if (monster.getType() == EntityType.ZOMBIE || monster.getType() == EntityType.SKELETON) monster.getEquipment().setItemInMainHand(weapon);
        monster.getEquipment().setChestplateDropChance(0.0f);
        monster.getEquipment().setLeggingsDropChance(0.0f);
        monster.getEquipment().setItemInMainHandDropChance(0.0f);
    }
}
