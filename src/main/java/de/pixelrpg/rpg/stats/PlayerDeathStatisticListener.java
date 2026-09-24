package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.core.StatisticType;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.damage.DamageType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerDeathStatisticListener implements Listener {
    private final PlayerProfileManager profiles; private final StatisticsAPI statistics;
    public PlayerDeathStatisticListener(PlayerProfileManager profiles,StatisticsAPI statistics){this.profiles=profiles;this.statistics=statistics;}
    // Zuständig für das Hochzählen der DEATHS-Statistik und das Amboss-Easteregg bei registrierten Spielern.
    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event){var player=event.getEntity();if(!profiles.isRegistered(player.getUniqueId()))return;statistics.recordStatistic(player.getUniqueId(),StatisticType.DEATHS,1L);if(event.getDamageSource().getDamageType()!=DamageType.FALLING_ANVIL)return;var profile=profiles.getProfile(player.getUniqueId()).orElse(null);if(profile==null||profile.getLevel()<=1)return;profile.setExperience(0L);profiles.saveProfileAsync(player.getUniqueId());player.sendMessage(Component.text("Der Amboss war wohl zu schwer. Dein Level wurde auf 1 zurückgesetzt.",NamedTextColor.GOLD));}
}
