// src/main/java/de/pixelrpg/rpg/stats/RPGStatsListener.java
package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.events.PlayerClassChangeEvent;
import de.pixelrpg.rpg.api.events.PlayerRankUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class RPGStatsListener implements Listener {

    private final StatEngine statEngine;

    public RPGStatsListener(StatEngine statEngine) {
        this.statEngine = statEngine;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        statEngine.recalculate(event.getPlayer());
    }

    @EventHandler
    public void onRankUp(PlayerRankUpEvent event) {
        statEngine.recalculate(event.getPlayer());
    }

    @EventHandler
    public void onClassChange(PlayerClassChangeEvent event) {
        statEngine.recalculate(event.getPlayer());
    }
}