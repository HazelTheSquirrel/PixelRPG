// src/main/java/de/pixelrpg/rpg/player/TitleDisplayJoinListener.java
package de.pixelrpg.rpg.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class TitleDisplayJoinListener implements Listener {

    private final TitleDisplayService titleDisplayService;

    public TitleDisplayJoinListener(TitleDisplayService titleDisplayService) {
        this.titleDisplayService = titleDisplayService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        titleDisplayService.apply(event.getPlayer());
    }
}