package de.pixelrpg.rpg.dialogue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class WorldState {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new Gson();
    private final Set<String> flags = ConcurrentHashMap.newKeySet();

    public WorldState(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "world-state.json");
    }

    public void load() {
        flags.clear();
        if (!file.exists()) return;
        Type type = new TypeToken<Map<String, Boolean>>() { }.getType();
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<String, Boolean> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                loaded.forEach((id, enabled) -> {
                    if (Boolean.TRUE.equals(enabled)) flags.add(id);
                });
            }
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load world-state.json", exception);
        }
    }

    public boolean isSet(String id) {
        return flags.contains(id);
    }

    public void set(String id) {
        if (id != null && !id.isBlank() && flags.add(id)) save();
    }

    public void clear(String id) {
        if (id != null && flags.remove(id)) save();
    }

    public Set<String> flags() {
        return Set.copyOf(flags);
    }

    public void save() {
        Map<String, Boolean> snapshot = new java.util.HashMap<>();
        flags.forEach(flag -> snapshot.put(flag, true));
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(snapshot, writer);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save world-state.json", exception);
        }
    }

    public void shutdown() {
        save();
    }
}
