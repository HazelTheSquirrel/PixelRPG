package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.events.PlayerClassChangeEvent;
import de.pixelrpg.rpg.api.events.PlayerJoinGuildEvent;
import de.pixelrpg.rpg.api.events.PlayerLeaveGuildEvent;
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

    // Aktiviert die RPG-Stats unmittelbar nach erfolgreicher PixelRPG-Registrierung.
    @EventHandler
    public void onGuildJoin(PlayerJoinGuildEvent event) {
        statEngine.recalculate(event.getPlayer());
    }

    // Entfernt RPG-Stat-Modifikatoren unmittelbar nach dem Verlassen von PixelRPG.
    @EventHandler
    public void onGuildLeave(PlayerLeaveGuildEvent event) {
        statEngine.clear(event.getPlayer());
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
