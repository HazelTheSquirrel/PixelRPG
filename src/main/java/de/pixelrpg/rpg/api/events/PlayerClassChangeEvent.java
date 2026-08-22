// src/main/java/de/pixelrpg/rpg/api/events/PlayerClassChangeEvent.java (VOLLSTÄNDIG, ersetzt alte Datei — kein Skill-Focus-Item mehr nötig)
package de.pixelrpg.rpg.api.events;

import de.pixelrpg.rpg.player.PlayerClass;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerClassChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlayerClass previousClass;
    private final PlayerClass newClass;

    public PlayerClassChangeEvent(Player player, PlayerClass previousClass, PlayerClass newClass) {
        this.player = player;
        this.previousClass = previousClass;
        this.newClass = newClass;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerClass getPreviousClass() {
        return previousClass;
    }

    public PlayerClass getNewClass() {
        return newClass;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}