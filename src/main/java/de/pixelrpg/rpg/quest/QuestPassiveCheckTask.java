// src/main/java/de/pixelrpg/rpg/quest/QuestPassiveCheckTask.java
package de.pixelrpg.rpg.quest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class QuestPassiveCheckTask {

    private final Plugin plugin;
    private final QuestManager questManager;

    public QuestPassiveCheckTask(Plugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                questManager.checkInventoryQuests(player);
                questManager.checkReachLocationQuests(player);
            }
        }, 40L, 40L);
    }
}