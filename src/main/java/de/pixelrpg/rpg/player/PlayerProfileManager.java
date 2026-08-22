package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.events.PlayerClassChangeEvent;
import de.pixelrpg.rpg.api.events.PlayerJoinGuildEvent;
import de.pixelrpg.rpg.api.events.PlayerLeaveGuildEvent;
import de.pixelrpg.rpg.api.events.PlayerLevelUpEvent;
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
    private double respecCost = 300.0;
    private int respecMinLevel = 40;

    public PlayerProfileManager(Plugin plugin) { this.plugin = plugin; }

    public void initialize(FileConfiguration config) {
        storageType = StorageType.fromString(config.getString("storage.type", "YAML"));
        respecCost = config.getDouble("classes.respec-cost", 300.0);
        respecMinLevel = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, config.getInt("classes.respec-min-level", 40)));
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
            } else {
                repository = new YamlPlayerProfileRepository(plugin.getDataFolder());
            }
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
                .map(profile -> enqueueVoid(profile.getUuid(), () -> persistSync(profile)))
                .toList();
        try {
            CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Not all player profiles could be flushed cleanly on shutdown.", e);
        }
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
        if (repository != null) repository.shutdown();
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

    public void registerToGuild(Player player) {
        PlayerProfile profile = activeProfiles.computeIfAbsent(player.getUniqueId(), PlayerProfile::new);
        if (profile.isRegisteredInGuild()) return;
        profile.setRegisteredInGuild(true);
        persistAsync(profile);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        Bukkit.getPluginManager().callEvent(new PlayerJoinGuildEvent(player));
    }

    public void leaveGuild(Player player) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.isRegisteredInGuild()) return;
        profile.resetProgress();
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerLeaveGuildEvent(player));
    }

    public boolean selectClass(Player player, PlayerClass newClass) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.isRegisteredInGuild() || profile.getPlayerClass() != PlayerClass.NONE) return false;
        if (profile.getLevel() < 30 || newClass == PlayerClass.NONE) return false;
        PlayerClass oldClass = profile.getPlayerClass();
        profile.setPlayerClass(newClass);
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerClassChangeEvent(player, oldClass, newClass));
        return true;
    }

    public enum RespecResult { SUCCESS, NOT_REGISTERED, LEVEL_TOO_LOW, INSUFFICIENT_FUNDS, SAME_CLASS }

    public RespecResult respecClass(Player player, PlayerClass newClass) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.isRegisteredInGuild()) return RespecResult.NOT_REGISTERED;
        if (profile.getPlayerClass() == newClass) return RespecResult.SAME_CLASS;
        if (profile.getLevel() < respecMinLevel) return RespecResult.LEVEL_TOO_LOW;
        if (!profile.removeMoney(respecCost)) return RespecResult.INSUFFICIENT_FUNDS;
        PlayerClass oldClass = profile.getPlayerClass();
        profile.setPlayerClass(newClass);
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerClassChangeEvent(player, oldClass, newClass));
        return RespecResult.SUCCESS;
    }

    public double getRespecCost() { return respecCost; }
    public int getRespecMinLevel() { return respecMinLevel; }

    public AttributePurchaseResult purchaseAttribute(Player player, PlayerAttribute attribute) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.isRegisteredInGuild()) return AttributePurchaseResult.NOT_REGISTERED;
        int current = profile.getAttributePoints(attribute);
        if (current >= attribute.getMaxPoints()) return AttributePurchaseResult.MAX_REACHED;
        if (profile.getLevel() < AttributeConfig.levelRequirementForPoint(attribute, current)) return AttributePurchaseResult.LEVEL_TOO_LOW;
        double cost = AttributeConfig.costFor(attribute, current, profile.getPlayerClass());
        if (!profile.removeMoney(cost)) return AttributePurchaseResult.INSUFFICIENT_FUNDS;
        profile.addAttributePoint(attribute);
        persistAsync(profile);
        return AttributePurchaseResult.SUCCESS;
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

    private void persistSync(PlayerProfile profile) {
        if (!profile.beginSave()) return;
        try {
            repository.save(profile);
        } catch (Exception e) {
            profile.markDirty();
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save profile for " + profile.getUuid(), e);
            if (storageType == StorageType.MYSQL) writeEmergencyBackup(profile);
        }
    }

    private void writeEmergencyBackup(PlayerProfile profile) {
        try {
            File folder = new File(plugin.getDataFolder(), "emergency");
            YamlPlayerProfileRepository emergency = new YamlPlayerProfileRepository(folder);
            emergency.init();
            emergency.save(profile);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Emergency backup failed for " + profile.getUuid(), e);
        }
    }

    private <T> CompletableFuture<T> enqueue(UUID uuid, Supplier<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFuture<Void> chain = saveChain.compute(uuid, (id, previous) -> {
            CompletableFuture<Void> base = previous != null ? previous : CompletableFuture.completedFuture(null);
            return base.exceptionally(ignored -> null).thenRunAsync(() -> {
                try {
                    result.complete(task.get());
                } catch (Throwable throwable) {
                    result.completeExceptionally(throwable);
                }
            }, saveExecutor);
        });
        chain.whenComplete((ignored, throwable) -> saveChain.remove(uuid, chain));
        return result;
    }

    private CompletableFuture<Void> enqueueVoid(UUID uuid, Runnable task) { return enqueue(uuid, () -> { task.run(); return null; }); }

    @Override public boolean isRegistered(UUID uuid) { PlayerProfile p = activeProfiles.get(uuid); return p != null && p.isRegisteredInGuild(); }
    @Override public int getLevel(UUID uuid) { PlayerProfile p = activeProfiles.get(uuid); return p != null ? p.getLevel() : Level.MIN_LEVEL; }
    @Override public PlayerClass getPlayerClass(UUID uuid) { PlayerProfile p = activeProfiles.get(uuid); return p != null ? p.getPlayerClass() : PlayerClass.NONE; }
    @Override public boolean hasSelectedClass(UUID uuid) { return getPlayerClass(uuid) != PlayerClass.NONE; }
    @Override public long getExperience(UUID uuid) { PlayerProfile p = activeProfiles.get(uuid); return p != null ? p.getExperience() : 0L; }
    @Override public void addExperience(UUID uuid, long amount) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null || !profile.isRegisteredInGuild()) return;
        int before = profile.getLevel();
        profile.addExperience(amount);
        int after = profile.getLevel();
        persistAsync(profile);
        if (before != after) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) Bukkit.getPluginManager().callEvent(new PlayerLevelUpEvent(player, before, after));
        }
    }
    @Override public int getAttributePoints(UUID uuid, PlayerAttribute attribute) { PlayerProfile p = activeProfiles.get(uuid); return p != null ? p.getAttributePoints(attribute) : 0; }
    @Override public double getBalance(UUID uuid) { PlayerProfile p = activeProfiles.get(uuid); return p != null ? p.getMoney() : 0.0; }
    @Override public void deposit(UUID uuid, double amount) { PlayerProfile p = activeProfiles.get(uuid); if (p != null && p.isRegisteredInGuild()) { p.addMoney(amount); persistAsync(p); } }
    @Override public boolean withdraw(UUID uuid, double amount) { PlayerProfile p = activeProfiles.get(uuid); if (p == null || !p.isRegisteredInGuild()) return false; boolean success = p.removeMoney(amount); if (success) persistAsync(p); return success; }
    public enum AttributePurchaseResult { SUCCESS, NOT_REGISTERED, MAX_REACHED, LEVEL_TOO_LOW, INSUFFICIENT_FUNDS }
}
