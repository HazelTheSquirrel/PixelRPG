// src/main/java/de/pixelrpg/rpg/api/events/PlayerRankUpEvent.java
package de.pixelrpg.rpg.api.events;

import de.pixelrpg.rpg.core.Rank;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerRankUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Rank previousRank;
    private final Rank newRank;

    public PlayerRankUpEvent(Player player, Rank previousRank, Rank newRank) {
        this.player = player;
        this.previousRank = previousRank;
        this.newRank = newRank;
    }

    public Player getPlayer() {
        return player;
    }

    public Rank getPreviousRank() {
        return previousRank;
    }

    public Rank getNewRank() {
        return newRank;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}