package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class WorldBossSpawnTask {

    private final Plugin plugin;
    private final BossRepository bossRepository;
    private final BossManager bossManager;
    private final de.pixelrpg.rpg.api.GuildAPI guildAPI;
    private final boolean enabled;
    private final int intervalTicks;
    private final double spawnRadius;
    private final int maxConcurrentBosses;
    private final double spawnChancePercent;
    private BukkitTask task;

    public WorldBossSpawnTask(Plugin plugin, BossRepository bossRepository, BossManager bossManager,
                               de.pixelrpg.rpg.api.GuildAPI guildAPI, boolean enabled, int intervalMinutes,
                               double spawnRadius, int maxConcurrentBosses) {
        this(plugin, bossRepository, bossManager, guildAPI, enabled, intervalMinutes, spawnRadius,
                maxConcurrentBosses, plugin.getConfig().getDouble("bosses.auto-spawn.spawn-chance-percent", 15.0D));
    }

    public WorldBossSpawnTask(Plugin plugin, BossRepository bossRepository, BossManager bossManager,
                              de.pixelrpg.rpg.api.GuildAPI guildAPI, boolean enabled, int intervalMinutes,
                              double spawnRadius, int maxConcurrentBosses, double spawnChancePercent) {
        this.plugin = plugin;
        this.bossRepository = bossRepository;
        this.bossManager = bossManager;
        this.guildAPI = guildAPI;
        this.enabled = enabled;
        this.intervalTicks = Math.max(1, intervalMinutes) * 60 * 20;
        this.spawnRadius = Math.max(1.0D, spawnRadius);
        this.maxConcurrentBosses = Math.max(1, maxConcurrentBosses);
        this.spawnChancePercent = Math.max(0.0D, Math.min(100.0D, spawnChancePercent));
    }

    public void start() {
        if (!enabled || task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::attemptSpawn, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    private void attemptSpawn() {
        if (bossManager.getActiveBossCount() >= maxConcurrentBosses) return;
        if (ThreadLocalRandom.current().nextDouble(100.0D) >= spawnChancePercent) return;

        Player anchor = pickRandomRegisteredPlayer();
        if (anchor == null) return;

        EntityType entityType = pickRandomBossEntityType();
        if (entityType == null) return;

        BossDefinition definition = bossRepository.getForEntityType(entityType);
        if (definition == null) definition = createGenericDefinition(entityType);

        if (bossManager.hasActiveBossOfType(definition.getId())) return;

        Location spawnLocation = findSpawnLocation(anchor);
        if (spawnLocation == null) return;

        bossManager.spawnWorldBoss(definition, spawnLocation);
    }

    private EntityType pickRandomBossEntityType() {
        List<EntityType> candidates = java.util.Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .filter(EntityType::isSpawnable)
                .filter(type -> type.getEntityClass() != null)
                .filter(type -> Mob.class.isAssignableFrom(type.getEntityClass()))
                .toList();
        if (candidates.isEmpty()) return null;
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private BossDefinition createGenericDefinition(EntityType entityType) {
        String id = "auto_" + entityType.name().toLowerCase(java.util.Locale.ROOT);
        BossDefinition definition = new BossDefinition(id, "Boss " + readableName(entityType));
        definition.setBaseEntityType(entityType);
        definition.setLevel(30);
        definition.setHealthMultiplier(3.5D);
        definition.setDamageMultiplier(1.35D);
        definition.setScaleMultiplier(1.35D);
        definition.setPhases(List.of());
        definition.setLootConfig(new BossLootConfig(List.of(), ItemRarity.RARE, 250.0D, 500L));
        return definition;
    }

    private String readableName(EntityType entityType) {
        String raw = entityType.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder(raw.length());
        boolean capitalize = true;
        for (char character : raw.toCharArray()) {
            if (capitalize && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
            if (character == ' ') capitalize = true;
        }
        return result.toString();
    }

    private Player pickRandomRegisteredPlayer() {
        List<? extends Player> online = Bukkit.getOnlinePlayers().stream()
                .filter(player -> guildAPI.isRegistered(player.getUniqueId()))
                .toList();
        if (online.isEmpty()) return null;
        return online.get(ThreadLocalRandom.current().nextInt(online.size()));
    }

    private Location findSpawnLocation(Player anchor) {
        World world = anchor.getWorld();
        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        double distance = ThreadLocalRandom.current().nextDouble(spawnRadius * 0.5, spawnRadius);

        int x = anchor.getLocation().getBlockX() + (int) (Math.cos(angle) * distance);
        int z = anchor.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;

        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y + 1.0, z + 0.5);
    }
}
