package de.pixelrpg.rpg.npc;

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
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class NpcProfileStore {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, NpcProfile> profiles = new ConcurrentHashMap<>();

    public NpcProfileStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npc-profiles.json");
    }

    public void load() {
        profiles.clear();
        copyDefaultIfMissing();
        if (!file.exists()) return;
        Type type = new TypeToken<Map<String, NpcProfile>>() { }.getType();
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<String, NpcProfile> loaded = gson.fromJson(reader, type);
            if (loaded != null) profiles.putAll(loaded);
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load npc-profiles.json", exception);
        }
    }

    public void synchronize(Collection<RPGNpc> npcs) {
        boolean changed = false;
        for (RPGNpc npc : npcs) {
            if (profiles.putIfAbsent(npc.id(), NpcProfile.resident(npc)) == null) changed = true;
        }
        if (changed) save();
    }

    public Optional<NpcProfile> get(String npcId) {
        return Optional.ofNullable(profiles.get(npcId));
    }

    public NpcProfile getOrCreate(RPGNpc npc) {
        NpcProfile existing = profiles.get(npc.id());
        if (existing != null) return existing;
        NpcProfile created = NpcProfile.resident(npc);
        NpcProfile previous = profiles.putIfAbsent(npc.id(), created);
        if (previous != null) return previous;
        save();
        return created;
    }

    public void put(NpcProfile profile) {
        profiles.put(profile.npcId(), profile);
        save();
    }

    public Collection<NpcProfile> all() {
        return List.copyOf(profiles.values());
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(profiles, writer);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save npc-profiles.json", exception);
        }
    }

    public void shutdown() {
        save();
    }

    private void copyDefaultIfMissing() {
        if (file.exists()) return;
        try {
            File parent = file.getParentFile();
            if (parent != null) Files.createDirectories(parent.toPath());
            plugin.saveResource("data/npc-profiles.json", false);
            File source = new File(plugin.getDataFolder(), "data/npc-profiles.json");
            if (source.exists()) Files.copy(source.toPath(), file.toPath());
        } catch (IOException | IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to create default npc-profiles.json", exception);
        }
    }
}
