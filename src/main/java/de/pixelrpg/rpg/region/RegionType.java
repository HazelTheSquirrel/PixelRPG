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
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return OTHER;
        }
    }
}
