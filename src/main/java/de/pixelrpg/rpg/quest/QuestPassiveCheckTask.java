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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Event-driven quest/navigation runtime; there is no permanent passive polling loop. */
public final class QuestPassiveCheckTask implements Listener {
    private final Plugin plugin;
    private final QuestManager questManager;
    private final WakeScheduler<UUID> wakeScheduler;
    private final Map<UUID, BlockPosition> lastBlockByPlayer = new ConcurrentHashMap<>();
    private QuestNavigationService navigationService;
    private QuestInventoryTracker inventoryTracker;
    private QuestNavigationLifecycleListener navigationLifecycleListener;
    private boolean started;

    public QuestPassiveCheckTask(Plugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.wakeScheduler = new WakeScheduler<>(plugin);
    }

    public void start() {
        if (started) return;
        started = true;
        PixelRPGPlugin pixelRPG = PixelRPGPlugin.getInstance();
        navigationService = new QuestNavigationService(plugin, questManager.getRepository(), pixelRPG.getPlayerProfileManager(), pixelRPG.getNpcManager());
        inventoryTracker = new QuestInventoryTracker(questManager);
        navigationLifecycleListener = new QuestNavigationLifecycleListener(navigationService);
        plugin.getServer().getPluginManager().registerEvents(inventoryTracker, plugin);
        plugin.getServer().getPluginManager().registerEvents(navigationLifecycleListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

    // A player joining the server needs its persisted quest timers and navigation state restored once.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        questManager.restoreTimers(event.getPlayer());
        remember(event.getPlayer());
        wake(event.getPlayer());
    }

    // Movement only wakes the location-sensitive quest path when the player crosses a block boundary.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BlockPosition current = BlockPosition.of(event.getTo());
        BlockPosition previous = lastBlockByPlayer.put(player.getUniqueId(), current);
        if (previous == null || !previous.equals(current)) wake(player);
    }

    // A world change invalidates both location checks and navigation targets.
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        remember(event.getPlayer());
        wake(event.getPlayer());
    }

    // Registration enables quest processing for the player and therefore wakes its state once.
    @EventHandler
    public void onRegistration(PlayerRegistrationEvent event) {
        wake(event.getPlayer());
    }

    // Unregistration removes quest processing state from the player.
    @EventHandler
    public void onUnregistration(PlayerUnregistrationEvent event) {
        wake(event.getPlayer());
    }

    // Quest completion can change the navigation target from an objective to its quest giver.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        wake(event.getPlayer());
    }

    // Disconnecting a player cancels any queued quest work and drops its movement state.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        wakeScheduler.cancel(uuid);
        lastBlockByPlayer.remove(uuid);
    }

    private void process(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;
        if (navigationService == null) return;
        questManager.checkReachLocationQuests(player);
        navigationService.refresh(player);
    }

    private void remember(Player player) {
        lastBlockByPlayer.put(player.getUniqueId(), BlockPosition.of(player.getLocation()));
    }

    public void clear(Player player) {
        if (navigationService != null) navigationService.clear(player);
        lastBlockByPlayer.remove(player.getUniqueId());
        wakeScheduler.cancel(player.getUniqueId());
    }

    public void stop() {
        if (!started) return;
        wakeScheduler.clear();
        if (inventoryTracker != null) {
            inventoryTracker.shutdown();
            HandlerList.unregisterAll(inventoryTracker);
        }
        if (navigationLifecycleListener != null) HandlerList.unregisterAll(navigationLifecycleListener);
        HandlerList.unregisterAll(this);
        if (navigationService != null) navigationService.clearAll();
        lastBlockByPlayer.clear();
        inventoryTracker = null;
        navigationLifecycleListener = null;
        navigationService = null;
        started = false;
    }

    private record BlockPosition(UUID worldId, int x, int y, int z) {
        private static BlockPosition of(org.bukkit.Location location) {
            return location == null || location.getWorld() == null ? null : new BlockPosition(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }
}
