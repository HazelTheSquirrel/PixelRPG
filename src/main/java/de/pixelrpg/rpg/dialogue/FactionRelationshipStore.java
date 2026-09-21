package de.pixelrpg.rpg.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.pixelrpg.rpg.npc.NpcFaction;
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

public final class FactionRelationshipStore {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Integer> values = new ConcurrentHashMap<>();

    public FactionRelationshipStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "faction-relationships.json");
    }

    public void load() {
        values.clear();
        if (!file.exists()) return;
        Type type = new TypeToken<Map<String, Integer>>() { }.getType();
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<String, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) values.putAll(loaded);
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load faction-relationships.json", exception);
        }
    }

    public int get(NpcFaction first, NpcFaction second) {
        return values.getOrDefault(key(first, second), 0);
    }

    public void adjust(NpcFaction first, NpcFaction second, int amount) {
        if (first == null || second == null || first == NpcFaction.NONE || second == NpcFaction.NONE) return;
        values.merge(key(first, second), amount, Integer::sum);
        values.merge(key(second, first), amount, Integer::sum);
        save();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(values, writer);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save faction-relationships.json", exception);
        }
    }

    public void shutdown() {
        save();
    }

    private String key(NpcFaction first, NpcFaction second) {
        return first.name() + ":" + second.name();
    }
}
