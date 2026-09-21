package de.pixelrpg.rpg.lore;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.pixelrpg.rpg.dialogue.PlayerKnowledgeStore;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class LoreRegistry {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new Gson();
    private final Map<String, LoreEntry> entries = new ConcurrentHashMap<>();

    public LoreRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lore.json");
    }

    public void load() {
        entries.clear();
        if (!file.exists()) copyDefault();
        if (!file.exists()) return;
        Type type = new TypeToken<List<LoreEntry>>() { }.getType();
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            List<LoreEntry> loaded = gson.fromJson(reader, type);
            if (loaded != null) loaded.forEach(entry -> entries.put(entry.id(), entry));
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load lore.json", exception);
        }
    }

    public Optional<LoreEntry> get(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    public List<LoreEntry> all() {
        return List.copyOf(entries.values());
    }

    public boolean canDiscover(PlayerKnowledgeStore knowledge, UUID playerId, String id) {
        LoreEntry entry = entries.get(id);
        if (entry == null) return false;
        return entry.prerequisites().stream().allMatch(prerequisite ->
                knowledge.knows(playerId, prerequisite));
    }

    public boolean discover(PlayerKnowledgeStore knowledge, UUID playerId, String id) {
        if (!canDiscover(knowledge, playerId, id)) return false;
        return knowledge.learn(playerId, id);
    }

    private void copyDefault() {
        try {
            plugin.saveResource("data/lore.json", false);
            File source = new File(plugin.getDataFolder(), "data/lore.json");
            java.nio.file.Files.copy(source.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to create default lore.json", exception);
        }
    }

    public void shutdown() { }
}
