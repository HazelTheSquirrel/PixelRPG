// src/main/java/de/pixelrpg/rpg/dungeon/DungeonRepository.java
package de.pixelrpg.rpg.dungeon;

import de.pixelrpg.rpg.core.Rank;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class DungeonRepository {

    private final Plugin plugin;
    private final File file;
    private final Map<String, DungeonDefinition> definitionsById = new ConcurrentHashMap<>();

    public DungeonRepository(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dungeons.yml");
    }

    public void load() {
        definitionsById.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("dungeons");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            DungeonDefinition definition = new DungeonDefinition(id, section.getString("name", id));
            definition.setMinRank(parseRank(section.getString("min-rank", "F")));
            definition.setMaxRank(parseRank(section.getString("max-rank", "S")));
            definition.setInstanceMode(parseMode(section.getString("instance-mode", "INSTANCED")));
            definition.setCooldownMinutes(section.getInt("cooldown-minutes", 30));
            definition.setOrigin(
                    section.getInt("origin.x", 0),
                    section.getInt("origin.y", 0),
                    section.getInt("origin.z", 0));
            definition.setCaptureWorld(section.getString("capture-world"));
            definition.setDimensions(
                    section.getInt("dimensions.width", 0),
                    section.getInt("dimensions.height", 0),
                    section.getInt("dimensions.length", 0));
            definition.setSchematicFile(section.getString("schematic-file"));

            List<Map<?, ?>> markerMaps = section.getMapList("markers");
            for (Map<?, ?> markerMap : markerMaps) {
                DungeonMarkerType type = DungeonMarkerType.valueOf(String.valueOf(markerMap.get("type")));
                int dx = toInt(markerMap.get("dx"));
                int dy = toInt(markerMap.get("dy"));
                int dz = toInt(markerMap.get("dz"));
                String mobType = markerMap.get("mobType") != null ? String.valueOf(markerMap.get("mobType")) : null;
                definition.addMarker(new RelativeMarker(type, dx, dy, dz, mobType));
            }

            definitionsById.put(id, definition);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (DungeonDefinition definition : definitionsById.values()) {
            String path = "dungeons." + definition.getId();
            yaml.set(path + ".name", definition.getDisplayName());
            yaml.set(path + ".min-rank", definition.getMinRank().name());
            yaml.set(path + ".max-rank", definition.getMaxRank().name());
            yaml.set(path + ".instance-mode", definition.getInstanceMode().name());
            yaml.set(path + ".cooldown-minutes", definition.getCooldownMinutes());
            yaml.set(path + ".origin.x", definition.getOriginX());
            yaml.set(path + ".origin.y", definition.getOriginY());
            yaml.set(path + ".origin.z", definition.getOriginZ());
            yaml.set(path + ".capture-world", definition.getCaptureWorld());
            yaml.set(path + ".dimensions.width", definition.getWidth());
            yaml.set(path + ".dimensions.height", definition.getHeight());
            yaml.set(path + ".dimensions.length", definition.getLength());
            yaml.set(path + ".schematic-file", definition.getSchematicFile());

            List<Map<String, Object>> markerMaps = new ArrayList<>();
            for (RelativeMarker marker : definition.getMarkers()) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("type", marker.getType().name());
                map.put("dx", marker.getDx());
                map.put("dy", marker.getDy());
                map.put("dz", marker.getDz());
                if (marker.getMobType() != null) {
                    map.put("mobType", marker.getMobType());
                }
                markerMaps.add(map);
            }
            yaml.set(path + ".markers", markerMaps);
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save dungeons.yml", e);
        }
    }

    public DungeonDefinition getOrCreate(String id, String displayName) {
        return definitionsById.computeIfAbsent(id, k -> new DungeonDefinition(id, displayName));
    }

    public DungeonDefinition get(String id) {
        return definitionsById.get(id);
    }

    public List<DungeonDefinition> getAll() {
        return new ArrayList<>(definitionsById.values());
    }

    public boolean remove(String id) {
        boolean removed = definitionsById.remove(id) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public File schematicFile(String fileName) {
        return new File(new File(plugin.getDataFolder(), "dungeon_schematics"), fileName);
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Rank parseRank(String raw) {
        try {
            return Rank.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Rank.F;
        }
    }

    private DungeonInstanceMode parseMode(String raw) {
        try {
            return DungeonInstanceMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return DungeonInstanceMode.INSTANCED;
        }
    }
}