package de.pixelrpg.rpg.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerProfileLifecycleListener implements Listener {
    private final PlayerProfileManager profiles;

    public PlayerProfileLifecycleListener(PlayerProfileManager profiles) { this.profiles = profiles; }

    /** Loads the authoritative profile before the player is allowed to join. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (profiles.loadForPreLogin(event.getUniqueId()) == PlayerProfileManager.LoadOutcome.FAILED) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("PixelRPG profile could not be loaded. Please try again later.", NamedTextColor.RED));
        }
    }

    /** Activates the already loaded profile after the player joins. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        profiles.activateOnJoin(event.getPlayer().getUniqueId());
    }

    /** Persists and releases the profile when the player disconnects. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        profiles.deactivateOnQuit(event.getPlayer().getUniqueId());
    }
}
