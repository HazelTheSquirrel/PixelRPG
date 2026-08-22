package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class QuestPassiveCheckTask {

    private final Plugin plugin;
    private final QuestManager questManager;
    private final int intervalTicks;
    private BukkitTask task;
    private QuestNavigationService navigationService;

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
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                questManager.checkInventoryQuests(player);
                questManager.checkReachLocationQuests(player);
                questManager.checkEscortQuests(player);
                navigationService.refresh(player);
            }
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
        navigationService = null;
    }
}
