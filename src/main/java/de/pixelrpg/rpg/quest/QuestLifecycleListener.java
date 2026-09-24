package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class QuestLifecycleListener implements Listener {
    private final QuestService quests;
    private final PlayerProfileManager profiles;

    public QuestLifecycleListener(QuestService quests, PlayerProfileManager profiles) {
        this.quests = quests;
        this.profiles = profiles;
    }

    /** Restores persistent quest timers and inventory-based objectives after the profile is active. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        quests.restoreTimers(event.getPlayer());
        quests.checkCollect(event.getPlayer());
    }

    /** Persists current quest progress before the player profile is deactivated. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        profiles.saveProfileAsync(event.getPlayer().getUniqueId());
    }
}
