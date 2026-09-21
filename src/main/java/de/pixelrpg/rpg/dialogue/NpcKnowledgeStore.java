package de.pixelrpg.rpg.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public final class NpcKnowledgeStore {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Set<String>> knowledgeByNpc = new ConcurrentHashMap<>();

    public NpcKnowledgeStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npc-knowledge.json");
    }

    public void load() {
        knowledgeByNpc.clear();
        if (!file.exists()) return;
        Type type = new TypeToken<Map<String, Set<String>>>() { }.getType();
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<String, Set<String>> loaded = gson.fromJson(reader, type);
            if (loaded != null) loaded.forEach((id, values) ->
                    knowledgeByNpc.put(id, ConcurrentHashMap.newKeySet()));
            if (loaded != null) loaded.forEach((id, values) -> knowledgeByNpc.get(id).addAll(values));
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load npc-knowledge.json", exception);
        }
    }

    public boolean knows(String npcId, String entryId) {
        return knowledgeByNpc.getOrDefault(npcId, Set.of()).contains(entryId);
    }

    public boolean learn(String npcId, String entryId) {
        if (npcId == null || npcId.isBlank() || entryId == null || entryId.isBlank()) return false;
        boolean changed = knowledgeByNpc.computeIfAbsent(npcId, ignored -> ConcurrentHashMap.newKeySet()).add(entryId);
        if (changed) save();
        return changed;
    }

    public Set<String> get(String npcId) {
        return Set.copyOf(knowledgeByNpc.getOrDefault(npcId, Set.of()));
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) { gson.toJson(knowledgeByNpc, writer); }
        catch (IOException exception) { plugin.getLogger().log(Level.WARNING, "Failed to save npc-knowledge.json", exception); }
    }

    public void shutdown() { save(); }
}
