package de.pixelrpg.rpg.region;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Region metadata with immutable world/geometry and editable properties. */
public final class PixelRegion {
    private final UUID id;
    private final String worldName;
    private final RegionGeometry geometry;
    private final boolean global;
    private final int minY;
    private final int maxY;
    private String name;
    private RegionType type;
    private String description;
    private UUID ownerId;
    private final Set<UUID> members;
    private String enterMessage;
    private String leaveMessage;
    private int priority;
    private final EnumMap<RegionFlag, Boolean> flags;
    private final Map<String, String> properties;
    private final List<RegionSpawnPoint> spawnPoints;

    public PixelRegion(UUID id, String worldName, RegionGeometry geometry, int minY, int maxY, String name,
                       RegionType type, String description, UUID ownerId, Set<UUID> members,
                       String enterMessage, String leaveMessage, int priority,
                       Map<RegionFlag, Boolean> flags, Map<String, String> properties) {
        this(id, worldName, geometry, minY, maxY, name, type, description, ownerId, members,
                enterMessage, leaveMessage, priority, flags, properties, List.of());
    }

    public PixelRegion(UUID id, String worldName, RegionGeometry geometry, int minY, int maxY, String name,
                       RegionType type, String description, UUID ownerId, Set<UUID> members,
                       String enterMessage, String leaveMessage, int priority,
                       Map<RegionFlag, Boolean> flags, Map<String, String> properties,
                       List<RegionSpawnPoint> spawnPoints) {
        this(id, worldName, geometry, false, minY, maxY, name, type, description, ownerId, members,
                enterMessage, leaveMessage, priority, flags, properties, spawnPoints);
    }

    private PixelRegion(UUID id, String worldName, RegionGeometry geometry, boolean global, int minY, int maxY,
                        String name, RegionType type, String description, UUID ownerId, Set<UUID> members,
                        String enterMessage, String leaveMessage, int priority,
                        Map<RegionFlag, Boolean> flags, Map<String, String> properties,
                        List<RegionSpawnPoint> spawnPoints) {
        if (minY > maxY) throw new IllegalArgumentException("minY must not exceed maxY");
        if (worldName == null || worldName.isBlank()) throw new IllegalArgumentException("worldName must not be blank");
        if (!global && geometry == null) throw new IllegalArgumentException("geometry is required for normal regions");
        this.id = id;
        this.worldName = worldName;
        this.geometry = geometry;
        this.global = global;
        this.minY = minY;
        this.maxY = maxY;
        this.name = name;
        this.type = type;
        this.description = description == null ? "" : description;
        this.ownerId = global ? null : ownerId;
        this.members = new HashSet<>();
        if (members != null) this.members.addAll(members);
        if (this.ownerId != null) this.members.remove(this.ownerId);
        this.enterMessage = enterMessage == null ? "" : enterMessage;
        this.leaveMessage = leaveMessage == null ? "" : leaveMessage;
        this.priority = priority;
        this.flags = new EnumMap<>(RegionFlag.class);
        if (flags != null) this.flags.putAll(flags);
        this.properties = new java.util.HashMap<>();
        if (properties != null) this.properties.putAll(properties);
        this.spawnPoints = new ArrayList<>();
        if (spawnPoints != null) this.spawnPoints.addAll(spawnPoints);
    }

    public static PixelRegion global(String worldName, Map<RegionFlag, Boolean> flags) {
        return new PixelRegion(UUID.nameUUIDFromBytes(("pixelrpg:global:" + worldName).getBytes(StandardCharsets.UTF_8)),
                worldName, null, true, Integer.MIN_VALUE, Integer.MAX_VALUE, "Wildnis", RegionType.OTHER,
                "Globale Standardregion", null, Set.of(), "", "", 0, flags, Map.of(), List.of());
    }

    public UUID id() { return id; }
    public String worldName() { return worldName; }
    public RegionGeometry geometry() { return geometry; }
    public boolean isGlobal() { return global; }
    public int minY() { return minY; }
    public int maxY() { return maxY; }
    public String name() { return name; }
    public RegionType type() { return type; }
    public String description() { return description; }
    public UUID ownerId() { return ownerId; }
    public Set<UUID> members() { return Set.copyOf(members); }
    public String enterMessage() { return enterMessage; }
    public String leaveMessage() { return leaveMessage; }
    public int priority() { return priority; }
    public Map<RegionFlag, Boolean> flags() { return Map.copyOf(flags); }
    public Map<String, String> properties() { return Map.copyOf(properties); }
    public List<RegionSpawnPoint> spawnPoints() { return List.copyOf(spawnPoints); }

    public boolean contains(double x, int y, double z) {
        return global || (y >= minY && y <= maxY && geometry.contains(x, z));
    }

    /** Returns whether this region explicitly defines the supplied rule. */
    public boolean hasFlag(RegionFlag flag) { return flags.containsKey(flag); }

    /** Returns whether the supplied rule is enabled; global regions use the current built-in default when unset. */
    public boolean flag(RegionFlag flag) { return flags.getOrDefault(flag, defaultFlag(flag)); }

    private static boolean defaultFlag(RegionFlag flag) {
        return switch (flag) {
            case FIRE_SPREAD, LAVA_FLOW, EXPLOSION, TNT, CREEPER_EXPLOSION, GHAST_FIREBALL,
                 ENDERMAN_GRIEF, DENY_SPAWN -> false;
            default -> true;
        };
    }

    public boolean isOwner(UUID playerId) { return !global && playerId != null && playerId.equals(ownerId); }
    public boolean isMember(UUID playerId) { return playerId != null && members.contains(playerId); }
    public void setFlag(RegionFlag flag, boolean enabled) { flags.put(flag, enabled); }
    public void setName(String value) { name = value; }
    public void setType(RegionType value) { type = value; }
    public void setDescription(String value) { description = value == null ? "" : value; }
    public void setOwner(UUID id) {
        ownerId = global ? null : id;
        if (ownerId != null) members.remove(ownerId);
    }
    public void clearOwner() { ownerId = null; }
    public boolean addMember(UUID playerId) {
        if (playerId == null || isOwner(playerId)) return false;
        return members.add(playerId);
    }
    public boolean removeMember(UUID playerId) { return playerId != null && members.remove(playerId); }
    public void setEnterMessage(String value) { enterMessage = value == null ? "" : value; }
    public void setLeaveMessage(String value) { leaveMessage = value == null ? "" : value; }
    public void setPriority(int value) { priority = value; }
    public void setProperty(String key, String value) { properties.put(key, value); }
    public void removeProperty(String key) { properties.remove(key); }
    public void addSpawnPoint(RegionSpawnPoint point) { spawnPoints.add(point); }
}
