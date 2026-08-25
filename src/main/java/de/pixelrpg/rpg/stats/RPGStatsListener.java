package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.events.PlayerLevelUpEvent;
import de.pixelrpg.rpg.api.events.PlayerRegistrationEvent;
import de.pixelrpg.rpg.api.events.PlayerUnregistrationEvent;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

public final class RPGStatsListener implements Listener {
    private final StatEngine statEngine;
    private final PlayerProfileManager profileManager;
    private final BukkitTask companionSyncTask;

    public RPGStatsListener(StatEngine statEngine, PlayerProfileManager profileManager) {
        this.statEngine = statEngine;
        this.profileManager = profileManager;
        this.companionSyncTask = Bukkit.getScheduler().runTaskTimer(
                de.pixelrpg.rpg.PixelRPGPlugin.getInstance(),
                this::refreshOnlineStats,
                5L,
                5L);
    }

    // Aktualisiert die berechneten RPG-Werte beim Betreten des Servers nur für registrierte Spieler.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!profileManager.isRegistered(event.getPlayer().getUniqueId())) return;
        statEngine.recalculate(event.getPlayer());
    }

    // Aktiviert die RPG-Stats unmittelbar nach erfolgreicher PixelRPG-Registrierung.
    @EventHandler
    public void onRegistration(PlayerRegistrationEvent event) {
        statEngine.recalculate(event.getPlayer());
    }

    // Entfernt RPG-Stat-Modifikatoren unmittelbar nach der Abmeldung von PixelRPG.
    @EventHandler
    public void onUnregistration(PlayerUnregistrationEvent event) {
        statEngine.clear(event.getPlayer());
    }

    // Aktualisiert die berechneten Werte nach einem normalen Levelaufstieg.
    @EventHandler
    public void onLevelUp(PlayerLevelUpEvent event) {
        statEngine.recalculate(event.getPlayer());
    }

    // Aktualisiert die berechneten Werte, sobald sich die Ausrüstung eines registrierten Spielers ändert.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquipmentChanged(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!profileManager.isRegistered(player.getUniqueId())) return;
        statEngine.recalculate(player);
    }

    private void refreshOnlineStats() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (profileManager.isRegistered(player.getUniqueId())) statEngine.recalculate(player);
        }
    }
}
