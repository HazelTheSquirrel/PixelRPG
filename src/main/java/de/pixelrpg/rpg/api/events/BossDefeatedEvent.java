// src/main/java/de/pixelrpg/rpg/api/events/BossDefeatedEvent.java
package de.pixelrpg.rpg.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;
import java.util.UUID;

public final class BossDefeatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String bossId;
    private final Set<UUID> participants;

    public BossDefeatedEvent(String bossId, Set<UUID> participants) {
        this.bossId = bossId;
        this.participants = participants;
    }

    public String getBossId() {
        return bossId;
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