package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

/** Awards companion progression only while a companion is actively summoned. */
public final class CompanionExperienceListener implements Listener {
    private static final long MOB_KILL_EXPERIENCE = 50L;
    private static final long QUEST_COMPLETION_EXPERIENCE = 500L;

    private final CompanionService companionService;

    public CompanionExperienceListener(CompanionService companionService) {
        this.companionService = companionService;
    }

    // Awards rarity-scaled companion XP when the player defeats a mob while a companion is active.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(RPGKeys.Companion.id(), PersistentDataType.STRING)) return;
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        Companion active = companionService.getActive(player.getUniqueId());
        if (active == null) return;
        long gained = scaledExperience(MOB_KILL_EXPERIENCE, active.rarity());
        companionService.awardExperience(player.getUniqueId(), gained);
    }

    // Awards rarity-scaled companion XP for completing a quest while the companion is active.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        Companion active = companionService.getActive(event.getPlayer().getUniqueId());
        if (active == null) return;
        long gained = scaledExperience(QUEST_COMPLETION_EXPERIENCE, active.rarity());
        companionService.awardExperience(event.getPlayer().getUniqueId(), gained);
    }

    // Removes the active companion entity when its owner leaves the server.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        companionService.clearActive(event.getPlayer().getUniqueId());
    }

    private static long scaledExperience(long base, CompanionRarity rarity) {
        return Math.max(1L, Math.round(base * rarity.experienceMultiplier()));
    }
}
