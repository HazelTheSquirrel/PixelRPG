// src/main/java/de/pixelrpg/rpg/party/Party.java
package de.pixelrpg.rpg.party;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {

    public static final int MAX_MEMBERS = 9;

    private final UUID id;
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();

    public Party(UUID id, UUID leader) {
        this.id = id;
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean isFull() {
        return members.size() >= MAX_MEMBERS;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean addMember(UUID uuid) {
        if (isFull() || isMember(uuid)) {
            return false;
        }
        return members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public UUID promoteNextLeader() {
        UUID next = members.stream().filter(uuid -> !uuid.equals(leader)).findFirst().orElse(null);
        if (next != null) {
            leader = next;
        }
        return next;
    }
}