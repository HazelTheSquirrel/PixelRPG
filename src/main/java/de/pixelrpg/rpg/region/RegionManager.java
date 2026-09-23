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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Central source of truth for PixelRPG regions and their runtime spatial indexes. */
public final class RegionManager {
    private final RegionRepository repository;
    private final Map<UUID, PixelRegion> regions = new ConcurrentHashMap<>();
    private final Map<String, PixelRegion> globalRegions = new ConcurrentHashMap<>();
    private final Map<ChunkKey, List<UUID>> index = new ConcurrentHashMap<>();
    private final Map<ChunkKey, List<SpawnPointRef>> spawnPointIndex = new ConcurrentHashMap<>();
    private final ExecutorService persistenceExecutor;
    private CompletableFuture<Void> persistenceChain = CompletableFuture.completedFuture(null);
    private volatile boolean shuttingDown;

    public RegionManager(RegionRepository repository) {
        this.repository = repository;
        this.persistenceExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-RegionIO");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void load() {
        if (shuttingDown) return;
        regions.clear();
        globalRegions.clear();
        index.clear();
        spawnPointIndex.clear();
        repository.load().forEach(this::registerLoaded);
        repository.loadGlobalRegions().forEach((world, region) -> globalRegions.put(world, region));
        Bukkit.getWorlds().forEach(world -> globalRegion(world.getName()));
        if (repository.consumeMigrationNeeded()) save();
    }

    public Optional<PixelRegion> get(UUID id) {
        if (id == null) return Optional.empty();
        PixelRegion normal = regions.get(id);
        if (normal != null) return Optional.of(normal);
        return globalRegions.values().stream().filter(region -> region.id().equals(id)).findFirst();
    }

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
        if (minY > maxY) return RegionGeometry.ValidationResult.invalid("MinY darf nicht größer als maxY sein.");
        var validation = RegionGeometry.validate(points);
        if (!validation.valid()) return validation;

        PixelRegion region = new PixelRegion(
                id, worldName, validation.geometry(), minY, maxY,
                name == null || name.isBlank() ? id.toString() : name,
                type == null ? RegionType.OTHER : type, "", null, java.util.Set.of(), "", "", 0,
                Map.of(), Map.of(), spawnPoints == null ? List.of() : List.copyOf(spawnPoints)
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

    /** Captures the current region state on the server thread and persists it sequentially off-thread. */
    public synchronized void save() {
        if (shuttingDown || persistenceExecutor.isShutdown()) return;
        List<PixelRegion> snapshot = regions.values().stream().map(RegionManager::snapshot).toList();
        enqueuePersistence(() -> repository.save(snapshot));
    }

    /** Persists a complete global region definition after an in-memory metadata change. */
    public synchronized void saveGlobalRegion(PixelRegion global) {
        if (shuttingDown || global == null || !global.isGlobal()) return;
        Map<String, PixelRegion> snapshot = globalRegions.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        enqueuePersistence(() -> repository.saveGlobalRegions(snapshot));
    }

    /** Changes a global region flag and persists the complete global region definition. */
    public synchronized void setGlobalFlag(String worldName, RegionFlag flag, boolean enabled) {
        if (shuttingDown || worldName == null || worldName.isBlank() || flag == null) return;
        PixelRegion global = globalRegion(worldName);
        global.setFlag(flag, enabled);
        saveGlobalRegion(global);
    }

    /** Changes the owner of a normal region and persists the updated ownership metadata. */
    public synchronized boolean setOwner(UUID regionId, UUID ownerId) {
        PixelRegion region = regions.get(regionId);
        if (region == null || region.isGlobal()) return false;
        if (ownerId == null) region.clearOwner();
        else region.setOwner(ownerId);
        save();
        return true;
    }

    /** Adds a player to a region's member set and persists the change. */
    public synchronized boolean addMember(UUID regionId, UUID memberId) {
        PixelRegion region = regions.get(regionId);
        if (region == null || region.isGlobal() || !region.addMember(memberId)) return false;
        save();
        return true;
    }

    /** Removes a player from a region's member set and persists the change. */
    public synchronized boolean removeMember(UUID regionId, UUID memberId) {
        PixelRegion region = regions.get(regionId);
        if (region == null || region.isGlobal() || !region.removeMember(memberId)) return false;
        save();
        return true;
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

    /** Returns the spawn points in the indexed chunk window around a location. */
    public List<SpawnPointRef> spawnPointsNear(Location location, int radiusChunks) {
        if (location == null || location.getWorld() == null || radiusChunks < 0) return List.of();
        int centerX = floorChunk(location.getX());
        int centerZ = floorChunk(location.getZ());
        int minX = centerX - radiusChunks;
        int maxX = centerX + radiusChunks;
        int minZ = centerZ - radiusChunks;
        int maxZ = centerZ + radiusChunks;
        List<SpawnPointRef> result = new ArrayList<>();
        String worldName = location.getWorld().getName();
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                List<SpawnPointRef> points = spawnPointIndex.get(new ChunkKey(worldName, chunkX, chunkZ));
                if (points != null) result.addAll(points);
            }
        }
        return result;
    }

    /** Resolves a flag from the most specific region, falling back to the world's global region when unset. */
    public boolean hasFlag(Location location, RegionFlag flag) {
        if (location == null || location.getWorld() == null || flag == null) return true;
        Optional<PixelRegion> local = findLocal(location);
        if (local.isPresent() && local.get().hasFlag(flag)) return local.get().flag(flag);
        return globalRegion(location.getWorld().getName()).flag(flag);
    }

