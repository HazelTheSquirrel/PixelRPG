package de.pixelrpg.rpg.scoreboard;

import de.pixelrpg.rpg.api.events.PlayerRegistrationEvent;
import de.pixelrpg.rpg.api.events.PlayerUnregistrationEvent;
import de.pixelrpg.rpg.core.WakeScheduler;
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

/** Tracks playtime with per-player staggered one-shot saves instead of synchronized global save bursts. */
public final class PlaytimeTracker implements Listener {
    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    private final WakeScheduler<UUID> flushScheduler;
    private long intervalTicks = 6000L;

    public PlaytimeTracker(Plugin plugin, PlayerProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.flushScheduler = new WakeScheduler<>(plugin);
    }

    public void startAutosaveTask(int intervalTicks) {
        intervalTicks = Math.max(20, intervalTicks);
        this.intervalTicks = intervalTicks;
        for (Player player : Bukkit.getOnlinePlayers()) scheduleNext(player.getUniqueId(), staggerDelay(player.getUniqueId()));
    }

    public void shutdown() {
        flushScheduler.clear();
        for (UUID uuid : sessionStart.keySet()) flush(uuid);
        sessionStart.clear();
    }

    // Zuständig für den Start einer Spielzeit-Sitzung registrierter Spieler.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!profileManager.isRegistered(player.getUniqueId())) return;
        UUID uuid = player.getUniqueId();
        sessionStart.put(uuid, System.currentTimeMillis());
        scheduleNext(uuid, staggerDelay(uuid));
    }

    // Zuständig für die Speicherung der Spielzeit beim Verlassen des Servers.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        flushScheduler.cancel(uuid);
        flush(uuid);
        sessionStart.remove(uuid);
    }

    // Zuständig dafür, dass die Spielzeit nach einer erfolgreichen PixelRPG-Registrierung ab diesem Moment zählt.
    @EventHandler
    public void onRegistration(PlayerRegistrationEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        sessionStart.put(uuid, System.currentTimeMillis());
        scheduleNext(uuid, staggerDelay(uuid));
    }

    // Zuständig dafür, dass beim Abmelden von PixelRPG die bis dahin gesammelte Spielzeit gespeichert wird.
    @EventHandler
    public void onUnregistration(PlayerUnregistrationEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        flushScheduler.cancel(uuid);
        flush(uuid);
        sessionStart.remove(uuid);
    }

    /** Compatibility/manual entry point: flushes all active sessions immediately. */
    public void flushAll() {
        for (UUID uuid : sessionStart.keySet()) flush(uuid);
    }

    private void scheduleNext(UUID uuid, long delayTicks) {
        if (uuid == null || !sessionStart.containsKey(uuid)) return;
        flushScheduler.wakeLater(uuid, Math.max(1L, delayTicks), () -> {
            if (!sessionStart.containsKey(uuid)) return;
            flush(uuid);
            if (sessionStart.containsKey(uuid)) scheduleNext(uuid, intervalTicks);
        });
    }

    private long staggerDelay(UUID uuid) {
        long spread = Math.max(1L, intervalTicks);
        return Math.max(1L, Math.floorMod((long) uuid.hashCode(), spread));
    }

    private void flush(UUID uuid) {
        Long start = sessionStart.get(uuid);
        if (start == null || !profileManager.isRegistered(uuid)) return;
        long delta = System.currentTimeMillis() - start;
        if (delta <= 0L) return;
        profileManager.getProfile(uuid).ifPresent(profile -> {
            if (!profile.isRegistered()) return;
            profile.addPlaytimeMillis(delta);
            sessionStart.put(uuid, System.currentTimeMillis());
            profileManager.saveProfileAsync(uuid);
        });
    }
}
