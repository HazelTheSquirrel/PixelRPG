package de.pixelrpg.rpg.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a registered player enters the RPG combat state. */
public final class PlayerCombatEnterEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public PlayerCombatEnterEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
