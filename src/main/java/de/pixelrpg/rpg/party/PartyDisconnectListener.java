package de.pixelrpg.rpg.party;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PartyDisconnectListener implements Listener {
    private final PartyManager partyManager;
    public PartyDisconnectListener(PartyManager partyManager) { this.partyManager = partyManager; }

    // Cleans party invitations and schedules empty-party cleanup on disconnect.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        partyManager.handleDisconnect(event.getPlayer().getUniqueId());
    }
}
