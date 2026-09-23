package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class QuestService implements AutoCloseable {
    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final QuestRepository repository;

    public QuestService(Plugin plugin, PlayerProfileManager profileManager, QuestRepository repository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.profileManager = Objects.requireNonNull(profileManager, "profileManager");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<QuestDefinition> find(String id) {
        return Optional.ofNullable(repository.get(id));
    }

    public boolean canStart(PlayerProfile profile, String questId) {
        QuestDefinition quest = repository.get(questId);
        if (quest == null || profile.hasActiveQuest(questId) || profile.hasCompletedQuest(questId)) return false;
        if (profile.getActiveQuests().size() >= repository.maxActiveQuests()) return false;
        if (profile.getLevel() < quest.requiredLevel()) return false;
        if (quest.isProfessionQuest()) {
            Profession profession;
            try {
                profession = Profession.valueOf(quest.profession());
            } catch (IllegalArgumentException exception) {
                return false;
            }
            if (!profile.hasLearnedProfession(profession) || profile.getProfessionLevel(profession) < quest.requiredProfessionLevel()) return false;
        }
        for (String prerequisite : quest.prerequisites()) {
            if (!profile.hasCompletedQuest(prerequisite)) return false;
        }
        return true;
    }

    public boolean start(PlayerProfile profile, String questId) {
        if (!canStart(profile, questId)) return false;
        QuestDefinition quest = repository.get(questId);
        long expiry = quest.hasTimeLimit() ? System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(quest.durationMinutes()) : 0L;
        profile.startQuest(new QuestProgress(questId, 0, expiry));
        return true;
    }

    public boolean abandon(PlayerProfile profile, String questId) {
        if (!profile.hasActiveQuest(questId)) return false;
        profile.removeActiveQuest(questId);
        return true;
    }

    public boolean setProgress(PlayerProfile profile, String questId, int amount) {
        QuestDefinition quest = repository.get(questId);
        QuestProgress progress = profile.getActiveQuests().get(questId);
        if (quest == null || progress == null || progress.isExpired()) return false;
        int next = Math.clamp(amount, 0, quest.requiredAmount());
        if (next == progress.getCurrentAmount()) return true;
        progress.setCurrentAmount(next);
        return true;
    }

    public boolean incrementProgress(PlayerProfile profile, String questId, int amount) {
        if (amount <= 0) return false;
        QuestProgress progress = profile.getActiveQuests().get(questId);
        if (progress == null) return false;
        return setProgress(profile, questId, progress.getCurrentAmount() + amount);
    }

    public boolean isComplete(PlayerProfile profile, String questId) {
        QuestDefinition quest = repository.get(questId);
        QuestProgress progress = profile.getActiveQuests().get(questId);
        return quest != null && progress != null && !progress.isExpired() && progress.getCurrentAmount() >= quest.requiredAmount();
    }

    public boolean complete(PlayerProfile profile, String questId) {
        if (!isComplete(profile, questId)) return false;
        profile.removeActiveQuest(questId);
        profile.markQuestCompleted(questId);
        plugin.getServer().getPluginManager().callEvent(new QuestCompletedEvent(profile.getUuid(), questId));
        return true;
    }

    public QuestRepository repository() {
        return repository;
    }

    @Override
    public void close() {
        repository.close();
    }
}
