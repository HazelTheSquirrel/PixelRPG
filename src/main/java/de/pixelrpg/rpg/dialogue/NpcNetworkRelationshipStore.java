package de.pixelrpg.rpg.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class NpcNetworkRelationshipStore {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Integer> values = new ConcurrentHashMap<>();

    public NpcNetworkRelationshipStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npc-network-relationships.json");
    }

    public void load() {
        values.clear();
        if (!file.exists()) return;
        Type type = new TypeToken<Map<String, Integer>>() { }.getType();
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<String, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) values.putAll(loaded);
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load npc-network-relationships.json", exception);
        }
    }

    public int get(String firstNpcId, String secondNpcId, String relation) {
        return values.getOrDefault(key(firstNpcId, secondNpcId, relation), 0);
    }

    public void set(String firstNpcId, String secondNpcId, String relation, int value) {
        if (firstNpcId == null || secondNpcId == null || relation == null || relation.isBlank()) return;
        values.put(key(firstNpcId, secondNpcId, relation), value);
        save();
    }

    public void adjust(String firstNpcId, String secondNpcId, String relation, int amount) {
        if (firstNpcId == null || secondNpcId == null || relation == null || relation.isBlank()) return;
        values.merge(key(firstNpcId, secondNpcId, relation), amount, Integer::sum);
        save();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(values, writer);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save npc-network-relationships.json", exception);
        }
    }

    public void shutdown() {
        save();
    }

    private String key(String firstNpcId, String secondNpcId, String relation) {
        return firstNpcId + ":" + secondNpcId + ":" + relation;
    }
}
