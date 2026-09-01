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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PlayerProfileManager implements GuildAPI, EconomyAPI {
    private final Plugin plugin;
    private final Map<UUID, PlayerProfile> activeProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerProfile> loadingCache = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> saveChain = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> saveRequested = ConcurrentHashMap.newKeySet();
    private final CopyOnWriteArrayList<Consumer<UUID>> profileChangeListeners = new CopyOnWriteArrayList<>();
    private PlayerProfileRepository repository;
    private DatabaseManager databaseManager;
    private StorageType storageType;
    private ExecutorService saveExecutor;
    private int loadTimeoutSeconds = 8;
    private volatile boolean shuttingDown;

    public PlayerProfileManager(Plugin plugin) { this.plugin = plugin; }

    public void initialize(FileConfiguration config) {
        shuttingDown = false;
        storageType = StorageType.fromString(config.getString("storage.type", "YAML"));
        loadTimeoutSeconds = Math.max(1, config.getInt("storage.load-timeout-seconds", 8));
        int threads = Math.max(1, config.getInt("storage.save-executor-threads", 4));
        saveExecutor = Executors.newFixedThreadPool(threads, runnable -> { Thread thread = new Thread(runnable, "PixelRPG-ProfileIO"); thread.setDaemon(true); return thread; });
        try {
            if (storageType == StorageType.MYSQL) { databaseManager = new DatabaseManager(); databaseManager.connect(config); repository = new MySQLPlayerProfileRepository(databaseManager); }
            else repository = new YamlPlayerProfileRepository(plugin.getDataFolder());
            repository.init();
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize storage, falling back to YAML.", exception);
            storageType = StorageType.YAML; repository = new YamlPlayerProfileRepository(plugin.getDataFolder());
            try { repository.init(); } catch (Exception fallbackException) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "YAML fallback storage initialization failed.", fallbackException); }
        }
        Bukkit.getServicesManager().register(GuildAPI.class, this, plugin, ServicePriority.Normal);
        Bukkit.getServicesManager().register(EconomyAPI.class, this, plugin, ServicePriority.Normal);
    }

    /** Registers a lightweight callback invoked when an active profile changes. */
    public void addProfileChangeListener(Consumer<UUID> listener) {
        if (listener != null) profileChangeListeners.addIfAbsent(listener);
    }

    /** Removes a previously registered reactive profile listener. */
    public void removeProfileChangeListener(Consumer<UUID> listener) {
        if (listener != null) profileChangeListeners.remove(listener);
    }

    public void shutdown() {
        if (shuttingDown) return; shuttingDown = true;
        List<CompletableFuture<Void>> pending = activeProfiles.values().stream().map(profile -> captureAndEnqueue(profile.getUuid(), profile)).toList();
        try { CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).get(30, TimeUnit.SECONDS); }
        catch (Exception exception) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Not all player profiles could be flushed cleanly on shutdown.", exception); }
        if (saveExecutor != null) { saveExecutor.shutdown(); try { if (!saveExecutor.awaitTermination(10, TimeUnit.SECONDS)) saveExecutor.shutdownNow(); } catch (InterruptedException exception) { saveExecutor.shutdownNow(); Thread.currentThread().interrupt(); } }
        saveChain.clear(); saveRequested.clear(); loadingCache.clear(); activeProfiles.clear(); profileChangeListeners.clear();
        if (repository != null) { try { repository.shutdown(); } catch (RuntimeException exception) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to close player-profile repository.", exception); } }
        repository = null; databaseManager = null;
        Bukkit.getServicesManager().unregister(GuildAPI.class, this); Bukkit.getServicesManager().unregister(EconomyAPI.class);
    }

    public enum LoadOutcome { SUCCESS, FAILED }
    public LoadOutcome loadForPreLogin(UUID uuid) {
        if (shuttingDown) return LoadOutcome.FAILED;
        try { return enqueue(uuid, () -> { try { PlayerProfile profile = repository.load(uuid).orElseGet(() -> new PlayerProfile(uuid)); loadingCache.put(uuid, profile); return LoadOutcome.SUCCESS; } catch (Exception exception) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load profile for " + uuid, exception); return LoadOutcome.FAILED; } }).get(loadTimeoutSeconds, TimeUnit.SECONDS); }
        catch (Exception exception) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Profile load timed out or failed for " + uuid, exception); return LoadOutcome.FAILED; }
    }

    public void activateOnJoin(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = loadingCache.remove(uuid);
        if (profile == null) profile = new PlayerProfile(uuid);
        wireProfile(profile);
        activeProfiles.put(uuid, profile);
    }

    public void deactivateOnQuit(UUID uuid) { PlayerProfile profile = activeProfiles.remove(uuid); if (profile != null) { profile.setDirtyCallback(null); persistAsync(profile); } }
    public Optional<PlayerProfile> getProfile(UUID uuid) { return Optional.ofNullable(activeProfiles.get(uuid)); }
    public void registerPlayer(Player player) { PlayerProfile profile = activeProfiles.computeIfAbsent(player.getUniqueId(), ignored -> { PlayerProfile created = new PlayerProfile(player.getUniqueId()); wireProfile(created); return created; }); if (profile.isRegistered()) return; profile.setRegistered(true); persistAsync(profile); Bukkit.getPluginManager().callEvent(new PlayerRegistrationEvent(player)); }
    public void unregisterPlayer(Player player) { PlayerProfile profile = activeProfiles.get(player.getUniqueId()); if (profile == null || !profile.isRegistered()) return; profile.resetProgress(); persistAsync(profile); Bukkit.getPluginManager().callEvent(new PlayerUnregistrationEvent(player)); }
    public void unlockWaypoint(UUID uuid, String waypointId) { PlayerProfile profile = activeProfiles.get(uuid); if (profile != null) { profile.unlockWaypoint(waypointId); persistAsync(profile); } }
    public void saveProfileAsync(UUID uuid) { PlayerProfile profile = activeProfiles.get(uuid); if (profile != null) persistAsync(profile); }
    private void persistAsync(PlayerProfile profile) { if (shuttingDown || saveExecutor == null || saveExecutor.isShutdown()) return; captureAndEnqueue(profile.getUuid(), profile); }

    private void wireProfile(PlayerProfile profile) {
        profile.setDirtyCallback(() -> {
            UUID uuid = profile.getUuid();
            for (Consumer<UUID> listener : profileChangeListeners) listener.accept(uuid);
        });
    }

    private CompletableFuture<Void> captureAndEnqueue(UUID uuid, PlayerProfile profile) {
        if (saveExecutor == null || saveExecutor.isShutdown()) return CompletableFuture.failedFuture(new IllegalStateException("Profile I/O executor is not running"));
        if (!saveRequested.add(uuid)) return CompletableFuture.completedFuture(null);
        PlayerProfile snapshot = profile.snapshotForSave();
        if (snapshot == null) { saveRequested.remove(uuid); return CompletableFuture.completedFuture(null); }
        CompletableFuture<Void> future = enqueueVoid(uuid, () -> persistSnapshot(snapshot, profile));
        future.whenComplete((ignored, throwable) -> { saveRequested.remove(uuid); if (!shuttingDown && profile.isDirty() && saveExecutor != null && !saveExecutor.isShutdown()) persistAsync(profile); });
        return future;
    }

    private void persistSnapshot(PlayerProfile snapshot, PlayerProfile liveProfile) {
        if (repository == null) return;
        try { long savedRevision = repository.save(snapshot); liveProfile.setPersistenceRevision(savedRevision); }
        catch (Exception exception) { liveProfile.markDirty(); plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save profile for " + snapshot.getUuid(), exception); if (storageType == StorageType.MYSQL) writeEmergencyBackup(snapshot); }
    }

    private void writeEmergencyBackup(PlayerProfile profile) {
        YamlPlayerProfileRepository emergency = null;
        try { File folder = new File(plugin.getDataFolder(), "emergency"); emergency = new YamlPlayerProfileRepository(folder); emergency.init(); emergency.save(profile); }
        catch (Exception exception) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Emergency backup failed for " + profile.getUuid(), exception); }
        finally { if (emergency != null) try { emergency.shutdown(); } catch (RuntimeException exception) { plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to close emergency profile repository.", exception); } }
    }

    private <T> CompletableFuture<T> enqueue(UUID uuid, Supplier<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>(); ExecutorService executor = saveExecutor;
        if (executor == null || executor.isShutdown()) { result.completeExceptionally(new IllegalStateException("Profile I/O executor is not running")); return result; }
        CompletableFuture<Void> chain = saveChain.compute(uuid, (id, previous) -> { CompletableFuture<Void> base = previous != null ? previous : CompletableFuture.completedFuture(null); return base.exceptionally(ignored -> null).thenRunAsync(() -> { try { result.complete(task.get()); } catch (Throwable throwable) { result.completeExceptionally(throwable); } }, executor); });
        chain.whenComplete((ignored, throwable) -> saveChain.remove(uuid, chain)); return result;
    }
    private CompletableFuture<Void> enqueueVoid(UUID uuid, Runnable task) { return enqueue(uuid, () -> { task.run(); return null; }); }

    @Override public boolean isRegistered(UUID uuid) { PlayerProfile profile = activeProfiles.get(uuid); return profile != null && profile.isRegistered(); }
    @Override public int getLevel(UUID uuid) { PlayerProfile profile = activeProfiles.get(uuid); return profile != null ? profile.getLevel() : Level.MIN_LEVEL; }
    @Override public long getExperience(UUID uuid) { PlayerProfile profile = activeProfiles.get(uuid); return profile != null ? profile.getExperience() : 0L; }
    @Override public void addExperience(UUID uuid, long amount) { PlayerProfile profile = activeProfiles.get(uuid); if (profile == null || !profile.isRegistered()) return; int before = profile.getLevel(); profile.addExperience(amount); int after = profile.getLevel(); persistAsync(profile); if (before != after) { Player player = Bukkit.getPlayer(uuid); if (player != null) Bukkit.getPluginManager().callEvent(new PlayerLevelUpEvent(player, before, after)); } }
    @Override public double getBalance(UUID uuid) { PlayerProfile profile = activeProfiles.get(uuid); return profile != null ? profile.getMoney() : 0.0; }
    @Override public void deposit(UUID uuid, double amount) { PlayerProfile profile = activeProfiles.get(uuid); if (profile != null && profile.isRegistered()) { profile.addMoney(amount); persistAsync(profile); } }
    @Override public boolean withdraw(UUID uuid, double amount) { PlayerProfile profile = activeProfiles.get(uuid); if (profile == null || !profile.isRegistered()) return false; boolean success = profile.removeMoney(amount); if (success) persistAsync(profile); return success; }
}
