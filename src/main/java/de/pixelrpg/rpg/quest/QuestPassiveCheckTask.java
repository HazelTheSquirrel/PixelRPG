package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class QuestPassiveCheckTask {
    private final Plugin plugin;
    private final QuestManager questManager;
    private final int intervalTicks;
    private BukkitTask task;
    private QuestNavigationService navigationService;
    private QuestInventoryTracker inventoryTracker;
    private QuestNavigationLifecycleListener navigationLifecycleListener;

    public QuestPassiveCheckTask(Plugin plugin, QuestManager questManager) {
        this(plugin, questManager, 40);
    }

    public QuestPassiveCheckTask(Plugin plugin, QuestManager questManager, int intervalTicks) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.intervalTicks = Math.max(1, intervalTicks);
    }

    public void start() {
        if (task != null) return;
        PixelRPGPlugin pixelRPG = PixelRPGPlugin.getInstance();
        navigationService = new QuestNavigationService(plugin, questManager.getRepository(),
                pixelRPG.getPlayerProfileManager(), pixelRPG.getNpcManager());
        inventoryTracker = new QuestInventoryTracker(questManager);
        navigationLifecycleListener = new QuestNavigationLifecycleListener(navigationService);
        plugin.getServer().getPluginManager().registerEvents(inventoryTracker, plugin);
        plugin.getServer().getPluginManager().registerEvents(navigationLifecycleListener, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                inventoryTracker.refresh(player);
                questManager.checkReachLocationQuests(player);
                navigationService.refresh(player);
            }
        }, intervalTicks, intervalTicks);
    }

    public void clear(org.bukkit.entity.Player player) {
        if (navigationService != null) navigationService.clear(player);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (inventoryTracker != null) {
            HandlerList.unregisterAll(inventoryTracker);
            inventoryTracker = null;
        }
        if (navigationLifecycleListener != null) {
            HandlerList.unregisterAll(navigationLifecycleListener);
            navigationLifecycleListener = null;
        }
        if (navigationService != null) navigationService.clearAll();
        navigationService = null;
    }
}
