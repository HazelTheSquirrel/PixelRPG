package de.pixelrpg.rpg.region;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Region metadata with immutable world/geometry and editable properties. */
public final class PixelRegion {
    private final UUID id;
    private final String worldName;
    private final RegionGeometry geometry;
    private final int minY;
    private final int maxY;
    private String name;
    private RegionType type;
    private String description;
    private UUID ownerGuildId;
    private String ownerGuildName;
    private String enterMessage;
    private String leaveMessage;
    private int priority;
    private final EnumMap<RegionFlag, Boolean> flags;
    private final Map<String, String> properties;

    public PixelRegion(UUID id, String worldName, RegionGeometry geometry, int minY, int maxY, String name,
                       RegionType type, String description, UUID ownerGuildId, String ownerGuildName,
                       String enterMessage, String leaveMessage, int priority,
                       Map<RegionFlag, Boolean> flags, Map<String, String> properties) {
        if (minY > maxY) throw new IllegalArgumentException("minY must not exceed maxY");
        this.id = id;
        this.worldName = worldName;
        this.geometry = geometry;
        this.minY = minY;
        this.maxY = maxY;
        this.name = name;
        this.type = type;
        this.description = description == null ? "" : description;
        this.ownerGuildId = ownerGuildId;
        this.ownerGuildName = ownerGuildName;
        this.enterMessage = enterMessage == null ? "" : enterMessage;
        this.leaveMessage = leaveMessage == null ? "" : leaveMessage;
        this.priority = priority;
        this.flags = new EnumMap<>(RegionFlag.class);
        if (flags != null) this.flags.putAll(flags);
        this.properties = new java.util.HashMap<>();
        if (properties != null) this.properties.putAll(properties);
    }

    public UUID id() { return id; }
    public String worldName() { return worldName; }
    public RegionGeometry geometry() { return geometry; }
    public int minY() { return minY; }
    public int maxY() { return maxY; }
    public String name() { return name; }
    public RegionType type() { return type; }
    public String description() { return description; }
    public UUID ownerGuildId() { return ownerGuildId; }
    public String ownerGuildName() { return ownerGuildName; }
    public String enterMessage() { return enterMessage; }
    public String leaveMessage() { return leaveMessage; }
    public int priority() { return priority; }
    public Map<RegionFlag, Boolean> flags() { return Map.copyOf(flags); }
    public Map<String, String> properties() { return Map.copyOf(properties); }

    public boolean contains(double x, int y, double z) {
        return y >= minY && y <= maxY && geometry.contains(x, z);
    }

    public boolean flag(RegionFlag flag) { return flags.getOrDefault(flag, false); }
    public void setFlag(RegionFlag flag, boolean enabled) { flags.put(flag, enabled); }
    public void setName(String value) { name = value; }
    public void setType(RegionType value) { type = value; }
    public void setDescription(String value) { description = value == null ? "" : value; }
    public void setOwner(UUID id, String guildName) { ownerGuildId = id; ownerGuildName = guildName; }
    public void clearOwner() { ownerGuildId = null; ownerGuildName = null; }
    public void setEnterMessage(String value) { enterMessage = value == null ? "" : value; }
    public void setLeaveMessage(String value) { leaveMessage = value == null ? "" : value; }
    public void setPriority(int value) { priority = value; }
    public void setProperty(String key, String value) { properties.put(key, value); }
    public void removeProperty(String key) { properties.remove(key); }
}
