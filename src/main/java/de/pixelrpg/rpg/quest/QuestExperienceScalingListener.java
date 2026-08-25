package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Compatibility listener retained for the existing registration path; quest XP is now definition-driven and fixed. */
public final class QuestExperienceScalingListener implements Listener {

    // Keeps quest XP deterministic: Quest.rewardExp is the complete reward and is not modified by player level.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        // Intentionally empty. QuestManager already grants the configured fixed rewardExp.
    }
}
