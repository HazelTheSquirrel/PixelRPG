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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class NpcRelationshipStore {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Integer> values = new ConcurrentHashMap<>();

    public NpcRelationshipStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npc-relationships.json");
    }

    public void load() {
        values.clear();
        if (!file.exists()) return;
        Type type = new TypeToken<Map<String, Integer>>() { }.getType();
        try (FileReader reader = new FileReader(file)) {
            Map<String, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) values.putAll(loaded);
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load npc-relationships.json", exception);
        }
    }

    public int get(UUID playerId, String npcId, String relation) {
        return values.getOrDefault(key(playerId, npcId, relation), 0);
    }

    public void adjust(UUID playerId, String npcId, String relation, int amount) {
        values.merge(key(playerId, npcId, relation), amount, Integer::sum);
        save();
    }

    public void rememberMeeting(UUID playerId, String npcId) {
        adjust(playerId, npcId, "met", 1);
    }

    public boolean hasMet(UUID playerId, String npcId) {
        return get(playerId, npcId, "met") > 0;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file)) { gson.toJson(values, writer); }
        catch (IOException exception) { plugin.getLogger().log(Level.WARNING, "Failed to save npc-relationships.json", exception); }
    }

    public void shutdown() { save(); }

    private String key(UUID playerId, String npcId, String relation) {
        return playerId + ":" + npcId + ":" + relation;
    }
}
