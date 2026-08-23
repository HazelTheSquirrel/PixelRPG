package de.pixelrpg.rpg.companion;

import java.util.Objects;
import java.util.UUID;

/** Player-owned mutable companion state; static definition data is never stored here. */
public record CompanionInstance(
        UUID ownerUuid,
        String companionId,
        int level,
        long experience,
        boolean unlocked,
        boolean active,
        CompanionEquipment equipment
) {
    public CompanionInstance {
        Objects.requireNonNull(ownerUuid, "ownerUuid");
        Objects.requireNonNull(companionId, "companionId");
        Objects.requireNonNull(equipment, "equipment");
        if (level < 1) throw new IllegalArgumentException("level must be at least 1");
        if (experience < 0L) throw new IllegalArgumentException("experience must not be negative");
    }

    public CompanionInstance withActive(boolean value) {
        return new CompanionInstance(ownerUuid, companionId, level, experience, unlocked, value, equipment);
    }

    public CompanionInstance withProgress(int newLevel, long newExperience) {
        return new CompanionInstance(ownerUuid, companionId, newLevel, newExperience, unlocked, active, equipment);
    }

    public CompanionInstance withEquipment(CompanionEquipment value) {
        return new CompanionInstance(ownerUuid, companionId, level, experience, unlocked, active, value == null ? CompanionEquipment.empty() : value.copy());
    }
}
