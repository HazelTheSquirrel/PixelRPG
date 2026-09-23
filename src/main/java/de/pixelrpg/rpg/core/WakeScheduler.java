package de.pixelrpg.rpg.core;

import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Coalesces event-driven work into one-shot main-thread executions; no permanent polling task is required. */
public final class WakeScheduler<K> {
    private final Plugin plugin;
    private final Map<K, BukkitTask> scheduled = new ConcurrentHashMap<>();

    public WakeScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** Wakes a key once on the next server tick; repeated wake-ups before execution are coalesced. */
    public void wake(K key, Runnable action) {
        if (key == null || action == null || !plugin.isEnabled()) return;
        scheduled.computeIfAbsent(key, ignored -> scheduleNextTick(key, action));
    }

    /** Wakes a key after the supplied delay; repeated wake-ups remain coalesced. */
    public void wakeLater(K key, long delayTicks, Runnable action) {
        if (key == null || action == null || !plugin.isEnabled()) return;
        scheduled.computeIfAbsent(key, ignored -> scheduleLater(key, delayTicks, action));
    }

    private BukkitTask scheduleNextTick(K key, Runnable action) {
        try {
            return plugin.getServer().getScheduler().runTask(plugin, () -> {
                scheduled.remove(key);
                action.run();
            });
        } catch (IllegalPluginAccessException exception) {
            scheduled.remove(key);
            return null;
        }
    }

    private BukkitTask scheduleLater(K key, long delayTicks, Runnable action) {
        try {
            return plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                scheduled.remove(key);
                action.run();
            }, Math.max(1L, delayTicks));
        } catch (IllegalPluginAccessException exception) {
            scheduled.remove(key);
            return null;
        }
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
