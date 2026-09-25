package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.events.PlayerRegistrationEvent;
import de.pixelrpg.rpg.api.events.PlayerUnregistrationEvent;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.WakeScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/** Event-driven quest/navigation runtime; there is no permanent passive polling loop. */
public final class QuestPassiveCheckTask implements Listener {
    private final Plugin plugin;
    private final QuestManager questManager;
    private final WakeScheduler<UUID> wakeScheduler;
    private BukkitTask throttledTask;
    private QuestInventoryTracker inventoryTracker;
    private boolean started;

    public QuestPassiveCheckTask(Plugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.wakeScheduler = new WakeScheduler<>(plugin);
    }

    public void start() {
        if (started) return;
        started = true;
        questManager.setQuestStateChangeListener(this::wake);
        inventoryTracker = new QuestInventoryTracker(questManager);
        plugin.getServer().getPluginManager().registerEvents(inventoryTracker, plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        throttledTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::processOnlinePlayers, 20L, 20L);
        for (Player player : Bukkit.getOnlinePlayers()) wake(player);
    }

    /** Wakes one player after a relevant quest/navigation state change. */
    public void wake(Player player) {
        if (player == null) return;
        wakeScheduler.wake(player.getUniqueId(), () -> process(player.getUniqueId()));
    }

    /** Wakes one player after a relevant quest/navigation state change. */
    public void wake(UUID playerId) {
        if (playerId == null) return;
        wakeScheduler.wake(playerId, () -> process(playerId));
    }

    // A player joining the server restores persisted quest timers and coordinate targets once.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        questManager.restoreTimers(event.getPlayer());
        questManager.restoreQuestCoordinates(event.getPlayer());
        wake(event.getPlayer());
    }

    // Coordinate objective checks are throttled to one pass per second.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Coordinate targets are persisted once; movement only needs the throttled X/Z check.
    }

    // A world change requires another throttled coordinate-objective check.
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        wake(event.getPlayer());
    }

    // Registration enables quest processing for the player and therefore wakes its state once.
    @EventHandler
    public void onRegistration(PlayerRegistrationEvent event) {
        questManager.restoreQuestCoordinates(event.getPlayer());
        wake(event.getPlayer());
    }

    // Unregistration removes quest processing state from the player.
    @EventHandler
    public void onUnregistration(PlayerUnregistrationEvent event) {
        wake(event.getPlayer());
    }

    // Quest completion can change the active quest state.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        wake(event.getPlayer());
    }

    // Disconnecting a player cancels any queued quest work and drops its movement state.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        wakeScheduler.cancel(uuid);
    }

    private void process(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;
        questManager.checkReachLocationQuests(player);
    }

    private void processOnlinePlayers() {
        if (!started) return;
        for (Player player : Bukkit.getOnlinePlayers()) process(player.getUniqueId());
    }

    public void clear(Player player) {
        if (player == null) return;
        wakeScheduler.cancel(player.getUniqueId());
    }

    public void stop() {
        if (!started) return;
        wakeScheduler.clear();
        if (throttledTask != null) {
            throttledTask.cancel();
            throttledTask = null;
        }
        if (inventoryTracker != null) {
            inventoryTracker.shutdown();
            HandlerList.unregisterAll(inventoryTracker);
        }
        HandlerList.unregisterAll(this);
        inventoryTracker = null;
        questManager.setQuestStateChangeListener(null);
        started = false;
    }
}
