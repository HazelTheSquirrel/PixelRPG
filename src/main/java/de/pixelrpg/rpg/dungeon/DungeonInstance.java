// src/main/java/de/pixelrpg/rpg/dungeon/DungeonInstance.java
package de.pixelrpg.rpg.dungeon;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DungeonInstance {

    private final UUID instanceId;
    private final String dungeonId;
    private final Location origin;
    private final Set<UUID> partyMembers;
    private final Set<UUID> spawnedEntities = new HashSet<>();
    private boolean bossDefeated;

    public DungeonInstance(UUID instanceId, String dungeonId, Location origin, Set<UUID> partyMembers) {
        this.instanceId = instanceId;
        this.dungeonId = dungeonId;
        this.origin = origin;
        this.partyMembers = partyMembers;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public Location getOrigin() {
        return origin;
    }

    public Set<UUID> getPartyMembers() {
        return partyMembers;
    }

    public Set<UUID> getSpawnedEntities() {
        return spawnedEntities;
    }

    public boolean isBossDefeated() {
        return bossDefeated;
    }

    public void setBossDefeated(boolean bossDefeated) {
        this.bossDefeated = bossDefeated;
    }
}