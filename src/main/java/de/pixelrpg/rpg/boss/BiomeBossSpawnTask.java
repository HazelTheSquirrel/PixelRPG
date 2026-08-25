package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.api.GuildAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;

public final class BiomeBossSpawnTask {
    private static final double SPAWN_CHANCE_PERCENT = 10.0D;

    private final Plugin plugin;
    private final BossRepository bossRepository;
    private final BossManager bossManager;
    private final GuildAPI guildAPI;
    private final double spawnRadius;
    private final int maxConcurrentBosses;
    private final int intervalTicks;
    private BukkitTask task;

    public BiomeBossSpawnTask(Plugin plugin, BossRepository bossRepository, BossManager bossManager,
                              GuildAPI guildAPI, double spawnRadius, int checkIntervalSeconds, int maxConcurrentBosses) {
        this.plugin = plugin;
        this.bossRepository = bossRepository;
        this.bossManager = bossManager;
        this.guildAPI = guildAPI;
        this.spawnRadius = Math.max(8.0D, spawnRadius);
        this.intervalTicks = Math.max(20, checkIntervalSeconds * 20);
        this.maxConcurrentBosses = Math.max(1, maxConcurrentBosses);
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkPlayers, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    private void checkPlayers() {
        if (bossManager.getActiveBossCount() >= maxConcurrentBosses) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || !player.isValid() || !guildAPI.isRegistered(player.getUniqueId())) continue;
            Biome biome = player.getLocation().getBlock().getBiome();
            BossDefinition definition = bossRepository.getBiomeBoss(biome);
            if (definition == null || bossManager.hasActiveBossOfType(definition.getId())) continue;
            if (ThreadLocalRandom.current().nextDouble(100.0D) >= SPAWN_CHANCE_PERCENT) continue;

            Location location = findSpawnLocation(player, definition);
            if (location == null) continue;
            bossManager.spawnBiomeBoss(definition, location);
            break;
        }
    }

    private Location findSpawnLocation(Player player, BossDefinition definition) {
        World world = player.getWorld();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
            double distance = ThreadLocalRandom.current().nextDouble(8.0D, spawnRadius);
            int x = player.getLocation().getBlockX() + (int) (Math.cos(angle) * distance);
            int z = player.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;

            int y = world.getHighestBlockYAt(x, z);
            if (!definition.matchesBiome(world.getBiome(x, y, z))) continue;

            Location location = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
            if (!isSafe(location)) continue;
            return location;
        }
        return null;
    }

    private boolean isSafe(Location location) {
        var feet = location.getBlock();
        var head = feet.getRelative(0, 1, 0);
        return feet.isPassable() && head.isPassable();
    }
}
