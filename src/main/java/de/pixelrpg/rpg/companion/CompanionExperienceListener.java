package de.pixelrpg.rpg.companion;

import com.google.gson.JsonObject;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Awards companion progression only while a companion is actively summoned. */
public final class CompanionExperienceListener implements Listener {
    private final CompanionService companionService;
    private final long mobKillExperience;
    private final long questCompletionExperience;

    public CompanionExperienceListener(CompanionService companionService) {
        this.companionService = companionService;
        Plugin plugin = de.pixelrpg.rpg.PixelRPGPlugin.getInstance();
        this.mobKillExperience = readExperience(plugin, "mobKill", 25L);
        this.questCompletionExperience = readExperience(plugin, "questCompletion", 100L);
    }

    // Awards companion XP when the player defeats a mob while a companion is active.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(RPGKeys.Companion.id(), PersistentDataType.STRING)) return;
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        companionService.awardExperience(player.getUniqueId(), mobKillExperience);
    }

    // Awards companion XP for completing a quest while the companion is active.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        companionService.awardExperience(event.getPlayer().getUniqueId(), questCompletionExperience);
    }

    // Removes the active companion entity when its owner leaves the server.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        companionService.clearActive(event.getPlayer().getUniqueId());
    }

    private static long readExperience(Plugin plugin, String key, long fallback) {
        try {
            JsonObject root = new JsonDataManager(plugin).load("companions.json");
            JsonObject progression = root.getAsJsonObject("progression");
            JsonObject xp = progression == null ? null : progression.getAsJsonObject("xp");
            return xp != null && xp.has(key) && xp.get(key).isNumber()
                    ? Math.max(0L, xp.get(key).getAsLong())
                    : fallback;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to read companion XP setting '" + key + "': " + exception.getMessage());
            return fallback;
        }
    }
}
