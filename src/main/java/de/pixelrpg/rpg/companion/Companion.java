package de.pixelrpg.rpg.companion;

import org.bukkit.entity.EntityType;

import java.util.Objects;

/** Persistent companion definition and progression state. */
public record Companion(
        String id,
        String name,
        int level,
        long experience,
        CompanionRarity rarity,
        EntityType entityType,
        boolean active
) {
    public Companion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(entityType, "entityType");
        if (level < 1 || level > 99) throw new IllegalArgumentException("level must be between 1 and 99");
        if (experience < 0L) throw new IllegalArgumentException("experience must not be negative");
    }

    /** Compatibility constructor for simple companion definitions. */
    public Companion(String id, String name, int level, boolean active) {
        this(id, name, level, 0L, CompanionRarity.COMMON, EntityType.WOLF, active);
    }

    public Companion withActive(boolean value) {
        return new Companion(id, name, level, experience, rarity, entityType, value);
    }

    public Companion withName(String value) {
        return new Companion(id, value, level, experience, rarity, entityType, active);
    }

    public Companion withProgress(int newLevel, long newExperience) {
        return new Companion(id, name, newLevel, newExperience, rarity, entityType, active);
    }

    public Companion withDefinition(CompanionRarity newRarity, EntityType newEntityType) {
        return new Companion(id, name, level, experience, newRarity, newEntityType, active);
    }
}
