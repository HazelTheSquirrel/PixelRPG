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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PlayerProfileManager implements EconomyAPI, GuildAPI, AutoCloseable {
    private final Plugin plugin;
    private final Map<UUID, PlayerProfile> activeProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerProfile> loadingProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> ioChains = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> pendingSaves = new ConcurrentHashMap<>();
    private final java.util.concurrent.CopyOnWriteArrayList<Consumer<UUID>> changeListeners =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private PlayerProfileRepository repository;
    private DatabaseManager database;
    private StorageType storageType;
    private ExecutorService ioExecutor;
    private int loadTimeoutSeconds = 8;
    private volatile boolean shuttingDown;

    public PlayerProfileManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize(FileConfiguration config) {
        shuttingDown = false;
        storageType = StorageType.fromString(config.getString("storage.type", "YAML"));
        loadTimeoutSeconds = Math.max(1, config.getInt("storage.load-timeout-seconds", 8));
        ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

        try {
            if (storageType == StorageType.MYSQL) {
                database = new DatabaseManager();
                database.connect(config);
                repository = new MySQLPlayerProfileRepository(database.dataSource(), ioExecutor);
            } else {
                repository = new YamlPlayerProfileRepository(
                        plugin.getDataFolder().toPath().resolve("players"), ioExecutor);
            }
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Profile storage initialization failed.", exception);
            closeStorage();
            throw new IllegalStateException("PixelRPG profile storage could not be initialized.", exception);
        }

        if (repository == null) {
            throw new IllegalStateException("PixelRPG profile repository is unavailable.");
        }
        registerServices();
    }

    private void registerServices() {
        Bukkit.getServicesManager().register(EconomyAPI.class, this, plugin, ServicePriority.Normal);
        Bukkit.getServicesManager().register(GuildAPI.class, this, plugin, ServicePriority.Normal);
    }

    public LoadOutcome loadForPreLogin(UUID uuid) {
        if (shuttingDown || repository == null) return LoadOutcome.FAILED;
        try {
            PlayerProfile profile = loadProfile(uuid)
                    .get(loadTimeoutSeconds, TimeUnit.SECONDS);
            loadingProfiles.put(uuid, profile);
            return LoadOutcome.SUCCESS;
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Failed to load profile for " + uuid, exception);
            return LoadOutcome.FAILED;
        }
    }

    private CompletableFuture<PlayerProfile> loadProfile(UUID uuid) {
        return enqueue(uuid, () -> repository.load(uuid)
                .thenApply(profile -> profile == null ? new PlayerProfile(uuid) : profile));
    }

    public void activateOnJoin(UUID uuid) {
        PlayerProfile profile = loadingProfiles.remove(uuid);
        if (profile == null) {
            profile = new PlayerProfile(uuid);
        }
        wire(profile);
        activeProfiles.put(uuid, profile);
    }

    public void activateOnJoin(Player player) {
        activateOnJoin(player.getUniqueId());
    }

    public void deactivateOnQuit(UUID uuid) {
        PlayerProfile profile = activeProfiles.remove(uuid);
        if (profile == null) return;
        profile.setDirtyCallback(null);
        persistAsync(profile);
    }

    public Optional<PlayerProfile> getProfile(UUID uuid) {
        return Optional.ofNullable(activeProfiles.get(uuid));
    }

    public PlayerProfile get(UUID uuid) {
        return activeProfiles.get(uuid);
    }

    public void addProfileChangeListener(Consumer<UUID> listener) {
        if (listener != null) changeListeners.addIfAbsent(listener);
    }

    public void removeProfileChangeListener(Consumer<UUID> listener) {
        if (listener != null) changeListeners.remove(listener);
    }

    public void notifyExternalStateChange(UUID uuid) {
        if (uuid == null || !activeProfiles.containsKey(uuid)) return;
        changeListeners.forEach(listener -> listener.accept(uuid));
    }

    public void registerPlayer(Player player) {
        PlayerProfile profile = activeProfiles.computeIfAbsent(player.getUniqueId(), id -> {
            PlayerProfile created = new PlayerProfile(id);
            wire(created);
            return created;
        });
        if (profile.registered()) return;
        profile.registered(true);
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerRegistrationEvent(player));
    }

    public void unregisterPlayer(Player player) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.registered()) return;
        profile.resetProgress();
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerUnregistrationEvent(player));
    }

    public void unlockWaypoint(UUID uuid, String waypointId) {
        if (waypointId == null || waypointId.isBlank()) return;
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null) return;
        profile.unlockWaypoint(waypointId);
        persistAsync(profile);
    }

    public void saveProfileAsync(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile != null) persistAsync(profile);
    }

    private void wire(PlayerProfile profile) {
        profile.setDirtyCallback(() -> {
            if (!shuttingDown) {
                UUID uuid = profile.uniqueId();
                changeListeners.forEach(listener -> listener.accept(uuid));
            }
        });
    }

    private void persistAsync(PlayerProfile profile) {
        if (shuttingDown || ioExecutor == null || ioExecutor.isShutdown()) return;
        captureAndEnqueue(profile);
    }

    private CompletableFuture<Void> captureAndEnqueue(PlayerProfile profile) {
        UUID uuid = profile.uniqueId();
        synchronized (pendingSaves) {
            CompletableFuture<Void> existing = pendingSaves.get(uuid);
            if (existing != null && !existing.isDone()) return existing;

            PlayerProfile snapshot = profile.snapshotForSave();
            if (snapshot == null) return CompletableFuture.completedFuture(null);
            snapshot.revision(snapshot.revision() + 1L);

            CompletableFuture<Void> future = enqueue(uuid, () -> repository.save(snapshot)
                    .thenRun(() -> {
                        profile.revision(snapshot.revision());
                        profile.markCleanIfRevision(snapshot.mutationRevision());
                    }));
            pendingSaves.put(uuid, future);
            future.whenComplete((ignored, failure) -> {
                pendingSaves.remove(uuid, future);
                if (failure != null) {
                    profile.markDirty();
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                            "Failed to save profile for " + uuid, failure);
                    if (storageType == StorageType.MYSQL) {
                        writeEmergencyBackup(snapshot);
                    }
                } else if (!shuttingDown && profile.isDirty()) {
                    persistAsync(profile);
                }
            });
            return future;
        }
    }

    private void writeEmergencyBackup(PlayerProfile profile) {
        YamlPlayerProfileRepository emergency = null;
        try {
            File folder = new File(plugin.getDataFolder(), "emergency");
            emergency = new YamlPlayerProfileRepository(folder.toPath(), ioExecutor);
            emergency.save(profile).join();
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Emergency profile backup failed for " + profile.uniqueId(), exception);
        } finally {
            if (emergency != null) {
                try { emergency.close(); } catch (Exception ignored) { }
            }
        }
    }

    private <T> CompletableFuture<T> enqueue(UUID uuid, Supplier<CompletableFuture<T>> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        ioChains.compute(uuid, (id, previous) -> {
            CompletableFuture<Void> base = previous == null
                    ? CompletableFuture.completedFuture(null)
                    : previous.exceptionally(ignored -> null);
            CompletableFuture<Void> next = base.thenComposeAsync(ignored -> task.get()
                    .whenComplete((value, failure) -> {
                        if (failure == null) result.complete(value);
                        else result.completeExceptionally(failure);
                    }).thenApply(ignoredValue -> null), ioExecutor);
            next.whenComplete((ignored, failure) -> ioChains.remove(uuid, next));
            return next;
        });
        return result;
    }

    @Override
    public boolean isRegistered(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null && profile.registered();
    }

    @Override
    public int getLevel(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile == null ? Level.MIN_LEVEL : profile.level();
    }

    @Override
    public long getExperience(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile == null ? 0L : profile.experience();
    }

    @Override
    public void addExperience(UUID uuid, long amount) {
        if (amount <= 0L) return;
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null || !profile.registered()) return;

        int before = profile.level();
        profile.addExperience(amount);
        int after = profile.level();
        persistAsync(profile);

        if (before != after) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Bukkit.getPluginManager().callEvent(
                        new PlayerLevelUpEvent(player, before, after));
            }
        }
    }

    @Override
    public double getBalance(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile == null ? 0.0D : profile.money();
    }

    @Override
    public void deposit(UUID uuid, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) return;
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null || !profile.registered()) return;
        long minor = de.pixelrpg.rpg.economy.Money.fromMajor(amount);
        if (!profile.depositMinorUnits(minor)) {
            throw new IllegalArgumentException("Balance overflow.");
        }
        persistAsync(profile);
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) return false;
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null || !profile.registered()) return false;
        long minor = de.pixelrpg.rpg.economy.Money.fromMajor(amount);
        boolean success = profile.withdrawMinorUnits(minor);
        if (success) persistAsync(profile);
        return success;
    }

    @Override
    public void close() {
        shutdown();
    }

    public void shutdown() {
        if (shuttingDown) return;
        shuttingDown = true;

        CompletableFuture<?>[] saves = activeProfiles.values().stream()
                .map(this::captureAndEnqueue)
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(saves).get(30, TimeUnit.SECONDS);
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Not all player profiles were flushed cleanly on shutdown.", exception);
        }

        closeStorage();
        activeProfiles.clear();
        loadingProfiles.clear();
        ioChains.clear();
        pendingSaves.clear();
        Bukkit.getServicesManager().unregister(EconomyAPI.class, this);
        Bukkit.getServicesManager().unregister(GuildAPI.class, this);
    }

    private void closeStorage() {
        if (repository != null) {
            try { repository.close(); } catch (Exception ignored) { }
            repository = null;
        }
        if (database != null) {
            database.close();
            database = null;
        }
        if (ioExecutor != null) {
            ioExecutor.close();
            ioExecutor = null;
        }
    }

    public enum LoadOutcome {
        SUCCESS,
        FAILED
    }
}
