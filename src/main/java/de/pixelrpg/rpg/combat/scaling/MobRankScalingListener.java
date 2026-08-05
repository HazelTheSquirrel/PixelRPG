package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.Rank;
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

public final class MobRankScalingListener implements Listener {

    // TTL für den pro-Chunk zwischengespeicherten durchschnittlichen Spieler-Rang.
    // Verhindert wiederholte teure getNearbyPlayers()-Scans bei Massen-Spawns
    // im selben Gebiet (z. B. Nacht-Spawns, Spawner-Cluster).
    private static final long RANK_CACHE_TTL_MILLIS = 3000L;

    private record CachedRank(Rank rank, long expiresAtMillis) {
    }

    private final GuildAPI guildAPI;
    private final MobScalingConfig scalingConfig;
    private final Map<Long, CachedRank> nearbyRankCache = new ConcurrentHashMap<>();

    public MobRankScalingListener(GuildAPI guildAPI, MobScalingConfig scalingConfig) {
        this.guildAPI = guildAPI;
        this.scalingConfig = scalingConfig;
    }

    // Zuständig für die dynamische Skalierung von Monster-Stats (HP/Schaden) und
    // Rang-Zuweisung beim natürlichen Spawn, basierend auf umliegenden Spielern
    // und Regionsgrenzen.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMonsterSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (monster.getPersistentDataContainer().has(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER)) {
            return;
        }

        Rank averageNearbyRank = computeAverageNearbyRankCached(monster);

        RegionDangerProvider regionProvider = resolveRegionProvider();
        Rank regionMin = regionProvider.getMinRank(monster.getLocation());
        Rank regionMax = regionProvider.getMaxRank(monster.getLocation());

        World.Environment environment = monster.getWorld().getEnvironment();
        MobScalingConfig.DimensionModifier dimensionModifier = scalingConfig.getDimensionModifier(environment);

        int randomOffset = ThreadLocalRandom.current().nextInt(-1, 2);
        int targetOrdinal = averageNearbyRank.ordinal() + randomOffset + dimensionModifier.rankOffset();

        int minOrdinal = Math.max(0, regionMin.ordinal());
        int maxOrdinal = Math.min(Rank.values().length - 1, regionMax.ordinal());
        if (minOrdinal > maxOrdinal) {
            int tmp = minOrdinal;
            minOrdinal = maxOrdinal;
            maxOrdinal = tmp;
        }
        targetOrdinal = Math.max(minOrdinal, Math.min(targetOrdinal, maxOrdinal));

        Rank finalRank = Rank.fromOrdinalClamped(targetOrdinal);
        MobScalingConfig.RankBaseStats baseStats = scalingConfig.getBaseStats(finalRank);

        double maxHp = baseStats.hp() * dimensionModifier.hpMultiplier() * scalingConfig.getPlayerParityMultiplier();
        double damage = baseStats.damage() * dimensionModifier.damageMultiplier() * scalingConfig.getPlayerParityMultiplier();

        AttributeInstance hpAttribute = monster.getAttribute(Attribute.MAX_HEALTH);
        if (hpAttribute != null) {
            hpAttribute.setBaseValue(maxHp);
            monster.setHealth(maxHp);
        }

        AttributeInstance dmgAttribute = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttribute != null) {
            dmgAttribute.setBaseValue(damage);
        }

        monster.getPersistentDataContainer().set(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER, finalRank.ordinal());

        if (monster instanceof Zombie || monster instanceof Skeleton) {
            applyVisualGear(monster, finalRank);
        }
    }

    private Rank computeAverageNearbyRankCached(Monster monster) {
        long chunkKey = packChunkKey(monster.getWorld().getName(),
                monster.getLocation().getBlockX() >> 4, monster.getLocation().getBlockZ() >> 4);

        long now = System.currentTimeMillis();
        CachedRank cached = nearbyRankCache.get(chunkKey);
        if (cached != null && cached.expiresAtMillis() > now) {
            return cached.rank();
        }

        Rank computed = computeAverageNearbyRank(monster);
        nearbyRankCache.put(chunkKey, new CachedRank(computed, now + RANK_CACHE_TTL_MILLIS));
        return computed;
    }

    private long packChunkKey(String worldName, int chunkX, int chunkZ) {
        long worldHash = worldName.hashCode() & 0xFFFFL;
        return (worldHash << 48) | (((long) chunkX & 0xFFFFFFL) << 24) | ((long) chunkZ & 0xFFFFFFL);
    }

    private Rank computeAverageNearbyRank(Monster monster) {
        double scanRadius = 48.0;
        int totalOrdinal = 0;
        int count = 0;

        for (Player player : monster.getLocation().getNearbyPlayers(scanRadius)) {
            if (guildAPI.isRegistered(player.getUniqueId())) {
                totalOrdinal += guildAPI.getRank(player.getUniqueId()).ordinal();
                count++;
            }
        }

        if (count > 0) {
            return Rank.fromOrdinalClamped(Math.round((float) totalOrdinal / count));
        }

        Player nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        for (Player player : monster.getWorld().getPlayers()) {
            if (!guildAPI.isRegistered(player.getUniqueId())) {
                continue;
            }
            double distanceSquared = player.getLocation().distanceSquared(monster.getLocation());
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = player;
            }
        }

        return nearest != null ? guildAPI.getRank(nearest.getUniqueId()) : Rank.F;
    }

    private RegionDangerProvider resolveRegionProvider() {
        RegionDangerProvider provider = Bukkit.getServicesManager().load(RegionDangerProvider.class);
        return provider != null ? provider : new DefaultRegionDangerProvider();
    }

    private void applyVisualGear(Monster monster, Rank rank) {
        if (monster.getEquipment() == null) {
            return;
        }

        EntityType type = monster.getType();
        ItemStack chest = null;
        ItemStack legs = null;
        ItemStack weapon = null;

        switch (rank) {
            case S, A -> {
                chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
                legs = new ItemStack(Material.NETHERITE_LEGGINGS);
                weapon = new ItemStack(Material.NETHERITE_SWORD);
            }
            case B, C -> {
                chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
                legs = new ItemStack(Material.DIAMOND_LEGGINGS);
                weapon = new ItemStack(Material.DIAMOND_SWORD);
            }
            case D, E -> {
                chest = new ItemStack(Material.IRON_CHESTPLATE);
                legs = new ItemStack(Material.IRON_LEGGINGS);
                weapon = new ItemStack(Material.IRON_SWORD);
            }
            case F -> {
                chest = new ItemStack(Material.LEATHER_CHESTPLATE);
                weapon = new ItemStack(Material.STONE_SWORD);
            }
        }

        monster.getEquipment().setChestplate(chest);
        monster.getEquipment().setLeggings(legs);
        if (type == EntityType.ZOMBIE || type == EntityType.SKELETON) {
            monster.getEquipment().setItemInMainHand(weapon);
        }

        monster.getEquipment().setChestplateDropChance(0.0f);
        monster.getEquipment().setLeggingsDropChance(0.0f);
        monster.getEquipment().setItemInMainHandDropChance(0.0f);
    }
}