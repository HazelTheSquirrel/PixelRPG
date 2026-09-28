package de.pixelrpg.rpg.dialogue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Persists per-player dialogue nodes that were seen or completed. */
public final class DialogueProgressStore {
    private final Plugin plugin;
    private final File file;
    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private final Set<String> completed = ConcurrentHashMap.newKeySet();

    public DialogueProgressStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dialogue-progress.yml");
    }

    public void load() {
        seen.clear();
        completed.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        seen.addAll(yaml.getStringList("seen"));
        completed.addAll(yaml.getStringList("completed"));
    }

    public boolean hasSeen(UUID player, String nodeId) {
        return seen.contains(key(player, nodeId));
    }

    public void markSeen(UUID player, String nodeId) {
        if (seen.add(key(player, nodeId))) save();
    }

    public boolean isCompleted(UUID player, String nodeId) {
        return completed.contains(key(player, nodeId));
    }

    public void markCompleted(UUID player, String nodeId) {
        if (completed.add(key(player, nodeId))) save();
    }

    public void shutdown() {
        save();
    }

    private String key(UUID player, String nodeId) {
        return player + ":" + nodeId;
    }

    private synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("seen", new ArrayList<>(seen));
        yaml.set("completed", new ArrayList<>(completed));
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not save dialogue progress.", exception);
        }
    }
}
