package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.events.PlayerClassChangeEvent;
import de.pixelrpg.rpg.api.events.PlayerLevelUpEvent;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class RPGStatsListener implements Listener {

    private final StatEngine statEngine;
    private final PlayerProfileManager profileManager;

    public RPGStatsListener(StatEngine statEngine, PlayerProfileManager profileManager) {
        this.statEngine = statEngine;
        this.profileManager = profileManager;
    }

    // Aktualisiert die berechneten RPG-Werte beim Betreten des Servers nur für registrierte Spieler.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!profileManager.isRegistered(event.getPlayer().getUniqueId())) return;
        statEngine.recalculate(event.getPlayer());
    }

    // Aktualisiert die berechneten Werte nach einem normalen Levelaufstieg.
    @EventHandler
    public void onLevelUp(PlayerLevelUpEvent event) {
        statEngine.recalculate(event.getPlayer());
    }

    // Aktualisiert die berechneten Werte nach einem Klassenwechsel.
    @EventHandler
    public void onClassChange(PlayerClassChangeEvent event) {
        statEngine.recalculate(event.getPlayer());
    }
}
