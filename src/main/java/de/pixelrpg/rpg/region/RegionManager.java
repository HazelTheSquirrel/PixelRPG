package de.pixelrpg.rpg.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Central source of truth for PixelRPG regions and their runtime spatial indexes. */
public final class RegionManager {
    private final RegionRepository repository;
    private final Map<UUID, PixelRegion> regions = new ConcurrentHashMap<>();
    private final Map<String, PixelRegion> globalRegions = new ConcurrentHashMap<>();
    private final Map<ChunkKey, List<UUID>> index = new ConcurrentHashMap<>();
    private final Map<ChunkKey, List<RegionSpawnPoint>> spawnPointIndex = new ConcurrentHashMap<>();

    public RegionManager(RegionRepository repository) { this.repository = repository; }

    public void load() {
        regions.clear();
        globalRegions.clear();
        index.clear();
        spawnPointIndex.clear();
        repository.load().forEach(this::registerLoaded);
        repository.loadGlobalFlags().forEach((world, flags) -> globalRegions.put(world, PixelRegion.global(world, flags)));
        Bukkit.getWorlds().forEach(world -> globalRegion(world.getName()));
    }

    public Optional<PixelRegion> get(UUID id) { return Optional.ofNullable(regions.get(id)); }

    public List<PixelRegion> all() {
        return regions.values().stream()
                .sorted(Comparator.comparing(PixelRegion::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized RegionGeometry.ValidationResult create(UUID id, String worldName, List<RegionPoint> points,
                                                                 int minY, int maxY, String name, RegionType type) {
        return create(id, worldName, points, minY, maxY, name, type, List.of());
    }

    public synchronized RegionGeometry.ValidationResult create(UUID id, String worldName, List<RegionPoint> points,
                                                                 int minY, int maxY, String name, RegionType type,
                                                                 List<RegionSpawnPoint> spawnPoints) {
        if (regions.containsKey(id)) return RegionGeometry.ValidationResult.invalid("Eine Region mit dieser ID existiert bereits.");
        if (worldName == null || worldName.isBlank()) return RegionGeometry.ValidationResult.invalid("Eine Welt ist erforderlich.");
        if (minY > maxY) return RegionGeometry.ValidationResult.invalid("MinY darf nicht größer als MaxY sein.");
        var validation = RegionGeometry.validate(points);
        if (!validation.valid()) return validation;

        PixelRegion region = new PixelRegion(
                id, worldName, validation.geometry(), minY, maxY,
                name == null || name.isBlank() ? id.toString() : name,
                type == null ? RegionType.OTHER : type, "", null, null, "", "", 0,
                Map.of(), Map.of(), spawnPoints
        );
        regions.put(id, region);
        addToIndex(region);
        addSpawnPointsToIndex(region);
        save();
        return RegionGeometry.ValidationResult.valid(validation.geometry());
    }

    public synchronized boolean delete(UUID id) {
        PixelRegion removed = regions.remove(id);
        if (removed == null) return false;
        removeFromIndex(removed);
        removeSpawnPointsFromIndex(removed);
        save();
        return true;
    }

    public synchronized void save() { repository.save(regions.values()); }

    public synchronized void setGlobalFlag(String worldName, RegionFlag flag, boolean enabled) {
        if (worldName == null || worldName.isBlank()) return;
        PixelRegion global = globalRegions.computeIfAbsent(worldName, world -> PixelRegion.global(world, defaultGlobalFlags()));
        global.setFlag(flag, enabled);
        repository.saveGlobalFlags(globalRegions.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().flags())));
    }

    public Optional<PixelRegion> find(World world, double x, int y, double z) {
        if (world == null) return Optional.empty();
        List<UUID> candidates = index.getOrDefault(new ChunkKey(world.getName(), floorChunk(x), floorChunk(z)), List.of());
        Optional<PixelRegion> region = candidates.stream()
                .map(regions::get)
                .filter(java.util.Objects::nonNull)
                .filter(candidate -> candidate.contains(x, y, z))
                .max(Comparator.comparingInt(PixelRegion::priority).thenComparing(PixelRegion::id));
        return region.isPresent() ? region : Optional.of(globalRegion(world.getName()));
    }

    public Optional<PixelRegion> find(Location location) {
        if (location == null || location.getWorld() == null) return Optional.empty();
        return find(location.getWorld(), location.getX(), location.getBlockY(), location.getZ());
    }

    /** Resolves a flag from the most specific region, falling back to the world's global region when unset. */
    public boolean hasFlag(Location location, RegionFlag flag) {
        if (location == null || location.getWorld() == null) return true;
        Optional<PixelRegion> local = findLocal(location);
        if (local.isPresent() && local.get().hasFlag(flag)) return local.get().flag(flag);
        return globalRegion(location.getWorld().getName()).flag(flag);
    }

    /** Returns whether a location is an explicitly configured spawn point for the supplied hostile mob. */
    public boolean isExplicitSpawnPoint(Location location, String mobType) {
        if (location == null || location.getWorld() == null || mobType == null) return false;
        String normalized = SpawnMobType.normalize(mobType);
        List<RegionSpawnPoint> candidates = spawnPointIndex.getOrDefault(
                new ChunkKey(location.getWorld().getName(), floorChunk(location.getX()), floorChunk(location.getZ())), List.of());
        return candidates.stream()
                .filter(point -> point.mobType().equalsIgnoreCase(normalized))
                .anyMatch(point -> Math.abs(point.x() - location.getX()) < 0.01D
                        && Math.abs(point.y() - location.getY()) < 0.01D
                        && Math.abs(point.z() - location.getZ()) < 0.01D);
    }

    private Optional<PixelRegion> findLocal(Location location) {
        List<UUID> candidates = index.getOrDefault(new ChunkKey(location.getWorld().getName(), floorChunk(location.getX()), floorChunk(location.getZ())), List.of());
        return candidates.stream()
                .map(regions::get)
                .filter(java.util.Objects::nonNull)
                .filter(region -> region.contains(location.getX(), location.getBlockY(), location.getZ()))
                .max(Comparator.comparingInt(PixelRegion::priority).thenComparing(PixelRegion::id));
    }

    public PixelRegion globalRegion(String worldName) {
        return globalRegions.computeIfAbsent(worldName, world -> PixelRegion.global(world, defaultGlobalFlags()));
    }

    private static Map<RegionFlag, Boolean> defaultGlobalFlags() {
        EnumMapBuilder builder = new EnumMapBuilder();
        builder.put(RegionFlag.PVP, true).put(RegionFlag.MONSTER_SPAWN, true).put(RegionFlag.BLOCK_BREAK, true)
                .put(RegionFlag.BLOCK_PLACE, true).put(RegionFlag.FIRE_SPREAD, false).put(RegionFlag.LAVA_FLOW, false)
                .put(RegionFlag.EXPLOSION, false).put(RegionFlag.CREEPER_EXPLOSION, false)
                .put(RegionFlag.GHAST_FIREBALL, false).put(RegionFlag.ENDERMAN_GRIEF, false);
        return builder.build();
    }

    private void registerLoaded(PixelRegion region) {
        regions.put(region.id(), region);
        addToIndex(region);
        addSpawnPointsToIndex(region);
    }

    /** Adds a region only to the chunks covered by its bounding box. */
    private void addToIndex(PixelRegion region) {
        int minChunkX = floorChunk(region.geometry().minX());
        int maxChunkX = floorChunk(region.geometry().maxX());
        int minChunkZ = floorChunk(region.geometry().minZ());
        int maxChunkZ = floorChunk(region.geometry().maxZ());
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                ChunkKey key = new ChunkKey(region.worldName(), x, z);
                index.compute(key, (ignored, current) -> {
                    List<UUID> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
                    updated.add(region.id());
                    return List.copyOf(updated);
                });
            }
        }
    }

    private void addSpawnPointsToIndex(PixelRegion region) {
        for (RegionSpawnPoint point : region.spawnPoints()) {
            ChunkKey key = new ChunkKey(point.worldName(), floorChunk(point.x()), floorChunk(point.z()));
            spawnPointIndex.compute(key, (ignored, current) -> {
                List<RegionSpawnPoint> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
                updated.add(point);
                return List.copyOf(updated);
            });
        }
    }

    /** Removes one region from the spatial index without rebuilding unrelated chunks. */
    private void removeFromIndex(PixelRegion region) {
        int minChunkX = floorChunk(region.geometry().minX());
        int maxChunkX = floorChunk(region.geometry().maxX());
        int minChunkZ = floorChunk(region.geometry().minZ());
        int maxChunkZ = floorChunk(region.geometry().maxZ());
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                ChunkKey key = new ChunkKey(region.worldName(), x, z);
                index.computeIfPresent(key, (ignored, current) -> {
                    List<UUID> updated = current.stream().filter(existingId -> !existingId.equals(region.id())).toList();
                    return updated.isEmpty() ? null : updated;
                });
            }
        }
    }

    private void removeSpawnPointsFromIndex(PixelRegion region) {
        for (RegionSpawnPoint point : region.spawnPoints()) {
            ChunkKey key = new ChunkKey(point.worldName(), floorChunk(point.x()), floorChunk(point.z()));
            spawnPointIndex.computeIfPresent(key, (ignored, current) -> {
                List<RegionSpawnPoint> updated = current.stream()
                        .filter(existing -> existing != point)
                        .toList();
                return updated.isEmpty() ? null : updated;
            });
        }
    }

    private static int floorChunk(double coordinate) { return Math.floorDiv((int) Math.floor(coordinate), 16); }
    private record ChunkKey(String world, int x, int z) { }

    private static final class EnumMapBuilder {
        private final java.util.EnumMap<RegionFlag, Boolean> values = new java.util.EnumMap<>(RegionFlag.class);
        EnumMapBuilder put(RegionFlag flag, boolean value) { values.put(flag, value); return this; }
        Map<RegionFlag, Boolean> build() { return Map.copyOf(values); }
    }
}
