package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Regenerates a small amount of mana for registered online players. */
public final class ManaRegenerationTask {
    private final Plugin plugin;
    private final StatEngine statEngine;
    private final PlayerProfileManager profileManager;
    private BukkitTask task;

    public ManaRegenerationTask(Plugin plugin, StatEngine statEngine, PlayerProfileManager profileManager) {
        this.plugin = plugin;
        this.statEngine = statEngine;
        this.profileManager = profileManager;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!profileManager.isRegistered(player.getUniqueId())) continue;
                double maxMana = statEngine.getMaxMana(player);
                if (maxMana <= 0.0) continue;
                statEngine.restoreMana(player, Math.max(1.0, maxMana * 0.02));
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }
}
