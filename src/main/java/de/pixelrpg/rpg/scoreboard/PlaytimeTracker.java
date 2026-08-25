package de.pixelrpg.rpg.scoreboard;

import de.pixelrpg.rpg.api.events.PlayerRegistrationEvent;
import de.pixelrpg.rpg.api.events.PlayerUnregistrationEvent;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaytimeTracker implements Listener {
    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    private BukkitTask autosaveTask;

    public PlaytimeTracker(Plugin plugin, PlayerProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
    }

    public void startAutosaveTask(int intervalTicks) {
        if (autosaveTask != null) return;
        autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::flushAll, intervalTicks, intervalTicks);
    }

    public void shutdown() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        flushAll();
        sessionStart.clear();
    }

    // Zuständig für den Start einer Spielzeit-Sitzung registrierter Spieler.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (profileManager.isRegistered(player.getUniqueId())) sessionStart.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // Zuständig für die Speicherung der Spielzeit beim Verlassen des Servers.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flush(event.getPlayer().getUniqueId());
        sessionStart.remove(event.getPlayer().getUniqueId());
    }

    // Zuständig dafür, dass die Spielzeit nach einer erfolgreichen PixelRPG-Registrierung ab diesem Moment zählt.
    @EventHandler
    public void onRegistration(PlayerRegistrationEvent event) {
        sessionStart.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    // Zuständig dafür, dass beim Abmelden von PixelRPG die bis dahin gesammelte Spielzeit gespeichert wird.
    @EventHandler
    public void onUnregistration(PlayerUnregistrationEvent event) {
        flush(event.getPlayer().getUniqueId());
        sessionStart.remove(event.getPlayer().getUniqueId());
    }

    public void flushAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            flush(player.getUniqueId());
            if (profileManager.isRegistered(player.getUniqueId())) sessionStart.put(player.getUniqueId(), System.currentTimeMillis());
            else sessionStart.remove(player.getUniqueId());
        }
    }

    private void flush(UUID uuid) {
        Long start = sessionStart.get(uuid);
        if (start == null || !profileManager.isRegistered(uuid)) return;
        long delta = System.currentTimeMillis() - start;
        if (delta <= 0L) return;
        profileManager.getProfile(uuid).ifPresent(profile -> {
            if (!profile.isRegistered()) return;
            profile.addPlaytimeMillis(delta);
            profileManager.saveProfileAsync(uuid);
        });
    }
}
