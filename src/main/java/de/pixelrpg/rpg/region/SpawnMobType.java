package de.pixelrpg.rpg.region;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import java.util.Locale;

/** Validates entity types used by explicit region spawn points. */
public final class SpawnMobType {
    private SpawnMobType() {}
    public static EntityType resolve(String value) {
        if (value == null || value.isBlank()) return null;
        try { return EntityType.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_')); }
        catch (IllegalArgumentException ignored) { return null; }
    }
    public static boolean isLivingEntity(String value) {
        EntityType type = resolve(value);
        return type != null && type != EntityType.PLAYER && type.getEntityClass() != null
                && LivingEntity.class.isAssignableFrom(type.getEntityClass());
    }
    public static String normalize(String value) {
        EntityType type = resolve(value);
        if (!isLivingEntity(value)) throw new IllegalArgumentException("Unknown or non-living entity type: " + value);
        return type.name();
    }
}
