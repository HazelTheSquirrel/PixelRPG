package de.pixelrpg.rpg.core;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Coalesces file snapshots so gameplay threads never block on disk writes. */
public final class AsyncFileWriter {
    private final Plugin plugin;
    private final ExecutorService executor;
    private final Map<Path, String> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Object lifecycleLock = new Object();

    public AsyncFileWriter(Plugin plugin, String threadName) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Replaces the latest pending snapshot for a file and wakes the writer once. */
    public void submit(Path path, String contents) {
        if (path == null || contents == null) return;
        Path normalized = path.toAbsolutePath().normalize();
        synchronized (lifecycleLock) {
            if (closed.get()) return;
            pending.put(normalized, contents);
            scheduleDrain();
        }
    }

    private void scheduleDrain() {
        if (!draining.compareAndSet(false, true)) return;
        try {
            executor.execute(this::drain);
        } catch (RuntimeException exception) {
            draining.set(false);
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to schedule async file drain", exception);
        }
    }

    private void drain() {
        try {
            while (!pending.isEmpty()) {
                for (Map.Entry<Path, String> entry : pending.entrySet()) {
                    if (!pending.remove(entry.getKey(), entry.getValue())) continue;
                    writeAtomically(entry.getKey(), entry.getValue());
                }
            }
        } finally {
            draining.set(false);
            // A submit can race the final empty check. Re-check after publishing the idle state.
            if (!pending.isEmpty() && !closed.get()) scheduleDrain();
        }
    }

    private void writeAtomically(Path target, String contents) {
        Path parent = target.getParent();
        Path temporary = null;
        try {
            if (parent != null) Files.createDirectories(parent);
            Path tempDirectory = parent == null ? Path.of(".").toAbsolutePath().normalize() : parent;
            temporary = Files.createTempFile(tempDirectory, target.getFileName().toString() + ".", ".tmp");
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to write async file " + target, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupException) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "Unable to remove temporary file " + temporary, cleanupException);
                }
            }
        }
    }

    /** Stops the writer after flushing all snapshots already queued before shutdown. */
    public void shutdown() {
        synchronized (lifecycleLock) {
            if (!closed.compareAndSet(false, true)) return;
            if (!pending.isEmpty() && draining.compareAndSet(false, true)) {
                try {
                    executor.execute(this::drain);
                } catch (RuntimeException exception) {
                    draining.set(false);
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to schedule final async file drain", exception);
                }
            }
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        pending.clear();
    }
}
