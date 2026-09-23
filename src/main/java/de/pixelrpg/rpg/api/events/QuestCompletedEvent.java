package de.pixelrpg.rpg.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class QuestCompletedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String questId;

    public QuestCompletedEvent(UUID playerId, String questId) {
        this.playerId = playerId;
        this.questId = questId;
    }

    public UUID playerId() { return playerId; }
    public String questId() { return questId; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
