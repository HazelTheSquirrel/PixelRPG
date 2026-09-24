package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.events.PlayerLevelUpEvent;
import de.pixelrpg.rpg.api.events.PlayerRegistrationEvent;
import de.pixelrpg.rpg.api.events.PlayerUnregistrationEvent;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class RPGStatsListener implements Listener {
    private final StatEngine stats; private final PlayerProfileManager profiles;
    public RPGStatsListener(StatEngine stats,PlayerProfileManager profiles){this.stats=stats;this.profiles=profiles;}
    // Aktualisiert die berechneten RPG-Werte beim Betreten des Servers nur für registrierte Spieler.
    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event){if(profiles.isRegistered(event.getPlayer().getUniqueId()))stats.recalculate(event.getPlayer());}
    // Aktiviert die RPG-Stats unmittelbar nach erfolgreicher PixelRPG-Registrierung.
    @EventHandler public void onRegistration(PlayerRegistrationEvent event){stats.recalculate(event.getPlayer());}
    // Entfernt RPG-Stat-Modifikatoren unmittelbar nach der Abmeldung von PixelRPG.
    @EventHandler public void onUnregistration(PlayerUnregistrationEvent event){stats.clear(event.getPlayer());}
    // Aktualisiert die berechneten Werte nach einem normalen Levelaufstieg.
    @EventHandler public void onLevelUp(PlayerLevelUpEvent event){stats.recalculate(event.getPlayer());}
    // Aktualisiert die berechneten Werte, sobald sich die Ausrüstung eines registrierten Spielers ändert.
    @EventHandler(priority=EventPriority.MONITOR)
    public void onEquipmentChanged(EntityEquipmentChangedEvent event){if(event.getEntity() instanceof Player player&&profiles.isRegistered(player.getUniqueId()))stats.recalculate(player);}
}
