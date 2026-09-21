package de.pixelrpg.rpg.dialogue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public final class NpcRelationshipDefinitionStore {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new Gson();

    public NpcRelationshipDefinitionStore(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.file = new File(plugin.getDataFolder(), "npc-relationships.json");
    }

    public void loadInto(NpcNetworkRelationshipStore store) {
        copyDefault();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            List<Definition> definitions = gson.fromJson(reader,
                    new TypeToken<List<Definition>>() { }.getType());
            if (definitions == null) return;
            for (Definition definition : definitions) {
                store.set(definition.firstNpcId(), definition.secondNpcId(),
                        definition.relation(), definition.value());
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load npc-relationships.json", exception);
        }
    }

    private void copyDefault() {
        if (file.exists()) return;
        try {
            Files.createDirectories(file.getParentFile().toPath());
            plugin.saveResource("data/npc-relationships.json", false);
            File source = new File(plugin.getDataFolder(), "data/npc-relationships.json");
            if (source.exists()) Files.copy(source.toPath(), file.toPath());
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to create default npc-relationships.json", exception);
        }
    }

    public record Definition(String firstNpcId, String secondNpcId, String relation, int value) { }
}
