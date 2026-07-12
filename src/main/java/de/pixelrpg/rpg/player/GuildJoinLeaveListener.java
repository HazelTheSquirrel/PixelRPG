// src/main/java/de/pixelrpg/rpg/player/GuildJoinLeaveListener.java
package de.pixelrpg.rpg.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class GuildJoinLeaveListener implements Listener {

    private final PlayerProfileManager profileManager;

    public GuildJoinLeaveListener(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        profileManager.loadForPreLogin(event.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profileManager.activateOnJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profileManager.deactivateOnQuit(event.getPlayer().getUniqueId());
    }
}