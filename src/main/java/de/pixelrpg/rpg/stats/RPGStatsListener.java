// src/main/java/de/pixelrpg/rpg/stats/RPGStatsListener.java
package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.events.PlayerClassChangeEvent;
import de.pixelrpg.rpg.api.events.PlayerLevelUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class RPGStatsListener implements Listener {

    private final StatEngine statEngine;

    public RPGStatsListener(StatEngine statEngine) {
        this.statEngine = statEngine;
    }

    // Aktualisiert die berechneten Werte beim Betreten des Servers.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
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