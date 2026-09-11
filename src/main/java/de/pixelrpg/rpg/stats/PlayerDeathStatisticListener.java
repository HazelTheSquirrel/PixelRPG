// src/main/java/de/pixelrpg/rpg/stats/PlayerDeathStatisticListener.java
package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.GuildAPI;
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

    private final PlayerProfileManager profileManager;
    private final StatisticsAPI statisticsAPI;

    public PlayerDeathStatisticListener(GuildAPI guildAPI, StatisticsAPI statisticsAPI) {
        if (!(guildAPI instanceof PlayerProfileManager manager)) {
            throw new IllegalArgumentException("PlayerDeathStatisticListener requires PlayerProfileManager");
        }
        this.profileManager = manager;
        this.statisticsAPI = statisticsAPI;
    }

    // Zuständig für das Hochzählen der DEATHS-Statistik und das Amboss-Easteregg bei registrierten Spielern.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        var player = event.getEntity();
        var uuid = player.getUniqueId();
        if (!profileManager.isRegistered(uuid)) {
            return;
        }
        statisticsAPI.recordStatistic(uuid, StatisticType.DEATHS, 1L);

        if (event.getDamageSource().getDamageType() != DamageType.FALLING_ANVIL) {
            return;
        }

        profileManager.getProfile(uuid).ifPresent(profile -> {
            if (profile.getLevel() <= 1) {
                return;
            }
            profile.setExperience(0L);
            profileManager.saveProfileAsync(uuid);
            player.sendMessage(Component.text("Der Amboss war wohl zu schwer. Dein Level wurde auf 1 zurückgesetzt.", NamedTextColor.GOLD));
        });
    }
}
