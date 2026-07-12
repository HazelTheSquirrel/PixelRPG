// src/main/java/de/pixelrpg/rpg/quest/GlobalEventState.java (VOLLSTÄNDIG, ersetzt alte Datei — internes System, jetzt JSON statt YAML, wie in Phase 3 gefordert)
package de.pixelrpg.rpg.quest;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public final class GlobalEventState {

    private final Plugin plugin;
    private final File file;
    private final Gson gson = new Gson();
    private final Map<String, Integer> progressByQuestId = new ConcurrentHashMap<>();

    public GlobalEventState(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "global_events.json");
    }

    public void load() {
        progressByQuestId.clear();
        if (!file.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            Type type = new com.google.gson.reflect.TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                progressByQuestId.putAll(loaded);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load global_events.json", e);
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(new HashMap<>(progressByQuestId), writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save global_events.json", e);
        }
    }

    public int getProgress(String questId) {
        return progressByQuestId.getOrDefault(questId, 0);
    }

    public int addProgress(String questId, int amount) {
        int updated = progressByQuestId.merge(questId, amount, Integer::sum);
        save();
        return updated;
    }

    public void resetProgress(String questId) {
        progressByQuestId.put(questId, 0);
        save();
    }
}