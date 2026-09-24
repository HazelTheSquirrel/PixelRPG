package de.pixelrpg.rpg.player;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.*;

public final class PlayerProfileManager implements AutoCloseable {
    public enum LoadOutcome { SUCCESS, FAILED }

    private final Plugin plugin;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private ExecutorService io;
    private PlayerProfileRepository repository;
    private de.pixelrpg.rpg.storage.DatabaseManager database;
    private int loadTimeoutSeconds;

    public PlayerProfileManager(Plugin plugin) { this.plugin = plugin; }

    public void initialize(FileConfiguration config) {
        io = Executors.newVirtualThreadPerTaskExecutor();
        loadTimeoutSeconds = Math.max(1, config.getInt("storage.load-timeout-seconds", 8));
        if ("MYSQL".equalsIgnoreCase(config.getString("storage.type", "YAML"))) {
            database = new de.pixelrpg.rpg.storage.DatabaseManager();
            database.connect(config);
            try {
                database.createSchema();
            } catch (Exception exception) {
                database.close();
                database = null;
                throw new IllegalStateException("Database initialization failed.", exception);
            }
            repository = new MySQLPlayerProfileRepository(database.dataSource(), io);
        } else {
            repository = new YamlPlayerProfileRepository(plugin.getDataFolder().toPath().resolve("players"), io);
        }
    }

    public LoadOutcome loadForPreLogin(UUID id) {
        try {
            PlayerProfile profile = repository.load(id).get(loadTimeoutSeconds, TimeUnit.SECONDS);
            profiles.put(id, profile);
            return LoadOutcome.SUCCESS;
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load profile for " + id, exception);
            return LoadOutcome.FAILED;
        }
    }

    public void activateOnJoin(UUID id) {
        profiles.computeIfAbsent(id, PlayerProfile::new);
    }

    public PlayerProfile get(UUID id) { return profiles.get(id); }

    public CompletableFuture<Void> save(UUID id) {
        PlayerProfile profile = profiles.get(id);
        if (profile == null || repository == null) return CompletableFuture.completedFuture(null);
        profile.revision(profile.revision() + 1);
        return repository.save(profile);
    }

    public CompletableFuture<Void> saveAll() {
        return CompletableFuture.allOf(profiles.keySet().stream().map(this::save).toArray(CompletableFuture[]::new));
    }

    public void deactivateOnQuit(UUID id) {
        PlayerProfile profile = profiles.remove(id);
        if (profile != null && repository != null) {
            profile.revision(profile.revision() + 1);
            repository.save(profile).exceptionally(error -> {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save profile for " + id, error);
                return null;
            });
        }
    }

    @Override
    public void close() {
        try {
            saveAll().join();
        } finally {
            if (repository != null) repository.close();
            if (database != null) database.close();
            if (io != null) io.close();
            profiles.clear();
        }
    }
}
