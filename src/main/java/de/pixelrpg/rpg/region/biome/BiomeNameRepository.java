// src/main/java/de/pixelrpg/rpg/region/biome/BiomeNameRepository.java
package de.pixelrpg.rpg.region.biome;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class BiomeNameRepository {

    private final Plugin plugin;
    private final File file;
    private final Map<String, String> clusterIdToName = new ConcurrentHashMap<>();

    public BiomeNameRepository(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "biome_region_names.yml");
    }

    public void load() {
        clusterIdToName.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("names");
        if (section == null) {
            return;
        }
        for (String clusterId : section.getKeys(false)) {
            clusterIdToName.put(clusterId, section.getString(clusterId));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (var entry : clusterIdToName.entrySet()) {
            yaml.set("names." + entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save biome_region_names.yml", e);
        }
    }

    public String getOrGenerate(String clusterId, String biomeKey, int ordinal) {
        String existing = clusterIdToName.get(clusterId);
        if (existing != null) {
            return existing;
        }

        String pretty = prettifyBiomeKey(biomeKey);
        String generated = pretty + " " + ordinal;
        clusterIdToName.put(clusterId, generated);
        save();
        return generated;
    }

    private String prettifyBiomeKey(String biomeKey) {
        String simple = biomeKey.contains(":") ? biomeKey.substring(biomeKey.indexOf(':') + 1) : biomeKey;
        String[] words = simple.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}