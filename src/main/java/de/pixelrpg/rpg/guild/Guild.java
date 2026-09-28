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
        UUID cityRegionId,
        int cityLevel,
        long cityUpgradeCooldownUntil,
        KingdomCombatMode combatMode,
        long combatModeChangeAt,
        long combatModeCooldownUntil
) {
    public static final int MAX_MEMBERS = 50;
    public static final int CREATION_COST_GOLD = 2_500;
    public static final int MIN_CREATION_LEVEL = 20;
    public static final int MAX_CITY_LEVEL = 10;

    public Guild(UUID id, String name, UUID leaderId, int memberCount) {
        this(id, name, leaderId, null, memberCount, 0L, null, 1, 0L, KingdomCombatMode.PVE, 0L, 0L);
    }

    public Guild(UUID id, String name, UUID leaderId, int memberCount, long treasuryMinorUnits) {
        this(id, name, leaderId, null, memberCount, treasuryMinorUnits, null, 1, 0L, KingdomCombatMode.PVE, 0L, 0L);
    }

    public Guild(UUID id, String name, UUID leaderId, int memberCount, long treasuryMinorUnits, UUID cityRegionId) {
        this(id, name, leaderId, null, memberCount, treasuryMinorUnits, cityRegionId, 1, 0L, KingdomCombatMode.PVE, 0L, 0L);
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

    public static double maxTerritoryArea(int level) {
        return switch (Math.clamp(level, 1, MAX_CITY_LEVEL)) {
            case 1 -> 1024.0D; case 2 -> 4096.0D; case 3 -> 9216.0D; case 4 -> 16384.0D; case 5 -> 25600.0D;
            case 6 -> 36864.0D; case 7 -> 57600.0D; case 8 -> 82944.0D; case 9 -> 112896.0D; case 10 -> 147456.0D;
            default -> 1024.0D;
        };
    }

    public static String cityName(int level) {
        return switch (Math.clamp(level, 1, MAX_CITY_LEVEL)) {
            case 1 -> "Lager"; case 2 -> "Außenposten"; case 3 -> "Weiler"; case 4 -> "Dorf"; case 5 -> "Stadt";
            case 6 -> "Großstadt"; case 7 -> "Regionalstadt"; case 8 -> "Provinzstadt"; case 9 -> "Residenzstadt"; case 10 -> "Metropole";
            default -> "Lager";
        };
    }
}
