// src/main/java/de/pixelrpg/rpg/combat/scaling/MobRankScalingListener.java
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

import java.util.concurrent.ThreadLocalRandom;

public final class MobRankScalingListener implements Listener {

    private final GuildAPI guildAPI;
    private final MobScalingConfig scalingConfig;

    public MobRankScalingListener(GuildAPI guildAPI, MobScalingConfig scalingConfig) {
        this.guildAPI = guildAPI;
        this.scalingConfig = scalingConfig;
    }

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

        Rank averageNearbyRank = computeAverageNearbyRank(monster);

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