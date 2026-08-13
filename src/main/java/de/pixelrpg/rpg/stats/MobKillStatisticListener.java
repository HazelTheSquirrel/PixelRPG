// src/main/java/de/pixelrpg/rpg/stats/MobKillStatisticListener.java
package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.core.StatisticType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MobKillStatisticListener implements Listener {

    private final GuildAPI guildAPI;
    private final StatisticsAPI statisticsAPI;

    public MobKillStatisticListener(GuildAPI guildAPI, StatisticsAPI statisticsAPI) {
        this.guildAPI = guildAPI;
        this.statisticsAPI = statisticsAPI;
    }

    // Zuständig für das Hochzählen der MOBS_KILLED-Statistik bei registrierten Spielern.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }

        Player killer = entity.getKiller();
        if (killer == null || !guildAPI.isRegistered(killer.getUniqueId())) {
            return;
        }

        statisticsAPI.recordStatistic(killer.getUniqueId(), StatisticType.MOBS_KILLED, 1L);
    }
}