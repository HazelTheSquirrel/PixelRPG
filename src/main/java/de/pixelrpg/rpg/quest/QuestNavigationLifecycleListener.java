package de.pixelrpg.rpg.quest;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Clears transient quest navigation state when a player disconnects. */
public final class QuestNavigationLifecycleListener implements Listener {
    private final QuestNavigationService navigationService;

    public QuestNavigationLifecycleListener(QuestNavigationService navigationService) {
        this.navigationService = navigationService;
    }

    /** Removes all temporary quest markers owned by the disconnecting player. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        navigationService.clear(event.getPlayer());
    }
}
