// src/main/java/de/pixelrpg/rpg/stats/PlayerDeathStatisticListener.java
package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.core.StatisticType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerDeathStatisticListener implements Listener {

    private final GuildAPI guildAPI;
    private final StatisticsAPI statisticsAPI;

    public PlayerDeathStatisticListener(GuildAPI guildAPI, StatisticsAPI statisticsAPI) {
        this.guildAPI = guildAPI;
        this.statisticsAPI = statisticsAPI;
    }

    // Zuständig für das Hochzählen der DEATHS-Statistik bei registrierten Spielern.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        var uuid = event.getEntity().getUniqueId();
        if (!guildAPI.isRegistered(uuid)) {
            return;
        }
        statisticsAPI.recordStatistic(uuid, StatisticType.DEATHS, 1L);
    }
}