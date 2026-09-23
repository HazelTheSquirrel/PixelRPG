package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.companion.CompanionService;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Applies companion unlock rewards after the quest domain has completed and persisted the quest state. */
public final class QuestCompanionRewardListener implements Listener {
    private final QuestService quests;
    private final CompanionService companions;
    private final PlayerProfileManager profiles;

    public QuestCompanionRewardListener(QuestService quests, CompanionService companions, PlayerProfileManager profiles) {
        this.quests = quests;
        this.companions = companions;
        this.profiles = profiles;
    }

    // Grants the companion referenced by the completed quest definition.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        QuestDefinition quest = quests.find(event.questId()).orElse(null);
        if (quest == null || !quest.reward().hasCompanion()) return;
        if (companions.adminGrant(event.playerId(), quest.reward().companionId())) {
            profiles.saveProfileAsync(event.playerId());
        }
    }
}
