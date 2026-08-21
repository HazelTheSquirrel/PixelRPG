package de.pixelrpg.rpg.quest;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class GlobalEventState {

    private final Plugin plugin;
    private final File file;
    private final Gson gson = new Gson();
    private final Map<String, Integer> progressByQuestId = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();
    private BukkitTask pendingSave;

    public GlobalEventState(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "global_events.json");
    }

    public void load() {
        progressByQuestId.clear();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Type type = new com.google.gson.reflect.TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) progressByQuestId.putAll(loaded);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load global_events.json", e);
        }
    }

    public void save() {
        BukkitTask task = pendingSave;
        if (task != null) {
            task.cancel();
            pendingSave = null;
        }
        synchronized (saveLock) {
            writeSave();
        }
    }

    public void shutdown() { save(); }

    public int getProgress(String questId) { return progressByQuestId.getOrDefault(questId, 0); }

    public int addProgress(String questId, int amount) {
        int updated = progressByQuestId.merge(questId, amount, Integer::sum);
        scheduleSave();
        return updated;
    }

    public void resetProgress(String questId) {
        progressByQuestId.put(questId, 0);
        scheduleSave();
    }

    private void scheduleSave() {
        if (pendingSave != null && !pendingSave.isCancelled()) return;
        pendingSave = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            synchronized (saveLock) {
                pendingSave = null;
                writeSave();
            }
        }, 20L);
    }

    private void writeSave() {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(new HashMap<>(progressByQuestId), writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save global_events.json", e);
        }
    }
}