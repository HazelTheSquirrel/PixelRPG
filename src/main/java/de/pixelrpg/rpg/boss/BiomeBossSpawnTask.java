package de.pixelrpg.rpg.boss;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class BiomeBossSpawnTask implements Listener {
    /** Biome bosses are intentionally rare world encounters rather than routine mob spawns. */
    private final double spawnChancePercent;

    private final Plugin plugin;
    private final BossRepository bossRepository;
    private final BossManager bossManager;
    private final double spawnRadius;
    private final int maxConcurrentBosses;
    private final int intervalTicks;
    private final Map<UUID, Biome> playerBiomes = new ConcurrentHashMap<>();
    private BukkitTask task;

    public BiomeBossSpawnTask(Plugin plugin, BossRepository bossRepository, BossManager bossManager,
                              double spawnRadius, int checkIntervalSeconds, int maxConcurrentBosses, double spawnChancePercent) {
        this.plugin = plugin;
        this.bossRepository = bossRepository;
        this.bossManager = bossManager;
        this.spawnRadius = Math.max(8.0D, spawnRadius);
        this.intervalTicks = Math.max(20, checkIntervalSeconds * 20);
        this.maxConcurrentBosses = Math.max(1, maxConcurrentBosses);
        this.spawnChancePercent = Math.clamp(spawnChancePercent, 0.01D, 100.0D);
    }

    public void start() {
        if (task != null) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) rememberBiome(player);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkPlayers, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        playerBiomes.clear();
        HandlerList.unregisterAll(this);
    }

    private void checkPlayers() {
        if (bossManager.getActiveBossCount() >= maxConcurrentBosses) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || !player.isValid() || !bossManager.isRegistered(player.getUniqueId())) continue;
            Biome biome = playerBiomes.get(player.getUniqueId());
            if (biome == null) {
                rememberBiome(player);
                biome = playerBiomes.get(player.getUniqueId());
            }
            if (biome == null) continue;
            BossDefinition definition = bossRepository.getBiomeBoss(biome);
            if (definition == null || bossManager.hasActiveBossOfType(definition.getId())) continue;
            if (ThreadLocalRandom.current().nextDouble(100.0D) >= spawnChancePercent) continue;
            Location location = findSpawnLocation(player, definition);
            if (location == null) continue;
            bossManager.spawnBiomeBoss(definition, location);
            break;
        }
    }

    // Joining initializes the player's cached biome before the next temporal spawn check.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        rememberBiome(event.getPlayer());
    }

    // Movement invalidates the cached biome only when the player crosses a block boundary.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;
        rememberBiome(event.getPlayer());
    }

    // A world change invalidates the player's biome cache immediately.
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        rememberBiome(event.getPlayer());
    }

    // Disconnecting a player removes its cached spawn-check state.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerBiomes.remove(event.getPlayer().getUniqueId());
    }

    private void rememberBiome(Player player) {
        if (player == null || !player.isOnline() || player.getWorld() == null) return;
        playerBiomes.put(player.getUniqueId(), player.getLocation().getBlock().getBiome());
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
