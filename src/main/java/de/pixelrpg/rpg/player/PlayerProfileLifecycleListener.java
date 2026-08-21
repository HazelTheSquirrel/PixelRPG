package de.pixelrpg.rpg.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/** Loads, activates and persists PlayerProfile instances across the player lifecycle. */
public final class PlayerProfileLifecycleListener implements Listener {
    private final PlayerProfileManager profileManager;

    public PlayerProfileLifecycleListener(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    /** Loads the player's persistent profile before the player is allowed to join. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerProfileManager.LoadOutcome outcome = profileManager.loadForPreLogin(event.getUniqueId());
        if (outcome == PlayerProfileManager.LoadOutcome.FAILED) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    org.bukkit.ChatColor.RED + "PixelRPG profile could not be loaded. Please try again later.");
        }
    }

    /** Activates the already loaded profile for the joined player. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        profileManager.activateOnJoin(event.getPlayer());
    }

    /** Deactivates and queues the player's profile for persistent saving after disconnect. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        profileManager.deactivateOnQuit(uuid);
    }
}
