// src/main/java/de/pixelrpg/rpg/region/YamlRegionRepository.java
package de.pixelrpg.rpg.region;

import de.pixelrpg.rpg.core.Rank;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class YamlRegionRepository implements RegionRepository {

    private final File file;

    public YamlRegionRepository(File dataFolder) {
        this.file = new File(dataFolder, "regions.yml");
    }

    @Override
    public void init() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public List<Region> loadAll() {
        List<Region> result = new ArrayList<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("regions");
        if (root == null) {
            return result;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            String name = section.getString("name", id);
            RegionCategory category = parseCategory(section.getString("category", "WILDERNESS"));
            Region region = new Region(id, name, category);
            region.setMinRank(parseRank(section.getString("min-rank", "F")));
            region.setMaxRank(parseRank(section.getString("max-rank", "S")));
            region.setPriority(section.getInt("priority", 0));

            List<Map<?, ?>> boxMaps = section.getMapList("boxes");
            for (Map<?, ?> boxMap : boxMaps) {
                String world = String.valueOf(boxMap.get("world"));
                int minX = toInt(boxMap.get("minX"));
                int minY = toInt(boxMap.get("minY"));
                int minZ = toInt(boxMap.get("minZ"));
                int maxX = toInt(boxMap.get("maxX"));
                int maxY = toInt(boxMap.get("maxY"));
                int maxZ = toInt(boxMap.get("maxZ"));
                region.addBox(new CuboidBounds(world, minX, minY, minZ, maxX, maxY, maxZ));
            }

            result.add(region);
        }

        return result;
    }

    @Override
    public void saveAll(Collection<Region> regions) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Region region : regions) {
            String path = "regions." + region.getId();
            yaml.set(path + ".name", region.getDisplayName());
            yaml.set(path + ".category", region.getCategory().name());
            yaml.set(path + ".min-rank", region.getMinRank().name());
            yaml.set(path + ".max-rank", region.getMaxRank().name());
            yaml.set(path + ".priority", region.getPriority());

            List<Map<String, Object>> boxMaps = new ArrayList<>();
            for (CuboidBounds box : region.getBoxes()) {
                boxMaps.add(Map.of(
                        "world", box.worldName(),
                        "minX", box.minX(), "minY", box.minY(), "minZ", box.minZ(),
                        "maxX", box.maxX(), "maxY", box.maxY(), "maxZ", box.maxZ()
                ));
            }
            yaml.set(path + ".boxes", boxMaps);
        }

        yaml.save(file);
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private RegionCategory parseCategory(String raw) {
        try {
            return RegionCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return RegionCategory.WILDERNESS;
        }
    }

    private Rank parseRank(String raw) {
        try {
            return Rank.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Rank.F;
        }
    }
}