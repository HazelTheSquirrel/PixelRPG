package de.pixelrpg.rpg.progression;

import de.pixelrpg.rpg.api.events.PlayerLevelUpEvent;
import de.pixelrpg.rpg.api.events.PlayerRegistrationEvent;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects character-level timing telemetry without changing progression values.
 *
 * The measured time is accumulated per level in the persistent player statistics.
 * This intentionally measures real session time instead of deriving level duration
 * from XP assumptions.
 */
public final class CharacterProgressionTelemetry implements Listener {
    private static final String LEVEL_TIME_PREFIX = "rp.character.level.";
    private static final String LEVEL_TIME_SUFFIX = ".time-millis";

    private final PlayerProfileManager profileManager;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public CharacterProgressionTelemetry(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    // Zuständig für den Start der Charakter-Levelzeitmessung beim Serverbeitritt.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        start(event.getPlayer());
    }

    // Zuständig für den Start der Charakter-Levelzeitmessung nach erfolgreicher Registrierung.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRegistration(PlayerRegistrationEvent event) {
        start(event.getPlayer());
    }

    // Zuständig für das Abschließen der aktuell gemessenen Charakter-Levelzeit beim Serveraustritt.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        flush(event.getPlayer().getUniqueId());
    }

    // Zuständig für das Abschließen der aktuellen Levelmessung und den Wechsel auf das neue Charakterlevel.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(PlayerLevelUpEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Session session = sessions.get(uuid);
        if (session == null) {
            start(event.getPlayer());
            session = sessions.get(uuid);
        }
        if (session == null) return;

        flush(uuid, session);
        sessions.put(uuid, new Session(event.getNewLevel(), System.currentTimeMillis()));
    }

    public void shutdown() {
        for (UUID uuid : sessions.keySet()) flush(uuid);
        sessions.clear();
    }

    private void start(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (!profileManager.isRegistered(uuid)) return;
        int level = Math.max(1, profileManager.getLevel(uuid));
        sessions.putIfAbsent(uuid, new Session(level, System.currentTimeMillis()));
    }

    private void flush(UUID uuid) {
        Session session = sessions.get(uuid);
        if (session != null) flush(uuid, session);
        sessions.remove(uuid, session);
    }

    private void flush(UUID uuid, Session session) {
        if (session == null) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - session.startedAtMillis());
        if (elapsed == 0L) return;

        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        if (profile == null || !profile.isRegistered()) return;

        String key = LEVEL_TIME_PREFIX + session.level() + LEVEL_TIME_SUFFIX;
        profile.incrementStatistic(key, elapsed);
    }

    private record Session(int level, long startedAtMillis) {
    }
}
