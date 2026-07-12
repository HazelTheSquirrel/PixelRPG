// src/main/java/de/pixelrpg/rpg/combat/skill/PassiveGemRecalcTask.java
package de.pixelrpg.rpg.combat.skill;

import de.pixelrpg.rpg.stats.StatEngine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PassiveGemRecalcTask {

    private final Plugin plugin;
    private final StatEngine statEngine;

    public PassiveGemRecalcTask(Plugin plugin, StatEngine statEngine) {
        this.plugin = plugin;
        this.statEngine = statEngine;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                statEngine.recalculate(player);
            }
        }, 60L, 60L);
    }
}