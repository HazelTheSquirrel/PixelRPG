package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Central source of truth for PixelRPG regions and their runtime spatial index. */
public final class RegionManager {
    private final RegionRepository repository;
    private final Map<UUID, PixelRegion> regions = new ConcurrentHashMap<>();
    private final Map<ChunkKey, List<UUID>> index = new ConcurrentHashMap<>();

    public RegionManager(RegionRepository repository) {
        this.repository = repository;
    }

    public void load() {
        regions.clear();
        index.clear();
        repository.load().forEach(this::registerLoaded);
    }

    public Optional<PixelRegion> get(UUID id) { return Optional.ofNullable(regions.get(id)); }
    public List<PixelRegion> all() { return regions.values().stream().sorted(Comparator.comparing(PixelRegion::name, String.CASE_INSENSITIVE_ORDER)).toList(); }

    public synchronized RegionGeometry.ValidationResult create(UUID id, String worldName, List<RegionPoint> points, int minY, int maxY,
                                                                 String name, RegionType type) {
        if (regions.containsKey(id)) return RegionGeometry.ValidationResult.invalid("Eine Region mit dieser ID existiert bereits.");
        if (worldName == null || worldName.isBlank()) return RegionGeometry.ValidationResult.invalid("Eine Welt ist erforderlich.");
        if (minY > maxY) return RegionGeometry.ValidationResult.invalid("MinY darf nicht größer als MaxY sein.");
        var validation = RegionGeometry.validate(points);
        if (!validation.valid()) return validation;
        PixelRegion region = new PixelRegion(id, worldName, validation.geometry(), minY, maxY,
                name == null || name.isBlank() ? id.toString() : name, type == null ? RegionType.OTHER : type,
                "", null, null, "", "", 0, Map.of(), Map.of());
        regions.put(id, region);
        rebuildIndex();
        save();
        return RegionGeometry.ValidationResult.valid(validation.geometry());
    }

    public synchronized boolean delete(UUID id) {
        PixelRegion removed = regions.remove(id);
        if (removed == null) return false;
        rebuildIndex();
        save();
        return true;
    }

    public synchronized void save() { repository.save(regions.values()); }

    public Optional<PixelRegion> find(World world, double x, int y, double z) {
        if (world == null) return Optional.empty();
        List<UUID> candidates = index.getOrDefault(new ChunkKey(world.getName(), floorChunk(x), floorChunk(z)), List.of());
        return candidates.stream().map(regions::get).filter(java.util.Objects::nonNull)
                .filter(region -> region.contains(x, y, z))
                .max(Comparator.comparingInt(PixelRegion::priority).thenComparing(PixelRegion::id));
    }

    public Optional<PixelRegion> find(Location location) {
        if (location == null || location.getWorld() == null) return Optional.empty();
        return find(location.getWorld(), location.getX(), location.getBlockY(), location.getZ());
    }

    public boolean hasFlag(Location location, RegionFlag flag) {
        return find(location).map(region -> region.flag(flag)).orElse(false);
    }

    private void registerLoaded(PixelRegion region) {
        regions.put(region.id(), region);
        addToIndex(region);
    }

    private void rebuildIndex() {
        index.clear();
        regions.values().forEach(this::addToIndex);
    }

    private void addToIndex(PixelRegion region) {
        int minChunkX = floorChunk(region.geometry().minX());
        int maxChunkX = floorChunk(region.geometry().maxX());
        int minChunkZ = floorChunk(region.geometry().minZ());
        int maxChunkZ = floorChunk(region.geometry().maxZ());
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                index.computeIfAbsent(new ChunkKey(region.worldName(), x, z), ignored -> new ArrayList<>()).add(region.id());
            }
        }
    }

    private static int floorChunk(double coordinate) { return Math.floorDiv((int) Math.floor(coordinate), 16); }
    private record ChunkKey(String world, int x, int z) { }
}
