package de.pixelrpg.rpg.region;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/** Persists region definitions without expanding polygons into block lists. */
public final class RegionRepository {
    private static final int CURRENT_FORMAT_VERSION = 3;

    private final File file;
    private final Logger logger;

    public RegionRepository(File dataFolder, Logger logger) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.file = new File(dataFolder, "regions.yml");
        this.logger = logger;
    }

    public List<PixelRegion> load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int formatVersion = yaml.getInt("format-version", 1);
        ConfigurationSection root = yaml.getConfigurationSection("regions");
        if (root == null) {
            migrateFormatIfNeeded(formatVersion, List.of(), yaml);
            return List.of();
        }

        List<PixelRegion> result = new ArrayList<>();
        for (String idText : root.getKeys(false)) {
            try {
                String base = "regions." + idText;
                UUID id = UUID.fromString(idText);
                String world = root.getString(idText + ".world");
                List<RegionPoint> points = new ArrayList<>();
                for (Map<?, ?> entry : root.getMapList(idText + ".points")) {
                    points.add(new RegionPoint(number(entry.get("x")), number(entry.get("z"))));
                }

                var validation = RegionGeometry.validate(points);
                if (!validation.valid() || world == null) {
                    logger.warning("Skipping invalid region " + idText + ": "
                            + (validation.error() == null ? "missing world" : validation.error()));
                    continue;
                }

                EnumMap<RegionFlag, Boolean> flags = new EnumMap<>(RegionFlag.class);
                ConfigurationSection flagSection = yaml.getConfigurationSection(base + ".flags");
                if (flagSection != null) {
                    for (String key : flagSection.getKeys(false)) {
                        try {
                            boolean value = flagSection.getBoolean(key);
                            // Format 1/2 used true = deny. Format 3 uses true = allow.
                            flags.put(RegionFlag.valueOf(key), formatVersion < 3 ? !value : value);
                        } catch (IllegalArgumentException ignored) {
                            // Unknown flags are intentionally ignored for forward compatibility.
                        }
                    }
                }

                Map<String, String> properties = new HashMap<>();
                ConfigurationSection propertySection = yaml.getConfigurationSection(base + ".properties");
                if (propertySection != null) {
                    propertySection.getKeys(false).forEach(key ->
                            properties.put(key, propertySection.getString(key, "")));
                }

                UUID guildId = parseUuid(root.getString(idText + ".owner-guild-id"));
                PixelRegion region = new PixelRegion(
                        id,
                        world,
                        validation.geometry(),
                        root.getInt(idText + ".min-y"),
                        root.getInt(idText + ".max-y"),
                        root.getString(idText + ".name", idText),
                        RegionType.parse(root.getString(idText + ".type", "OTHER")),
                        root.getString(idText + ".description", ""),
                        guildId,
                        root.getString(idText + ".owner-guild-name"),
                        root.getString(idText + ".enter-message", ""),
                        root.getString(idText + ".leave-message", ""),
                        root.getInt(idText + ".priority", 0),
                        flags,
                        properties
                );
                result.add(region);
            } catch (Exception exception) {
                logger.warning("Skipping malformed region " + idText + ": " + exception.getMessage());
            }
        }

        migrateFormatIfNeeded(formatVersion, result, yaml);
        return List.copyOf(result);
    }

    /** Loads world-wide defaults from the global region section. */
    public Map<String, Map<RegionFlag, Boolean>> loadGlobalFlags() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection worlds = yaml.getConfigurationSection("global-regions");
        if (worlds == null) return Map.of();
        Map<String, Map<RegionFlag, Boolean>> result = new HashMap<>();
        for (String world : worlds.getKeys(false)) {
            ConfigurationSection flags = worlds.getConfigurationSection(world + ".flags");
            EnumMap<RegionFlag, Boolean> values = defaultGlobalFlags();
            if (flags != null) {
                for (String key : flags.getKeys(false)) {
                    try {
                        values.put(RegionFlag.valueOf(key), flags.getBoolean(key));
                    } catch (IllegalArgumentException ignored) {
                        // Unknown future flags are ignored.
                    }
                }
            }
            result.put(world, Map.copyOf(values));
        }
        return Map.copyOf(result);
    }

    /** Saves the world-wide defaults while preserving all normal regions. */
    public synchronized void saveGlobalFlags(Map<String, Map<RegionFlag, Boolean>> globalFlags) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("format-version", CURRENT_FORMAT_VERSION);
        yaml.set("global-regions", null);
        for (Map.Entry<String, Map<RegionFlag, Boolean>> world : globalFlags.entrySet()) {
            for (Map.Entry<RegionFlag, Boolean> flag : world.getValue().entrySet()) {
                yaml.set("global-regions." + world.getKey() + ".flags." + flag.getKey().name(), flag.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save regions.yml", exception);
        }
    }

    public synchronized void save(Iterable<PixelRegion> regions) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("format-version", CURRENT_FORMAT_VERSION);
        yaml.set("regions", null);

        for (PixelRegion region : regions) {
            String base = "regions." + region.id();
            yaml.set(base + ".world", region.worldName());
            yaml.set(base + ".name", region.name());
            yaml.set(base + ".type", region.type().name());
            yaml.set(base + ".description", region.description());
            yaml.set(base + ".min-y", region.minY());
            yaml.set(base + ".max-y", region.maxY());
            yaml.set(base + ".priority", region.priority());

            if (region.ownerGuildId() != null) yaml.set(base + ".owner-guild-id", region.ownerGuildId().toString());
            if (region.ownerGuildName() != null) yaml.set(base + ".owner-guild-name", region.ownerGuildName());

            List<Map<String, Double>> points = region.geometry().points().stream()
                    .map(point -> Map.of("x", point.x(), "z", point.z()))
                    .toList();
            yaml.set(base + ".points", points);

            for (Map.Entry<RegionFlag, Boolean> flag : region.flags().entrySet()) {
                yaml.set(base + ".flags." + flag.getKey().name(), flag.getValue());
            }
            for (Map.Entry<String, String> property : region.properties().entrySet()) {
                yaml.set(base + ".properties." + property.getKey(), property.getValue());
            }

            yaml.set(base + ".enter-message", region.enterMessage());
            yaml.set(base + ".leave-message", region.leaveMessage());
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save regions.yml", exception);
        }
    }

    private void migrateFormatIfNeeded(int formatVersion, List<PixelRegion> regions, YamlConfiguration yaml) {
        if (formatVersion >= CURRENT_FORMAT_VERSION) return;
        save(regions);
        logger.info("Migrated regions.yml to format version " + CURRENT_FORMAT_VERSION + ".");
    }

    private static EnumMap<RegionFlag, Boolean> defaultGlobalFlags() {
        EnumMap<RegionFlag, Boolean> flags = new EnumMap<>(RegionFlag.class);
        flags.put(RegionFlag.PVP, true);
        flags.put(RegionFlag.MONSTER_SPAWN, true);
        flags.put(RegionFlag.BLOCK_BREAK, true);
        flags.put(RegionFlag.BLOCK_PLACE, true);
        flags.put(RegionFlag.FIRE_SPREAD, false);
        flags.put(RegionFlag.LAVA_FLOW, false);
        flags.put(RegionFlag.EXPLOSION, false);
        flags.put(RegionFlag.CREEPER_EXPLOSION, false);
        flags.put(RegionFlag.GHAST_FIREBALL, false);
        flags.put(RegionFlag.ENDERMAN_GRIEF, false);
        return flags;
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
    }
}
