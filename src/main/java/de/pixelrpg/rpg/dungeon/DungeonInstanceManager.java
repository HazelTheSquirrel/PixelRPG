// src/main/java/de/pixelrpg/rpg/dungeon/DungeonInstanceManager.java
package de.pixelrpg.rpg.dungeon;

import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.api.events.DungeonClearedEvent;
import de.pixelrpg.rpg.boss.BossDefinition;
import de.pixelrpg.rpg.boss.BossManager;
import de.pixelrpg.rpg.boss.BossRepository;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.item.RuneItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class DungeonInstanceManager {

    private final Plugin plugin;
    private final DungeonRepository repository;
    private final GuildAPI guildAPI;
    private final EconomyAPI economyAPI;
    private final PartyAPI partyAPI;
    private final MobScalingConfig mobScalingConfig;
    private final BossRepository bossRepository;
    private final BossManager bossManager;

    private final String instanceWorldName;
    private final int slotSpacing;
    private final int cleanupDelayMinutes;

    private final Map<UUID, DungeonInstance> instancesById = new ConcurrentHashMap<>();
    private final Map<Long, Long> cooldownByPlayerAndDungeon = new ConcurrentHashMap<>();

    private int nextSlot = 0;

    public DungeonInstanceManager(Plugin plugin, DungeonRepository repository, GuildAPI guildAPI,
                                   EconomyAPI economyAPI, PartyAPI partyAPI, MobScalingConfig mobScalingConfig,
                                   BossRepository bossRepository, BossManager bossManager,
                                   String instanceWorldName, int slotSpacing, int cleanupDelayMinutes) {
        this.plugin = plugin;
        this.repository = repository;
        this.guildAPI = guildAPI;
        this.economyAPI = economyAPI;
        this.partyAPI = partyAPI;
        this.mobScalingConfig = mobScalingConfig;
        this.bossRepository = bossRepository;
        this.bossManager = bossManager;
        this.instanceWorldName = instanceWorldName;
        this.slotSpacing = slotSpacing;
        this.cleanupDelayMinutes = cleanupDelayMinutes;
    }

    private World getOrCreateInstanceWorld() {
        World world = Bukkit.getWorld(instanceWorldName);
        if (world != null) {
            return world;
        }
        WorldCreator creator = new WorldCreator(instanceWorldName);
        creator.environment(World.Environment.NORMAL);
        creator.generator(new DungeonVoidGenerator());
        world = Bukkit.createWorld(creator);
        if (world != null) {
            world.setSpawnFlags(false, false);
            world.setDifficulty(org.bukkit.Difficulty.NORMAL);
        }
        return world;
    }

    public enum EnterResult {
        SUCCESS,
        DUNGEON_NOT_FOUND,
        NO_SCHEMATIC,
        RANK_TOO_LOW,
        ON_COOLDOWN
    }

    public EnterResult enterDungeon(org.bukkit.entity.Player player, String dungeonId) {
        DungeonDefinition definition = repository.get(dungeonId);
        if (definition == null) {
            return EnterResult.DUNGEON_NOT_FOUND;
        }
        if (!definition.hasSchematic()) {
            return EnterResult.NO_SCHEMATIC;
        }
        if (!guildAPI.getRank(player.getUniqueId()).isAtLeast(definition.getMinRank())) {
            return EnterResult.RANK_TOO_LOW;
        }

        long cooldownKey = cooldownMapKey(player.getUniqueId(), dungeonId);
        Long expiresAt = cooldownByPlayerAndDungeon.get(cooldownKey);
        if (expiresAt != null && System.currentTimeMillis() < expiresAt) {
            return EnterResult.ON_COOLDOWN;
        }

        Set<UUID> partyMembers = new HashSet<>(partyAPI.getPartyMembers(player.getUniqueId()));

        SchematicData schematic;
        try {
            schematic = SchematicIO.load(repository.schematicFile(definition.getSchematicFile()));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load schematic for dungeon " + dungeonId, e);
            return EnterResult.NO_SCHEMATIC;
        }

        Location origin = allocateOrigin();
        SchematicIO.paste(schematic, origin);

        UUID instanceId = UUID.randomUUID();
        DungeonInstance instance = new DungeonInstance(instanceId, dungeonId, origin, partyMembers);
        instancesById.put(instanceId, instance);

        spawnMarkers(definition, instance);

        Location entrance = origin.clone();
        for (RelativeMarker marker : definition.getMarkers()) {
            if (marker.getType() == DungeonMarkerType.ENTRANCE) {
                entrance = origin.clone().add(marker.getDx() + 0.5, marker.getDy(), marker.getDz() + 0.5);
                break;
            }
        }

        for (UUID memberUuid : partyMembers) {
            org.bukkit.entity.Player member = Bukkit.getPlayer(memberUuid);
            if (member == null || !member.isOnline()) {
                continue;
            }
            member.teleportAsync(entrance);
            member.showTitle(Title.title(
                    Component.text(definition.getDisplayName(), NamedTextColor.DARK_RED),
                    Component.text("Rank " + definition.getMinRank().name() + " - " + definition.getMaxRank().name(), NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(2000), Duration.ofMillis(400))
            ));
            cooldownByPlayerAndDungeon.put(cooldownMapKey(memberUuid, dungeonId),
                    System.currentTimeMillis() + (definition.getCooldownMinutes() * 60_000L));
        }

        scheduleCleanup(instance, schematic);
        return EnterResult.SUCCESS;
    }

    private long cooldownMapKey(UUID uuid, String dungeonId) {
        return uuid.getMostSignificantBits() ^ dungeonId.hashCode();
    }

    private Location allocateOrigin() {
        World world = getOrCreateInstanceWorld();
        int slot = nextSlot++;
        int x = slot * slotSpacing;
        return new Location(world, x, 100, 0);
    }

    private void spawnMarkers(DungeonDefinition definition, DungeonInstance instance) {
        MobScalingConfig.RankBaseStats minStats = mobScalingConfig.getBaseStats(definition.getMinRank());
        MobScalingConfig.RankBaseStats maxStats = mobScalingConfig.getBaseStats(definition.getMaxRank());

        for (RelativeMarker marker : definition.getMarkers()) {
            Location location = instance.getOrigin().clone().add(marker.getDx() + 0.5, marker.getDy(), marker.getDz() + 0.5);

            switch (marker.getType()) {
                case MOB_SPAWN -> spawnRegularMob(instance, location, marker, minStats, maxStats);
                case BOSS_SPAWN -> spawnBoss(instance, location, marker, definition);
                case LOOT_CHEST -> fillChest(location, definition);
                case ENTRANCE -> {
                }
            }
        }
    }

    private void spawnRegularMob(DungeonInstance instance, Location location, RelativeMarker marker,
                                  MobScalingConfig.RankBaseStats minStats, MobScalingConfig.RankBaseStats maxStats) {
        EntityType type;
        try {
            type = marker.getMobType() != null ? EntityType.valueOf(marker.getMobType().toUpperCase()) : EntityType.ZOMBIE;
        } catch (IllegalArgumentException e) {
            type = EntityType.ZOMBIE;
        }

        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);

        double hp = (minStats.hp() + maxStats.hp()) / 2.0;
        double damage = (minStats.damage() + maxStats.damage()) / 2.0;

        AttributeInstance hpAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (hpAttribute != null) {
            hpAttribute.setBaseValue(hp);
            entity.setHealth(hp);
        }
        AttributeInstance dmgAttribute = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttribute != null) {
            dmgAttribute.setBaseValue(damage);
        }

        entity.getPersistentDataContainer().set(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER, instance.hashCode() % 7);
        entity.getPersistentDataContainer().set(RPGKeys.Dungeon.instanceId(), PersistentDataType.STRING, instance.getInstanceId().toString());

        instance.getSpawnedEntities().add(entity.getUniqueId());
    }

    private void spawnBoss(DungeonInstance instance, Location location, RelativeMarker marker, DungeonDefinition definition) {
        BossDefinition bossDefinition = marker.getMobType() != null ? bossRepository.get(marker.getMobType()) : null;

        LivingEntity entity;
        if (bossDefinition != null) {
            entity = bossManager.spawnWorldBoss(bossDefinition, location);
        } else {
            entity = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
            MobScalingConfig.RankBaseStats maxStats = mobScalingConfig.getBaseStats(definition.getMaxRank());
            AttributeInstance hpAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
            if (hpAttribute != null) {
                hpAttribute.setBaseValue(maxStats.hp() * 3.0);
                entity.setHealth(maxStats.hp() * 3.0);
            }
            entity.getPersistentDataContainer().set(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER, definition.getMaxRank().ordinal());
            entity.getPersistentDataContainer().set(RPGKeys.Boss.bossId(), PersistentDataType.STRING, definition.getId());
            entity.customName(Component.text(definition.getDisplayName() + " Guardian", NamedTextColor.DARK_RED));
            entity.setCustomNameVisible(true);
        }

        entity.getPersistentDataContainer().set(RPGKeys.Dungeon.instanceId(), PersistentDataType.STRING, instance.getInstanceId().toString());
        instance.getSpawnedEntities().add(entity.getUniqueId());
    }

