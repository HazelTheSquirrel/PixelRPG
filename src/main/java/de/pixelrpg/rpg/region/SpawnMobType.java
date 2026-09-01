package de.pixelrpg.rpg.region;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;

import java.util.Locale;

/** Validates and normalizes hostile Bukkit entity types for region spawn points. */
public final class SpawnMobType {
    private SpawnMobType() { }

    public static boolean isHostileMob(String value) {
        EntityType type = resolve(value);
        return type != null && type.getEntityClass() != null && Monster.class.isAssignableFrom(type.getEntityClass());
    }

    public static String normalize(String value) {
        EntityType type = resolve(value);
        if (type == null) throw new IllegalArgumentException("Unknown entity type: " + value);
        return type.name();
    }

    public static EntityType resolve(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return EntityType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
