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

public final class AsyncFileWriter {
    private final Plugin plugin;
    private final ExecutorService executor;
    private final Map<Path, String> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Object lock = new Object();

    public AsyncFileWriter(Plugin plugin, String threadName) {
        this.plugin = Objects.requireNonNull(plugin);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void submit(Path path, String contents) {
        if (path == null || contents == null) return;
        synchronized (lock) {
            if (closed.get()) return;
            pending.put(path.toAbsolutePath().normalize(), contents);
            schedule();
        }
    }

    private void schedule() {
        if (!draining.compareAndSet(false, true)) return;
        try { executor.execute(this::drain); }
        catch (RuntimeException exception) {
            draining.set(false);
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to schedule async file write.", exception);
        }
    }

    private void drain() {
        try {
            while (!pending.isEmpty()) {
                for (Map.Entry<Path, String> entry : pending.entrySet()) {
                    if (!pending.remove(entry.getKey(), entry.getValue())) continue;
                    write(entry.getKey(), entry.getValue());
                }
            }
        } finally {
            draining.set(false);
            if (!pending.isEmpty() && !closed.get()) schedule();
        }
    }

    private void write(Path target, String contents) {
        Path parent = target.getParent();
        Path temporary = null;
        try {
            if (parent != null) Files.createDirectories(parent);
            Path directory = parent == null ? Path.of(".").toAbsolutePath().normalize() : parent;
            temporary = Files.createTempFile(directory, target.getFileName().toString() + ".", ".tmp");
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicFailure) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to write async file " + target, exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException exception) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "Unable to clean temporary file " + temporary, exception);
                }
            }
        }
    }

    public void shutdown() {
        synchronized (lock) {
            if (!closed.compareAndSet(false, true)) return;
            if (!pending.isEmpty() && draining.compareAndSet(false, true)) executor.execute(this::drain);
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
