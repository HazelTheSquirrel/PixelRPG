package de.pixelrpg.rpg.guild;

import java.util.Objects;
import java.util.UUID;
import de.pixelrpg.rpg.economy.Money;

/** Immutable guild identity, leadership and treasury state. */
public record Guild(
        UUID id,
        String name,
        UUID leaderId,
        UUID deputyId,
        int memberCount,
        long treasuryMinorUnits,
        UUID cityRegionId
) {
    public static final int MAX_MEMBERS = 50;
    public static final int CREATION_COST_GOLD = 2_500;
    public static final int MIN_CREATION_LEVEL = 20;

    public Guild(UUID id, String name, UUID leaderId, int memberCount) {
        this(id, name, leaderId, null, memberCount, 0L, null);
    }

    public Guild(UUID id, String name, UUID leaderId, int memberCount, long treasuryMinorUnits) {
        this(id, name, leaderId, null, memberCount, treasuryMinorUnits, null);
    }

    public Guild(UUID id, String name, UUID leaderId, int memberCount, long treasuryMinorUnits, UUID cityRegionId) {
        this(id, name, leaderId, null, memberCount, treasuryMinorUnits, cityRegionId);
    }

    public Guild {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(leaderId, "leaderId");
        if (deputyId != null && deputyId.equals(leaderId)) {
            throw new IllegalArgumentException("deputyId must differ from leaderId");
        }
        if (treasuryMinorUnits < 0L) throw new IllegalArgumentException("treasuryMinorUnits must be non-negative");
        if (memberCount < 1 || memberCount > MAX_MEMBERS) {
            throw new IllegalArgumentException("memberCount must be between 1 and " + MAX_MEMBERS);
        }
    }

    public double treasury() {
        return Money.toMajor(treasuryMinorUnits);
    }

    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }

    public boolean isDeputy(UUID playerId) {
        return deputyId != null && deputyId.equals(playerId);
    }

    public boolean canManageTerritory(UUID playerId) {
        return isLeader(playerId) || isDeputy(playerId);
    }
}
