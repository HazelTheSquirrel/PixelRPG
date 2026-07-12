// src/main/java/de/pixelrpg/rpg/api/events/DungeonClearedEvent.java
package de.pixelrpg.rpg.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;
import java.util.UUID;

public final class DungeonClearedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String dungeonId;
    private final Set<UUID> participants;

    public DungeonClearedEvent(String dungeonId, Set<UUID> participants) {
        this.dungeonId = dungeonId;
        this.participants = participants;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}