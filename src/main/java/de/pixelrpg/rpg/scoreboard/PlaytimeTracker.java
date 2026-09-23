package de.pixelrpg.rpg.scoreboard;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaytimeTracker implements Listener, AutoCloseable {
    private final PlayerProfileManager profiles;
    private final Map<UUID, Long> sessions = new ConcurrentHashMap<>();

    public PlaytimeTracker(Plugin plugin, PlayerProfileManager profiles) {
        this.profiles = profiles;
    }

    // Startet die Spielzeitmessung für einen registrierten Spieler.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (profiles.isRegistered(player.getUniqueId())) sessions.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // Überträgt die aktuelle Sitzung beim Verlassen in den persistenten Profilzustand.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flush(event.getPlayer().getUniqueId());
    }

    public void flush(UUID playerId) {
        Long started = sessions.remove(playerId);
        if (started == null) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - started);
        if (elapsed == 0L) return;
        profiles.getProfile(playerId).ifPresent(profile -> {
            profile.addPlaytimeMillis(elapsed);
            profiles.saveProfileAsync(playerId);
        });
    }

    public void flushAll() {
        for (UUID uuid : sessions.keySet()) flush(uuid);
    }

    @Override
    public void close() {
        flushAll();
    }
}
