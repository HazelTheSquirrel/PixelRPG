package de.pixelrpg.rpg.companion;

import org.bukkit.entity.EntityType;

import java.util.Objects;
import java.util.UUID;

/**
 * Compatibility/view model used by existing UI and command APIs.
 * New runtime code keeps static data in CompanionDefinition and player state in CompanionInstance.
 */
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
        if (level < 1 || level > 999) throw new IllegalArgumentException("level must be between 1 and 999");
        if (experience < 0L) throw new IllegalArgumentException("experience must not be negative");
    }

    public Companion(String id, String name, int level, boolean active) {
        this(id, name, level, 0L, CompanionRarity.COMMON, EntityType.WOLF, active);
    }

    public Companion withActive(boolean value) { return new Companion(id, name, level, experience, rarity, entityType, value); }
    public Companion withName(String value) { return new Companion(id, value, level, experience, rarity, entityType, active); }
    public Companion withProgress(int newLevel, long newExperience) { return new Companion(id, name, newLevel, newExperience, rarity, entityType, active); }
    public Companion withDefinition(CompanionRarity newRarity, EntityType newEntityType) { return new Companion(id, name, level, experience, newRarity, newEntityType, active); }

    public CompanionInstance toInstance(UUID ownerUuid, CompanionEquipment equipment) {
        return new CompanionInstance(ownerUuid, id, level, experience, true, active, equipment == null ? CompanionEquipment.empty() : equipment.copy());
    }

    public static Companion fromInstance(CompanionInstance instance, CompanionDefinition definition) {
        return new Companion(instance.companionId(), definition.displayName(), instance.level(), instance.experience(), definition.rarity(), definition.visual().entityType(), instance.active());
    }
}
