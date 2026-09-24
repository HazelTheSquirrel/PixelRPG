package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.core.StatisticType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MobKillStatisticListener implements Listener {
    private final GuildAPI guild; private final StatisticsAPI statistics;
    public MobKillStatisticListener(GuildAPI guild,StatisticsAPI statistics){this.guild=guild;this.statistics=statistics;}
    // Zuständig für das Hochzählen der MOBS_KILLED-Statistik bei registrierten Spielern.
    @EventHandler(priority=EventPriority.MONITOR)
    public void onMonsterDeath(EntityDeathEvent event){if(!(event.getEntity() instanceof Monster))return;Player killer=event.getEntity().getKiller();if(killer!=null&&guild.isRegistered(killer.getUniqueId()))statistics.recordStatistic(killer.getUniqueId(),StatisticType.MOBS_KILLED,1L);}
}
