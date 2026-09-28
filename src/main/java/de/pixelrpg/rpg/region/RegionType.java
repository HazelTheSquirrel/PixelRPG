package de.pixelrpg.rpg.region;

public enum RegionType {
    WILDERNESS,
    VILLAGE,
    CITY,
    GUILD_CITY,
    RUINS,
    FORTRESS,
    DUNGEON,
    DANGER_ZONE,
    BOSS_ZONE,
    OTHER;

    public static RegionType parse(String value) {
        if (value == null) return OTHER;
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalized.equals("GUILD_TERRITORY")) return GUILD_CITY;
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return OTHER;
        }
    }
}