    /** Returns whether a location is an explicitly configured spawn point for the supplied entity type. */
    public boolean isExplicitSpawnPoint(Location location, String mobType) {
        if (location == null || location.getWorld() == null || mobType == null) return false;
        String normalized = SpawnMobType.normalize(mobType);
        List<SpawnPointRef> candidates = spawnPointIndex.getOrDefault(
                new ChunkKey(location.getWorld().getName(), floorChunk(location.getX()), floorChunk(location.getZ())), List.of());
        return candidates.stream()
                .map(SpawnPointRef::point)
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

    public synchronized void shutdown() {
        if (shuttingDown) return;
        shuttingDown = true;
        persistenceExecutor.shutdown();
        try {
            if (!persistenceExecutor.awaitTermination(10, TimeUnit.SECONDS)) persistenceExecutor.shutdownNow();
        } catch (InterruptedException exception) {
            persistenceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        regions.clear();
        globalRegions.clear();
        index.clear();
        spawnPointIndex.clear();
    }

    private static Map<RegionFlag, Boolean> defaultGlobalFlags() {
        EnumMapBuilder builder = new EnumMapBuilder();
        builder.put(RegionFlag.PVP, true)
                .put(RegionFlag.MOB_DAMAGE, true)
                .put(RegionFlag.MOB_SPAWNING, true)
                .put(RegionFlag.DENY_SPAWN, false)
                .put(RegionFlag.BLOCK_BREAK, true)
                .put(RegionFlag.BLOCK_PLACE, true)
                .put(RegionFlag.ENTITY_INTERACTION, true)
                .put(RegionFlag.DAMAGE_ANIMALS, true)
                .put(RegionFlag.ITEM_DROP, true)
                .put(RegionFlag.ITEM_PICKUP, true)
                .put(RegionFlag.FALL_DAMAGE, true)
                .put(RegionFlag.FIRE_SPREAD, false)
                .put(RegionFlag.LAVA_FLOW, false)
                .put(RegionFlag.WATER_FLOW, true)
                .put(RegionFlag.EXPLOSION, false)
                .put(RegionFlag.TNT, false)
                .put(RegionFlag.CREEPER_EXPLOSION, false)
                .put(RegionFlag.GHAST_FIREBALL, false)
                .put(RegionFlag.ENDERMAN_GRIEF, false)
                .put(RegionFlag.LIGHTNING, true)
                .put(RegionFlag.CROP_GROWTH, true)
                .put(RegionFlag.LEAF_DECAY, true)
                .put(RegionFlag.BLOCK_TRAMPLING, true)
                .put(RegionFlag.ENTRY, true)
                .put(RegionFlag.EXIT, true)
                .put(RegionFlag.RESPAWN_ANCHORS, true)
                .put(RegionFlag.SLEEP, true)
                .put(RegionFlag.ENDERPEARL, true)
                .put(RegionFlag.CHORUS_FRUIT_TELEPORT, true)
                .put(RegionFlag.NATURAL_HEALTH_REGEN, true)
                .put(RegionFlag.NATURAL_HUNGER_DRAIN, true);
        for (RegionFlag flag : RegionFlag.values()) {
            if (!builder.contains(flag)) builder.put(flag, true);
        }
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
        for (int index = 0; index < region.spawnPoints().size(); index++) {
            RegionSpawnPoint point = region.spawnPoints().get(index);
            ChunkKey key = new ChunkKey(point.worldName(), floorChunk(point.x()), floorChunk(point.z()));
            SpawnPointRef reference = new SpawnPointRef(region.id(), index, point);
            spawnPointIndex.compute(key, (ignored, current) -> {
                List<SpawnPointRef> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
                updated.add(reference);
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
                List<SpawnPointRef> updated = current.stream().filter(existing -> !existing.regionId().equals(region.id())).toList();
                return updated.isEmpty() ? null : updated;
            });
        }
    }

    private void enqueuePersistence(Runnable write) {
        persistenceChain = persistenceChain.handle((ignored, throwable) -> null)
                .thenRunAsync(write, persistenceExecutor);
    }

    private static PixelRegion snapshot(PixelRegion region) {
        return new PixelRegion(
                region.id(), region.worldName(), region.geometry(), region.minY(), region.maxY(), region.name(),
                region.type(), region.description(), region.ownerId(), region.members(), region.enterMessage(),
                region.leaveMessage(), region.priority(), region.flags(), region.properties(), region.spawnPoints());
    }

    private static int floorChunk(double coordinate) { return Math.floorDiv((int) Math.floor(coordinate), 16); }
    private record ChunkKey(String world, int x, int z) { }
    public record SpawnPointRef(UUID regionId, int index, RegionSpawnPoint point) { }

    private static final class EnumMapBuilder {
        private final java.util.EnumMap<RegionFlag, Boolean> values = new java.util.EnumMap<>(RegionFlag.class);

        private EnumMapBuilder put(RegionFlag flag, boolean enabled) {
            values.put(flag, enabled);
            return this;
        }

        private boolean contains(RegionFlag flag) {
            return values.containsKey(flag);
        }

        private Map<RegionFlag, Boolean> build() {
            return Map.copyOf(values);
        }
    }
}
