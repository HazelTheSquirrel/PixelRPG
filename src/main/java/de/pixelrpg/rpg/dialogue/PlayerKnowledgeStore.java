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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PlayerKnowledgeStore {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new Gson();
    private final Map<UUID, Set<String>> knowledgeByPlayer = new ConcurrentHashMap<>();

    public PlayerKnowledgeStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-knowledge.json");
    }

    public void load() {
        knowledgeByPlayer.clear();
        if (!file.exists()) return;
        Type type = new TypeToken<Map<UUID, Set<String>>>() { }.getType();
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<UUID, Set<String>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                loaded.forEach((uuid, knowledge) ->
                        knowledgeByPlayer.put(uuid, ConcurrentHashMap.newKeySet()));
                loaded.forEach((uuid, knowledge) -> knowledgeByPlayer.get(uuid).addAll(knowledge));
            }
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player-knowledge.json", exception);
        }
    }

    public boolean knows(UUID player, String entryId) {
        Set<String> entries = knowledgeByPlayer.get(player);
        return entries != null && entries.contains(entryId);
    }

    public boolean learn(UUID player, String entryId) {
        if (entryId == null || entryId.isBlank()) return false;
        boolean changed = knowledgeByPlayer
                .computeIfAbsent(player, ignored -> ConcurrentHashMap.newKeySet())
                .add(entryId);
        if (changed) save();
        return changed;
    }

    public Set<String> get(UUID player) {
        return Set.copyOf(knowledgeByPlayer.getOrDefault(player, Set.of()));
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(knowledgeByPlayer, writer);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player-knowledge.json", exception);
        }
    }

    public void shutdown() {
        save();
    }
}
