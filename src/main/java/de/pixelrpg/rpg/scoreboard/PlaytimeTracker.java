// src/main/java/de/pixelrpg/rpg/scoreboard/PlaytimeTracker.java
package de.pixelrpg.rpg.scoreboard;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaytimeTracker implements Listener {

    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();

    public PlaytimeTracker(Plugin plugin, PlayerProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
    }

    public void startAutosaveTask(int intervalTicks) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::flushAll, intervalTicks, intervalTicks);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sessionStart.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flush(event.getPlayer().getUniqueId());
        sessionStart.remove(event.getPlayer().getUniqueId());
    }

    public void flushAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            flush(player.getUniqueId());
            sessionStart.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    private void flush(UUID uuid) {
        Long start = sessionStart.get(uuid);
        if (start == null) {
            return;
        }
        long delta = System.currentTimeMillis() - start;
        if (delta <= 0L) {
            return;
        }

        profileManager.getProfile(uuid).ifPresent(profile -> {
            profile.addPlaytimeMillis(delta);
            profileManager.saveProfileAsync(uuid);
        });
    }
}