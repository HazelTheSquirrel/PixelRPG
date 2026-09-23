package de.pixelrpg.rpg.region;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/** Persists region definitions without expanding polygons into block lists. */
public final class RegionRepository {
    private static final int CURRENT_FORMAT_VERSION = 8;

    private final File file;
    private final Logger logger;
    private volatile boolean migrationNeeded;

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
            migrateFormatIfNeeded(formatVersion);
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
                            flags.put(RegionFlag.valueOf(key), formatVersion < 3 ? !value : value);
                        } catch (IllegalArgumentException ignored) { }
                    }
                }
                migrateLegacyFlags(flags);

                Map<String, String> properties = new HashMap<>();
                ConfigurationSection propertySection = yaml.getConfigurationSection(base + ".properties");
                if (propertySection != null) {
                    propertySection.getKeys(false).forEach(key -> properties.put(key, propertySection.getString(key, "")));
                }

                List<RegionSpawnPoint> spawnPoints = new ArrayList<>();
                for (Map<?, ?> entry : yaml.getMapList(base + ".spawn-points")) {
                    Object mob = entry.get("mob");
                    Object x = entry.get("x");
                    Object y = entry.get("y");
                    Object z = entry.get("z");
                    if (mob == null || x == null || y == null || z == null) continue;
                    spawnPoints.add(new RegionSpawnPoint(String.valueOf(mob), world, number(x), number(y), number(z)));
                }

                UUID ownerId = parseUuid(root.getString(idText + ".owner"));
                Set<UUID> members = new HashSet<>();
                for (String member : yaml.getStringList(base + ".members")) {
                    UUID memberId = parseUuid(member);
                    if (memberId != null) members.add(memberId);
                }
                if (ownerId != null) members.remove(ownerId);

                result.add(new PixelRegion(
                        id, world, validation.geometry(), root.getInt(idText + ".min-y"), root.getInt(idText + ".max-y"),
                        root.getString(idText + ".name", idText), RegionType.parse(root.getString(idText + ".type", "OTHER")),
                        root.getString(idText + ".description", ""), ownerId, members,
                        root.getString(idText + ".enter-message", ""), root.getString(idText + ".leave-message", ""),
                        root.getInt(idText + ".priority", 0), flags, properties, spawnPoints));
            } catch (Exception exception) {
                logger.warning("Skipping malformed region " + idText + ": " + exception.getMessage());
            }
        }

        migrateFormatIfNeeded(formatVersion);
        return List.copyOf(result);
    }

    public boolean consumeMigrationNeeded() {
        if (!migrationNeeded) return false;
        migrationNeeded = false;
        return true;
    }

    /** Loads complete world-wide region definitions, including legacy flag-only global regions. */
    public Map<String, PixelRegion> loadGlobalRegions() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection worlds = yaml.getConfigurationSection("global-regions");
        if (worlds == null) return Map.of();

        Map<String, PixelRegion> result = new HashMap<>();
        for (String world : worlds.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection("global-regions." + world);
            EnumMap<RegionFlag, Boolean> flags = defaultGlobalFlags();
            if (section != null) {
                ConfigurationSection flagSection = section.getConfigurationSection("flags");
                if (flagSection != null) {
                    for (String key : flagSection.getKeys(false)) {
                        try { flags.put(RegionFlag.valueOf(key), flagSection.getBoolean(key)); }
                        catch (IllegalArgumentException ignored) { }
                    }
                }
            }
            migrateLegacyFlags(flags);
            result.put(world, PixelRegion.global(
                    world,
                    Map.copyOf(flags),
                    section == null ? "Wildnis" : section.getString("name", "Wildnis"),
                    section == null ? RegionType.OTHER : RegionType.parse(section.getString("type", "OTHER")),
                    section == null ? "Globale Standardregion" : section.getString("description", "Globale Standardregion"),
                    section == null ? "" : section.getString("enter-message", ""),
                    section == null ? "" : section.getString("leave-message", ""),
                    section == null ? 0 : section.getInt("priority", 0),
                    section == null ? Map.of() : readProperties(section)
            ));
        }
        return Map.copyOf(result);
    }

    /** Persists complete global region definitions without disturbing normal regions. */
    public synchronized void saveGlobalRegions(Map<String, PixelRegion> globalRegions) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("format-version", CURRENT_FORMAT_VERSION);
        yaml.set("global-regions", null);
        for (Map.Entry<String, PixelRegion> entry : globalRegions.entrySet()) {
            PixelRegion region = entry.getValue();
            String base = "global-regions." + entry.getKey();
            yaml.set(base + ".name", region.name());
            yaml.set(base + ".type", region.type().name());
            yaml.set(base + ".description", region.description());
            yaml.set(base + ".priority", region.priority());
            yaml.set(base + ".enter-message", region.enterMessage());
            yaml.set(base + ".leave-message", region.leaveMessage());
            for (Map.Entry<RegionFlag, Boolean> flag : region.flags().entrySet()) {
                yaml.set(base + ".flags." + flag.getKey().name(), flag.getValue());
            }
            for (Map.Entry<String, String> property : region.properties().entrySet()) {
                yaml.set(base + ".properties." + property.getKey(), property.getValue());
            }
        }
        writeAtomically(yaml);
    }

    private static Map<String, String> readProperties(ConfigurationSection section) {
        ConfigurationSection properties = section.getConfigurationSection("properties");
        if (properties == null) return Map.of();
        Map<String, String> result = new HashMap<>();
        for (String key : properties.getKeys(false)) result.put(key, properties.getString(key, ""));
        return Map.copyOf(result);
    }

    private void migrateFormatIfNeeded(int formatVersion) {
        if (formatVersion >= CURRENT_FORMAT_VERSION) return;
        migrationNeeded = true;
        logger.info("regions.yml requires migration to format version " + CURRENT_FORMAT_VERSION + "; migration will be persisted asynchronously.");
    }

    private static void migrateLegacyFlags(EnumMap<RegionFlag, Boolean> flags) {
        // Legacy flag names are intentionally ignored. Current RegionFlag values are read directly above;
        // removed legacy keys are discarded through the IllegalArgumentException catch.
    }

    private void writeAtomically(YamlConfiguration yaml) {
        Path target = file.toPath();
        Path temporary = target.resolveSibling(file.getName() + ".tmp");
        try {
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            throw new IllegalStateException("Could not save regions.yml", exception);
        }
    }

    private static EnumMap<RegionFlag, Boolean> defaultGlobalFlags() {
        EnumMap<RegionFlag, Boolean> flags = new EnumMap<>(RegionFlag.class);
        flags.put(RegionFlag.PVP, true);
        flags.put(RegionFlag.MOB_SPAWNING, true);
        flags.put(RegionFlag.DENY_SPAWN, false);
        flags.put(RegionFlag.BLOCK_BREAK, true);
        flags.put(RegionFlag.BLOCK_PLACE, true);
        flags.put(RegionFlag.ENTITY_INTERACTION, true);
        flags.put(RegionFlag.DAMAGE_ANIMALS, true);
        flags.put(RegionFlag.ITEM_DROP, true);
        flags.put(RegionFlag.ITEM_PICKUP, true);
        flags.put(RegionFlag.FALL_DAMAGE, true);
        flags.put(RegionFlag.FIRE_SPREAD, false);
        flags.put(RegionFlag.LAVA_FLOW, false);
        flags.put(RegionFlag.WATER_FLOW, true);
        flags.put(RegionFlag.EXPLOSION, false);
        flags.put(RegionFlag.TNT, false);
        flags.put(RegionFlag.CREEPER_EXPLOSION, false);
        flags.put(RegionFlag.GHAST_FIREBALL, false);
        flags.put(RegionFlag.ENDERMAN_GRIEF, false);
        flags.put(RegionFlag.LIGHTNING, true);
        flags.put(RegionFlag.CROP_GROWTH, true);
        flags.put(RegionFlag.LEAF_DECAY, true);
        flags.put(RegionFlag.BLOCK_TRAMPLING, true);
        flags.put(RegionFlag.ENTRY, true);
        flags.put(RegionFlag.EXIT, true);
        flags.put(RegionFlag.RESPAWN_ANCHORS, true);
        flags.put(RegionFlag.SLEEP, true);
        flags.put(RegionFlag.ENDERPEARL, true);
        flags.put(RegionFlag.CHORUS_FRUIT_TELEPORT, true);
        flags.put(RegionFlag.NATURAL_HEALTH_REGEN, true);
        flags.put(RegionFlag.NATURAL_HUNGER_DRAIN, true);
        for (RegionFlag flag : RegionFlag.values()) flags.putIfAbsent(flag, true);
        return flags;
    }

    private static UUID parseUuid(String value) {
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
    }
}
