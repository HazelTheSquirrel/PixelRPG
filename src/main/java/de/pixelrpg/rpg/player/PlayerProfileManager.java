package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.events.PlayerLevelUpEvent;
import de.pixelrpg.rpg.api.events.PlayerRegistrationEvent;
import de.pixelrpg.rpg.api.events.PlayerUnregistrationEvent;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.storage.DatabaseManager;
import de.pixelrpg.rpg.storage.StorageType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class PlayerProfileManager implements GuildAPI, EconomyAPI {
    private final Plugin plugin;
    private final Map<UUID, PlayerProfile> activeProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerProfile> loadingCache = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> saveChain = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> saveRequested = ConcurrentHashMap.newKeySet();
    private PlayerProfileRepository repository;
    private DatabaseManager databaseManager;
    private StorageType storageType;
    private ExecutorService saveExecutor;
    private int loadTimeoutSeconds = 8;

    public PlayerProfileManager(Plugin plugin) { this.plugin = plugin; }

    public void initialize(FileConfiguration config) {
        storageType = StorageType.fromString(config.getString("storage.type", "YAML"));
        loadTimeoutSeconds = Math.max(1, config.getInt("storage.load-timeout-seconds", 8));
        int threads = Math.max(1, config.getInt("storage.save-executor-threads", 4));
        saveExecutor = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-ProfileIO");
            thread.setDaemon(true);
            return thread;
        });
        try {
            if (storageType == StorageType.MYSQL) {
                databaseManager = new DatabaseManager();
                databaseManager.connect(config);
                repository = new MySQLPlayerProfileRepository(databaseManager);
            } else repository = new YamlPlayerProfileRepository(plugin.getDataFolder());
            repository.init();
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize storage, falling back to YAML.", e);
            storageType = StorageType.YAML;
            repository = new YamlPlayerProfileRepository(plugin.getDataFolder());
            try { repository.init(); } catch (Exception ignored) { }
        }
        Bukkit.getServicesManager().register(GuildAPI.class, this, plugin, ServicePriority.Normal);
        Bukkit.getServicesManager().register(EconomyAPI.class, this, plugin, ServicePriority.Normal);
    }

    public void shutdown() {
        List<CompletableFuture<Void>> pending = activeProfiles.values().stream()
                .map(profile -> enqueueVoid(profile.getUuid(), () -> persistSync(profile))).toList();
        try { CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS); }
        catch (Exception e) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Not all player profiles could be flushed cleanly on shutdown.", e); }
        if (saveExecutor != null) {
            saveExecutor.shutdown();
            try {
                if (!saveExecutor.awaitTermination(10, TimeUnit.SECONDS)) saveExecutor.shutdownNow();
            } catch (InterruptedException e) {
                saveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        saveChain.clear();
        saveRequested.clear();
        loadingCache.clear();
        activeProfiles.clear();
        if (repository != null) repository.shutdown();
        Bukkit.getServicesManager().unregister(GuildAPI.class, this);
        Bukkit.getServicesManager().unregister(EconomyAPI.class, this);
    }

    public enum LoadOutcome { SUCCESS, FAILED }

    public LoadOutcome loadForPreLogin(UUID uuid) {
        try {
            return enqueue(uuid, () -> {
                try {
                    PlayerProfile profile = repository.load(uuid).orElseGet(() -> new PlayerProfile(uuid));
                    loadingCache.put(uuid, profile);
                    return LoadOutcome.SUCCESS;
                } catch (Exception e) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load profile for " + uuid, e);
                    return LoadOutcome.FAILED;
                }
            }).get(loadTimeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Profile load timed out or failed for " + uuid, e);
            return LoadOutcome.FAILED;
        }
    }

    public void activateOnJoin(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = loadingCache.remove(uuid);
        activeProfiles.put(uuid, profile != null ? profile : new PlayerProfile(uuid));
    }

    public void deactivateOnQuit(UUID uuid) {
        PlayerProfile profile = activeProfiles.remove(uuid);
        if (profile != null) persistAsync(profile);
    }

    public Optional<PlayerProfile> getProfile(UUID uuid) { return Optional.ofNullable(activeProfiles.get(uuid)); }

    public void registerPlayer(Player player) {
        PlayerProfile profile = activeProfiles.computeIfAbsent(player.getUniqueId(), PlayerProfile::new);
        if (profile.isRegistered()) return;
        profile.setRegistered(true);
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerRegistrationEvent(player));
    }

    public void unregisterPlayer(Player player) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.isRegistered()) return;
        profile.resetProgress();
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerUnregistrationEvent(player));
    }

    public void unlockWaypoint(UUID uuid, String waypointId) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile != null) {
            profile.unlockWaypoint(waypointId);
            persistAsync(profile);
        }
    }

    public void saveProfileAsync(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile != null) persistAsync(profile);
    }

    private void persistAsync(PlayerProfile profile) {
        UUID uuid = profile.getUuid();
        if (!saveRequested.add(uuid)) return;
        CompletableFuture<Void> future = enqueueVoid(uuid, () -> persistSync(profile));
        future.whenComplete((ignored, throwable) -> {
            saveRequested.remove(uuid);
            if (profile.isDirty() && saveExecutor != null && !saveExecutor.isShutdown()) persistAsync(profile);
        });
    }

    /** Takes a synchronized, deep persistence snapshot before any blocking storage operation. */
    private void persistSync(PlayerProfile profile) {
        PlayerProfile snapshot = profile.snapshotForSave();
        if (snapshot == null) return;
        try { repository.save(snapshot); }
        catch (Exception e) {
            profile.markDirty();
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save profile for " + profile.getUuid(), e);
            if (storageType == StorageType.MYSQL) writeEmergencyBackup(snapshot);
        }
    }

    private void writeEmergencyBackup(PlayerProfile profile) {
        try {
            File folder = new File(plugin.getDataFolder(), "emergency");
            YamlPlayerProfileRepository emergency = new YamlPlayerProfileRepository(folder);
            emergency.init();
            emergency.save(profile);
        } catch (Exception e) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Emergency backup failed for " + profile.getUuid(), e); }
    }

    private <T> CompletableFuture<T> enqueue(UUID uuid, Supplier<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFuture<Void> chain = saveChain.compute(uuid, (id, previous) -> {
            CompletableFuture<Void> base = previous != null ? previous : CompletableFuture.completedFuture(null);
            return base.exceptionally(ignored -> null).thenRunAsync(() -> {
                try { result.complete(task.get()); }
                catch (Throwable throwable) { result.completeExceptionally(throwable); }
            }, saveExecutor);
        });
        chain.whenComplete((ignored, throwable) -> saveChain.remove(uuid, chain));
        return result;
    }

    private CompletableFuture<Void> enqueueVoid(UUID uuid, Runnable task) { return enqueue(uuid, () -> { task.run(); return null; }); }

    @Override public boolean isRegistered(UUID uuid) { PlayerProfile p = activeProfiles.get(uuid); return p != null && p.isRegistered(); }
    @Override public int getLevel(UUID uuid) { PlayerProfile p = activeProfiles.get(uuid); return p != null ? p.getLevel() : Level.MIN_LEVEL; }
    @Override public long getExperience(UUID uuid) { PlayerProfile p = activeProfiles.get(uuid); return p != null ? p.getExperience() : 0L; }
    @Override public void addExperience(UUID uuid, long amount) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null || !profile.isRegistered()) return;
        int before = profile.getLevel();
        profile.addExperience(amount);
        int after = profile.getLevel();
        persistAsync(profile);
        if (before != after) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) Bukkit.getPluginManager().callEvent(new PlayerLevelUpEvent(player, before, after));
        }
    }
    @Override public double getBalance(UUID uuid) { PlayerProfile p = activeProfiles.get(uuid); return p != null ? p.getMoney() : 0.0; }
    @Override public void deposit(UUID uuid, double amount) { PlayerProfile p = activeProfiles.get(uuid); if (p != null && p.isRegistered()) { p.addMoney(amount); persistAsync(p); } }
    @Override public boolean withdraw(UUID uuid, double amount) { PlayerProfile p = activeProfiles.get(uuid); if (p == null || !p.isRegistered()) return false; boolean success = p.removeMoney(amount); if (success) persistAsync(p); return success; }
}
