package de.pixelrpg.rpg.core;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Coalesces file snapshots so gameplay threads never block on disk writes. */
public final class AsyncFileWriter {
    private final Plugin plugin;
    private final ExecutorService executor;
    private final Map<Path, String> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean draining = new AtomicBoolean();

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
        if (path == null || contents == null || executor.isShutdown()) return;
        pending.put(path.toAbsolutePath().normalize(), contents);
        if (draining.compareAndSet(false, true)) executor.execute(this::drain);
    }

    private void drain() {
        try {
            while (!pending.isEmpty()) {
                for (Map.Entry<Path, String> entry : pending.entrySet()) {
                    if (!pending.remove(entry.getKey(), entry.getValue())) continue;
                    try {
                        Path parent = entry.getKey().getParent();
                        if (parent != null) Files.createDirectories(parent);
                        Files.writeString(entry.getKey(), entry.getValue(), StandardCharsets.UTF_8);
                    } catch (IOException exception) {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to write async file " + entry.getKey(), exception);
                    }
                }
            }
        } finally {
            draining.set(false);
            if (!pending.isEmpty() && draining.compareAndSet(false, true)) executor.execute(this::drain);
        }
    }

    /** Stops the writer after flushing all queued snapshots. */
    public void shutdown() {
        if (executor.isShutdown()) return;
        if (!pending.isEmpty() && draining.compareAndSet(false, true)) executor.execute(this::drain);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
