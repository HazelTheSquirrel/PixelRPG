package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

/** Converts player activities into centralized companion progression and lifecycle requests. */
public final class CompanionExperienceListener implements Listener {
    private static final long MOB_KILL_EXPERIENCE = 50L;
    private static final long QUEST_COMPLETION_EXPERIENCE = 500L;

    private final CompanionService companionService;

    public CompanionExperienceListener(CompanionService companionService) {
        this.companionService = companionService;
    }

    // Clears a companion's active runtime state when its spawned entity dies.
    @EventHandler
    public void onCompanionDeath(EntityDeathEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(RPGKeys.Companion.id(), PersistentDataType.STRING)) return;
        java.util.UUID ownerUuid = companionService.getOwnerOfEntity(event.getEntity().getUniqueId());
        if (ownerUuid != null) companionService.clearActive(ownerUuid);
    }

    // Awards base companion XP for a mob kill; rarity scaling is owned by CompanionProgression.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(RPGKeys.Companion.id(), PersistentDataType.STRING)) return;
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        if (companionService.getActive(player.getUniqueId()) == null) return;
        companionService.awardExperience(player.getUniqueId(), MOB_KILL_EXPERIENCE);
    }

    // Awards base companion XP for a quest completion; rarity scaling is centralized in progression.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        if (companionService.getActive(event.getPlayer().getUniqueId()) == null) return;
        companionService.awardExperience(event.getPlayer().getUniqueId(), QUEST_COMPLETION_EXPERIENCE);
    }

    // Dismisses the runtime companion while the owner is dead; ownership and progression remain persisted.
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        companionService.clearActive(event.getEntity());
    }

    // Removes the runtime companion entity when its owner leaves the server.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        companionService.clearActive(event.getPlayer().getUniqueId());
    }
}
