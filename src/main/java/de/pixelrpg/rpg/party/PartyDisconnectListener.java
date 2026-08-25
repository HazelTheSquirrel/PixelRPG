package de.pixelrpg.rpg.party;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PartyDisconnectListener implements Listener {

    private final PartyManager partyManager;

    public PartyDisconnectListener(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    // Zuständig für das Entfernen eines Spielers aus Party und ausstehenden Einladungen beim Verlassen des Servers.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        partyManager.handleDisconnect(event.getPlayer().getUniqueId());
    }
}