// src/main/java/de/pixelrpg/rpg/boss/WorldBossSpawnTask.java
package de.pixelrpg.rpg.boss;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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

    public WorldBossSpawnTask(Plugin plugin, BossRepository bossRepository, BossManager bossManager,
                               de.pixelrpg.rpg.api.GuildAPI guildAPI, boolean enabled, int intervalMinutes,
                               double spawnRadius, int maxConcurrentBosses) {
        this.plugin = plugin;
        this.bossRepository = bossRepository;
        this.bossManager = bossManager;
        this.guildAPI = guildAPI;
        this.enabled = enabled;
        this.intervalTicks = Math.max(1, intervalMinutes) * 60 * 20;
        this.spawnRadius = spawnRadius;
        this.maxConcurrentBosses = maxConcurrentBosses;
    }

    public void start() {
        if (!enabled) {
            return;
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::attemptSpawn, intervalTicks, intervalTicks);
    }

    private void attemptSpawn() {
        if (bossManager.getActiveBossCount() >= maxConcurrentBosses) {
            return;
        }

        List<BossDefinition> definitions = bossRepository.getAll();
        if (definitions.isEmpty()) {
            return;
        }

        Player anchor = pickRandomRegisteredPlayer();
        if (anchor == null) {
            return;
        }

        BossDefinition definition = definitions.get(ThreadLocalRandom.current().nextInt(definitions.size()));
        if (bossManager.hasActiveBossOfType(definition.getId())) {
            return;
        }

        Location spawnLocation = findSpawnLocation(anchor);
        if (spawnLocation == null) {
            return;
        }

        bossManager.spawnWorldBoss(definition, spawnLocation);
    }

    private Player pickRandomRegisteredPlayer() {
        List<? extends Player> online = Bukkit.getOnlinePlayers().stream()
                .filter(player -> guildAPI.isRegistered(player.getUniqueId()))
                .toList();
        if (online.isEmpty()) {
            return null;
        }
        return online.get(ThreadLocalRandom.current().nextInt(online.size()));
    }

    private Location findSpawnLocation(Player anchor) {
        World world = anchor.getWorld();
        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        double distance = ThreadLocalRandom.current().nextDouble(spawnRadius * 0.5, spawnRadius);

        int x = anchor.getLocation().getBlockX() + (int) (Math.cos(angle) * distance);
        int z = anchor.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);

        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }

        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y + 1.0, z + 0.5);
    }
}