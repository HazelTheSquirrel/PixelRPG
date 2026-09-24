package de.pixelrpg.rpg.party;

import de.pixelrpg.rpg.api.PartyAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read-only party API facade used by external integrations until the full party application layer is attached.
 * Runtime membership is intentionally kept by UUID and never by live server objects.
 */
public final class PartyService implements PartyAPI {
    private final Map<UUID, Set<UUID>> members = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> leaders = new ConcurrentHashMap<>();
    private final double range;

    public PartyService(double range) {
        this.range = Math.max(0.0D, range);
    }

    public boolean isInParty(UUID id) {
        return members.containsKey(id);
    }

    public Set<UUID> getPartyMembers(UUID id) {
        Set<UUID> partyMembers = members.get(id);
        return partyMembers == null ? Set.of(id) : Set.copyOf(partyMembers);
    }

    public UUID getPartyLeader(UUID id) {
        return leaders.getOrDefault(id, id);
    }

    public boolean isLeader(UUID id) {
        return id != null && id.equals(leaders.get(id));
    }

    public boolean isWithinShareRange(UUID source, UUID target) {
        if (source == null || target == null) {
            return false;
        }
        Player sourcePlayer = Bukkit.getPlayer(source);
        Player targetPlayer = Bukkit.getPlayer(target);
        if (sourcePlayer == null || targetPlayer == null
                || !sourcePlayer.isOnline() || !targetPlayer.isOnline()
                || !sourcePlayer.getWorld().equals(targetPlayer.getWorld())) {
            return false;
        }
        return sourcePlayer.getLocation().distanceSquared(targetPlayer.getLocation()) <= range * range;
    }

    public double getShareRange() {
        return range;
    }

    public void bindParty(UUID leader, Set<UUID> partyMembers) {
        if (leader == null || partyMembers == null || partyMembers.isEmpty()) {
            throw new IllegalArgumentException("A party requires a leader and at least one member.");
        }
        Set<UUID> snapshot = Set.copyOf(partyMembers);
        if (!snapshot.contains(leader)) {
            throw new IllegalArgumentException("The leader must be a party member.");
        }
        for (UUID member : snapshot) {
            members.put(member, snapshot);
            leaders.put(member, leader);
        }
    }

    public void clearParty(UUID member) {
        Set<UUID> partyMembers = members.remove(member);
        if (partyMembers == null) {
            return;
        }
        for (UUID partyMember : partyMembers) {
            members.remove(partyMember, partyMembers);
            leaders.remove(partyMember);
        }
    }
}
