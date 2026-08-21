package de.pixelrpg.rpg.quest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class QuestPassiveCheckTask {

    private final Plugin plugin;
    private final QuestManager questManager;
    private final int intervalTicks;
    private BukkitTask task;

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
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!questManager.isRegistered(player.getUniqueId())) continue;
                questManager.checkInventoryQuests(player);
                questManager.checkReachLocationQuests(player);
            }
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }
}