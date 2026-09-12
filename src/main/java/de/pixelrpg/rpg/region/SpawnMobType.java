package de.pixelrpg.rpg.region;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;

/** Validates and normalizes living Bukkit entity types for region spawn points. */
public final class SpawnMobType {
    private SpawnMobType() { }

    public static boolean isLivingEntity(String value) {
        EntityType type = resolve(value);
        return type != null
                && type != EntityType.PLAYER
                && type.getEntityClass() != null
                && LivingEntity.class.isAssignableFrom(type.getEntityClass());
    }

    public static String normalize(String value) {
        EntityType type = resolve(value);
        if (type == null || type == EntityType.PLAYER
                || type.getEntityClass() == null
                || !LivingEntity.class.isAssignableFrom(type.getEntityClass())) {
            throw new IllegalArgumentException("Unknown or non-living entity type: " + value);
        }
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
