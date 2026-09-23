package de.pixelrpg.rpg.dialogue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class DialogueProgressStore implements AutoCloseable {
    private final Plugin plugin;
    private final File file;
    private final ExecutorService ioExecutor;
    private final Set<String> seen = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<String> completed = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();
    private CompletableFuture<Void> saveChain = CompletableFuture.completedFuture(null);

    public DialogueProgressStore(Plugin plugin, ExecutorService ioExecutor) {
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
        this.file = new File(plugin.getDataFolder(), "dialogue-progress.yml");
        this.ioExecutor = java.util.Objects.requireNonNull(ioExecutor, "ioExecutor");
    }

    public CompletableFuture<Void> loadAsync() {
        if (closed.get()) return CompletableFuture.failedFuture(new IllegalStateException("Dialogue progress store is closed"));
        return CompletableFuture.runAsync(this::loadSnapshot, ioExecutor);
    }

    public boolean hasSeen(UUID player, String nodeId) {
        return seen.contains(key(player, nodeId));
    }

    public boolean hasCompleted(UUID player, String nodeId) {
        return completed.contains(key(player, nodeId));
    }

    public void markSeen(UUID player, String nodeId) {
        if (seen.add(key(player, nodeId))) enqueueSave();
    }

    public void markCompleted(UUID player, String nodeId) {
        String key = key(player, nodeId);
        seen.add(key);
        if (completed.add(key)) enqueueSave();
    }

    public void clear(UUID player) {
        String prefix = player + ":";
        seen.removeIf(value -> value.startsWith(prefix));
        completed.removeIf(value -> value.startsWith(prefix));
        enqueueSave();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            saveChain.get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to flush dialogue progress.", exception);
        }
    }

    private synchronized void enqueueSave() {
        if (closed.get()) return;
        Set<String> seenSnapshot = Set.copyOf(seen);
        Set<String> completedSnapshot = Set.copyOf(completed);
        saveChain = saveChain.handle((ignored, failure) -> null)
                .thenRunAsync(() -> writeSnapshot(seenSnapshot, completedSnapshot), ioExecutor);
    }

    private void loadSnapshot() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        seen.clear();
        completed.clear();
        seen.addAll(yaml.getStringList("seen"));
        completed.addAll(yaml.getStringList("completed"));
    }

    private void writeSnapshot(Set<String> seenSnapshot, Set<String> completedSnapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("seen", new ArrayList<>(seenSnapshot));
        yaml.set("completed", new ArrayList<>(completedSnapshot));

        Path target = file.toPath();
        Path temporary = target.resolveSibling(file.getName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save dialogue progress.", exception);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // Best-effort cleanup only.
            }
        }
    }

    private String key(UUID player, String nodeId) {
        return player + ":" + nodeId;
    }
}
