package de.pixelrpg.rpg.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import de.pixelrpg.rpg.npc.NpcFaction;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PlayerFactionRelationshipStore {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Integer> values = new ConcurrentHashMap<>();

    public PlayerFactionRelationshipStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-faction-relationships.json");
    }

    public void load() {
        values.clear();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Integer>>() { }.getType();
            Map<String, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                loaded.forEach((key, value) -> {
                    if (value instanceof Number number) values.put(key, number.intValue());
                });
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player-faction-relationships.json", exception);
        }
    }

    public int get(UUID playerId, NpcFaction faction) {
        if (playerId == null || faction == null) return 0;
        return values.getOrDefault(key(playerId, faction), 0);
    }

    public void adjust(UUID playerId, NpcFaction faction, int amount) {
        if (playerId == null || faction == null || faction == NpcFaction.NONE) return;
        values.merge(key(playerId, faction), amount, Integer::sum);
        save();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(values, writer);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player-faction-relationships.json", exception);
        }
    }

    public void shutdown() {
        save();
    }

    private String key(UUID playerId, NpcFaction faction) {
        return playerId + ":" + faction.name();
    }
}
