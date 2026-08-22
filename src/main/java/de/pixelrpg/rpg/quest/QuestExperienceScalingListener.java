package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Adds a level-aware XP supplement so late-game quests remain relevant without rewriting quest definitions. */
public final class QuestExperienceScalingListener implements Listener {
    private static final double NEXT_LEVEL_SHARE = 0.015D;
    private static final long MAX_BONUS_EXPERIENCE = 75_000L;

    private final QuestRepository questRepository;
    private final PlayerProfileManager profileManager;

    public QuestExperienceScalingListener(QuestRepository questRepository, PlayerProfileManager profileManager) {
        this.questRepository = questRepository;
        this.profileManager = profileManager;
    }

    // Adds a level-scaled XP supplement after the quest's configured base reward is granted.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        Quest quest = questRepository.getQuest(event.getQuestId());
        if (quest == null) return;

        int level = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, quest.recommendedLevel()));
        long currentThreshold = Level.getRequiredExperience(level);
        long nextThreshold = level < Level.MAX_NORMAL_LEVEL
                ? Level.getRequiredExperience(level + 1)
                : Level.getRequiredExperience(Level.MAX_NORMAL_LEVEL);
        long increment = level < Level.MAX_NORMAL_LEVEL
                ? Math.max(1L, nextThreshold - currentThreshold)
                : Math.max(1L, Level.getRequiredExperience(Level.MAX_NORMAL_LEVEL) - Level.getRequiredExperience(Level.MAX_NORMAL_LEVEL - 1));

        long bonus = Math.min(MAX_BONUS_EXPERIENCE, Math.max(10L, Math.round(increment * NEXT_LEVEL_SHARE)));
        profileManager.addExperience(event.getPlayer().getUniqueId(), bonus);
    }
}
