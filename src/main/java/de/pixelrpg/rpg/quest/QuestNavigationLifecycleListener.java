package de.pixelrpg.rpg.quest;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class QuestNavigationLifecycleListener implements Listener {

    private final QuestPassiveCheckTask passiveCheckTask;

    public QuestNavigationLifecycleListener(QuestPassiveCheckTask passiveCheckTask) {
        this.passiveCheckTask = passiveCheckTask;
    }

    // Zuständig für das sofortige Entfernen der persönlichen Quest-Locator-Wegpunkte beim Logout.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        passiveCheckTask.clear(event.getPlayer());
    }
}
