package de.pixelrpg.rpg.core;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Coalesces event-driven work into one-shot main-thread executions; no permanent polling task is required. */
public final class WakeScheduler<K> {
    private final Plugin plugin;
    private final Map<K, BukkitTask> scheduled = new HashMap<>();

    public WakeScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** Wakes a key once on the next server tick; repeated wake-ups before execution are coalesced. */
    public void wake(K key, Runnable action) {
        if (key == null || action == null || scheduled.containsKey(key)) return;
        BukkitTask task = plugin.getServer().getScheduler().runTask(plugin, () -> {
            scheduled.remove(key);
            action.run();
        });
        scheduled.put(key, task);
    }

    /** Wakes a key after the supplied delay; repeated wake-ups remain coalesced. */
    public void wakeLater(K key, long delayTicks, Runnable action) {
        if (key == null || action == null || scheduled.containsKey(key)) return;
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            scheduled.remove(key);
            action.run();
        }, Math.max(1L, delayTicks));
        scheduled.put(key, task);
    }

    /** Cancels a pending wake-up without executing it. */
    public void cancel(K key) {
        BukkitTask task = scheduled.remove(key);
        if (task != null) task.cancel();
    }

    /** Cancels all pending wake-ups. */
    public void clear() {
        scheduled.values().forEach(BukkitTask::cancel);
        scheduled.clear();
    }
}
