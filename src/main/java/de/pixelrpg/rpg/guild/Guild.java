package de.pixelrpg.rpg.guild;

import java.util.Objects;
import java.util.UUID;
import de.pixelrpg.rpg.economy.Money;

/** Immutable guild identity and membership limits. */
public record Guild(UUID id, String name, UUID leaderId, int memberCount, long treasuryMinorUnits) {
    public static final int MAX_MEMBERS = 50;
    public static final int CREATION_COST_GOLD = 2_500;
    public static final int MIN_CREATION_LEVEL = 20;

    public Guild(UUID id, String name, UUID leaderId, int memberCount) {
        this(id, name, leaderId, memberCount, 0L);
    }

    public Guild {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(leaderId, "leaderId");
        if (treasuryMinorUnits < 0L) throw new IllegalArgumentException("treasuryMinorUnits must be non-negative");
        if (memberCount < 1 || memberCount > MAX_MEMBERS) {
            throw new IllegalArgumentException("memberCount must be between 1 and " + MAX_MEMBERS);
        }
    }

    public double treasury() { return Money.toMajor(treasuryMinorUnits); }

    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }
}
