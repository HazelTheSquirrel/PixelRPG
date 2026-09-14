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
    private static final int CURRENT_FORMAT_VERSION = 6;

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

    /** Returns and clears the migration marker so the caller can schedule persistence off-thread. */
    public boolean consumeMigrationNeeded() {
        if (!migrationNeeded) return false;
        migrationNeeded = false;
        return true;
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
                    try { values.put(RegionFlag.valueOf(key), flags.getBoolean(key)); }
                    catch (IllegalArgumentException ignored) { }
                }
            }
            migrateLegacyFlags(values);
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
        writeAtomically(yaml);
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
            if (region.ownerId() != null) yaml.set(base + ".owner", region.ownerId().toString());
            yaml.set(base + ".members", region.members().stream().map(UUID::toString).sorted().toList());
            yaml.set(base + ".points", region.geometry().points().stream()
                    .map(point -> Map.of("x", point.x(), "z", point.z())).toList());
            for (Map.Entry<RegionFlag, Boolean> flag : region.flags().entrySet()) {
                yaml.set(base + ".flags." + flag.getKey().name(), flag.getValue());
            }
            for (Map.Entry<String, String> property : region.properties().entrySet()) {
                yaml.set(base + ".properties." + property.getKey(), property.getValue());
            }
            yaml.set(base + ".spawn-points", region.spawnPoints().stream()
                    .map(point -> Map.<String, Object>of("mob", point.mobType(), "x", point.x(), "y", point.y(), "z", point.z())).toList());
            yaml.set(base + ".enter-message", region.enterMessage());
            yaml.set(base + ".leave-message", region.leaveMessage());
        }
        writeAtomically(yaml);
    }

    private void migrateFormatIfNeeded(int formatVersion) {
        if (formatVersion >= CURRENT_FORMAT_VERSION) return;
        migrationNeeded = true;
        logger.info("regions.yml requires migration to format version " + CURRENT_FORMAT_VERSION + "; migration will be persisted asynchronously.");
    }

    private static void migrateLegacyFlags(EnumMap<RegionFlag, Boolean> flags) {
        mapLegacy(flags, RegionFlag.INTERACT, RegionFlag.ENTITY_INTERACTION);
        mapLegacy(flags, RegionFlag.USE,
                RegionFlag.DOOR_USE, RegionFlag.TRAPDOOR_USE, RegionFlag.FENCE_GATE_USE,
                RegionFlag.BUTTON_USE, RegionFlag.LEVER_USE, RegionFlag.PRESSURE_PLATE_USE);
        mapLegacy(flags, RegionFlag.CHEST_ACCESS,
                RegionFlag.CHEST_USE, RegionFlag.BARREL_USE, RegionFlag.SHULKER_BOX_USE,
                RegionFlag.HOPPER_USE, RegionFlag.DROPPER_USE, RegionFlag.DISPENSER_USE,
                RegionFlag.FURNACE_USE, RegionFlag.BLAST_FURNACE_USE, RegionFlag.SMOKER_USE,
                RegionFlag.BREWING_STAND_USE, RegionFlag.ENCHANTING_TABLE_USE,
                RegionFlag.CRAFTING_TABLE_USE, RegionFlag.ANVIL_USE);
        if (flags.containsKey(RegionFlag.MONSTER_SPAWN)) {
            boolean value = flags.get(RegionFlag.MONSTER_SPAWN);
            if (!value) {
                for (RegionFlag flag : RegionFlag.forCategory(RegionFlagCategory.MOB_SPAWN)) {
                    if (flag.name().startsWith("SPAWN_")) flags.putIfAbsent(flag, false);
                }
            }
        }
        flags.remove(RegionFlag.INTERACT);
        flags.remove(RegionFlag.USE);
        flags.remove(RegionFlag.CHEST_ACCESS);
        flags.remove(RegionFlag.MONSTER_SPAWN);
    }

    private static void mapLegacy(EnumMap<RegionFlag, Boolean> flags, RegionFlag legacy, RegionFlag... replacements) {
        Boolean value = flags.get(legacy);
        if (value == null || value) return;
        for (RegionFlag replacement : replacements) flags.putIfAbsent(replacement, false);
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
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
    }
}
