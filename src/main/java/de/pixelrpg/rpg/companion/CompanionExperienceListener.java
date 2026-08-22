package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Awards companion progression only while a companion is actively summoned. */
public final class CompanionExperienceListener implements Listener {
    private final CompanionService companionService;

    public CompanionExperienceListener(CompanionService companionService) {
        this.companionService = companionService;
    }

    // Awards companion XP when the player defeats a mob while a companion is active.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        companionService.awardExperience(player.getUniqueId(), 25L);
    }

    // Awards a smaller companion XP amount for completing a quest while the companion is active.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        companionService.awardExperience(event.getPlayer().getUniqueId(), 100L);
    }

    // Removes the active companion entity when its owner leaves the server.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        companionService.clearActive(event.getPlayer().getUniqueId());
    }
}