// src/main/java/de/pixelrpg/rpg/dungeon/DungeonInstanceManager.java (Ausschnitt: fillChest() — Rank statt Level)
    private void fillChest(Location location, DungeonDefinition definition) {
        Block block = location.getBlock();
        block.setType(Material.CHEST, false);
        BlockState state = block.getState();
        if (!(state instanceof Chest chest)) {
            return;
        }

        ItemRarity rarity = ItemRarity.rollRandom();
        Material[] pool = {Material.IRON_SWORD, Material.DIAMOND_CHESTPLATE, Material.BOW, Material.SHIELD};
        Material material = pool[ThreadLocalRandom.current().nextInt(pool.length)];

        RPGItemBuilder.createUnidentified(material, rarity, definition.getMaxRank())
                .ifPresent(item -> chest.getBlockInventory().addItem(item));
        chest.getBlockInventory().addItem(RuneItemFactory.createRandom());
        chest.update(true, false);
    }

    private void scheduleCleanup(DungeonInstance instance, SchematicData schematic) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID entityUuid : instance.getSpawnedEntities()) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(entityUuid);
                if (entity != null) {
                    entity.remove();
                }
            }
            SchematicIO.clear(schematic, instance.getOrigin());
            instancesById.remove(instance.getInstanceId());
        }, cleanupDelayMinutes * 60L * 20L);
    }

    public void onBossDefeated(String dungeonId, UUID instanceId) {
        DungeonInstance instance = instancesById.get(instanceId);
        if (instance == null || instance.isBossDefeated()) {
            return;
        }
        instance.setBossDefeated(true);

        DungeonDefinition definition = repository.get(dungeonId);
        double moneyReward = definition != null ? (definition.getMaxRank().ordinal() + 1) * 50.0 : 100.0;
        Set<UUID> participants = new HashSet<>();

        for (UUID memberUuid : instance.getPartyMembers()) {
            org.bukkit.entity.Player member = Bukkit.getPlayer(memberUuid);
            if (member == null || !member.isOnline()) {
                continue;
            }
            if (guildAPI.isRegistered(memberUuid)) {
                economyAPI.deposit(memberUuid, moneyReward);
                member.sendMessage(Component.text("Dungeon boss defeated! +" + moneyReward + " gold.", NamedTextColor.GOLD));
                participants.add(memberUuid);
            }
        }

        Bukkit.getPluginManager().callEvent(new DungeonClearedEvent(dungeonId, participants));
    }

    public Map<UUID, DungeonInstance> getInstances() {
        return instancesById;
    }
}