package de.pixelrpg.rpg.companion;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persists equipment independently from companion progression with per-player write coalescing. */
public final class CompanionEquipmentStore implements AutoCloseable {
    private final File folder;
    private final Logger logger;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PixelRPG-CompanionEquipmentIO");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, Map<String, CompanionEquipment>> pending = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> scheduledPlayers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    public CompanionEquipmentStore(File pluginDataFolder, Logger logger) {
        this.folder = new File(pluginDataFolder, "companions/equipment");
        this.logger = logger;
        if (!folder.exists() && !folder.mkdirs()) logger.warning("Unable to create companion equipment storage folder: " + folder);
    }

    /** Loads all persisted equipment for one player in a single YAML read during companion state initialization. */
    public Map<String, CompanionEquipment> loadPlayer(UUID playerId) {
        YamlConfiguration yaml = loadFile(playerId);
        ConfigurationSection companions = yaml.getConfigurationSection("companions");
        if (companions == null) return Map.of();

        Map<String, CompanionEquipment> result = new HashMap<>();
        for (String companionId : companions.getKeys(false)) {
            ConfigurationSection section = companions.getConfigurationSection(companionId);
            if (section == null || !section.getBoolean("initialized", false)) continue;
            result.put(companionId, loadEquipment(section));
        }
        return result;
    }

    /** Queues the latest immutable snapshot and coalesces repeated writes for the same player. */
    public void save(UUID playerId, String companionId, CompanionEquipment equipment) {
        if (closed.get() || playerId == null || companionId == null || companionId.isBlank()) return;
        CompanionEquipment snapshot = equipment == null ? CompanionEquipment.empty() : equipment.copy();
        pending.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(companionId, snapshot);
        schedule(playerId);
    }

    private void schedule(UUID playerId) {
        if (!scheduledPlayers.add(playerId)) return;
        try {
            ioExecutor.execute(() -> drainPlayer(playerId));
        } catch (RuntimeException exception) {
            scheduledPlayers.remove(playerId);
            logger.log(Level.SEVERE, "Unable to schedule companion equipment write", exception);
        }
    }

    private void drainPlayer(UUID playerId) {
        try {
            while (true) {
                Map<String, CompanionEquipment> updates = pending.remove(playerId);
                if (updates == null || updates.isEmpty()) return;
                saveBlocking(playerId, updates);
            }
        } finally {
            scheduledPlayers.remove(playerId);
            if (!closed.get() && pending.containsKey(playerId)) schedule(playerId);
        }
    }

    private void saveBlocking(UUID playerId, Map<String, CompanionEquipment> updates) {
        YamlConfiguration yaml = loadFile(playerId);
        for (Map.Entry<String, CompanionEquipment> entry : updates.entrySet()) {
            String path = "companions." + entry.getKey();
            CompanionEquipment equipment = entry.getValue();
            yaml.set(path + ".initialized", true);
            set(yaml, path + ".helmet", equipment.helmet());
            set(yaml, path + ".chestplate", equipment.chestplate());
            set(yaml, path + ".leggings", equipment.leggings());
            set(yaml, path + ".boots", equipment.boots());
            set(yaml, path + ".main-hand", equipment.mainHand());
            set(yaml, path + ".off-hand", equipment.offHand());
        }
        Path target = file(playerId).toPath();
        Path temporary = null;
        try {
            temporary = Files.createTempFile(folder.toPath(), playerId + ".", ".tmp");
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Unable to save companion equipment for " + playerId, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupException) {
                    logger.log(Level.WARNING, "Unable to remove temporary companion equipment file " + temporary, cleanupException);
                }
            }
        }
    }

    private YamlConfiguration loadFile(UUID playerId) {
        File file = file(playerId);
        return file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    private File file(UUID playerId) {
        return new File(folder, playerId + ".yml");
    }

    private static CompanionEquipment loadEquipment(ConfigurationSection section) {
        return new CompanionEquipment(
                get(section, "helmet"),
                get(section, "chestplate"),
                get(section, "leggings"),
                get(section, "boots"),
                get(section, "main-hand"),
                get(section, "off-hand")
        );
    }

    private static ItemStack get(ConfigurationSection section, String path) {
        ItemStack item = section.getItemStack(path);
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    private static void set(YamlConfiguration yaml, String path, ItemStack item) {
        yaml.set(path, item == null || item.getType().isAir() ? null : item.clone());
    }

    /** Flushes queued equipment writes and terminates the dedicated I/O executor. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (UUID playerId : pending.keySet()) {
            if (scheduledPlayers.add(playerId)) {
                try {
                    ioExecutor.execute(() -> drainPlayer(playerId));
                } catch (RuntimeException exception) {
                    scheduledPlayers.remove(playerId);
                    logger.log(Level.SEVERE, "Unable to schedule final companion equipment write", exception);
                }
            }
        }
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Companion equipment I/O did not finish within 10 seconds; forcing shutdown.");
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            ioExecutor.shutdownNow();
        }
        pending.clear();
        scheduledPlayers.clear();
    }
}
