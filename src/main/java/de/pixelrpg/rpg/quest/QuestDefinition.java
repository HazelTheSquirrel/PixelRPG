package de.pixelrpg.rpg.quest;

import java.util.List;
import java.util.Objects;

public record QuestDefinition(
        String id,
        String title,
        String description,
        QuestType type,
        String targetKey,
        int requiredAmount,
        int requiredLevel,
        int categoryLevel,
        QuestReward reward,
        int durationMinutes,
        String questGiverNpcId,
        QuestNavigation navigation,
        String profession,
        int requiredProfessionLevel,
        List<String> prerequisites,
        List<String> followUpQuestIds
) {
    public QuestDefinition {
        require(id, "id");
        require(title, "title");
        require(description, "description");
        Objects.requireNonNull(type, "type");
        targetKey = targetKey == null ? "" : targetKey;
        requiredAmount = Math.max(1, requiredAmount);
        requiredLevel = Math.max(1, requiredLevel);
        categoryLevel = Math.max(1, categoryLevel);
        reward = Objects.requireNonNull(reward, "reward");
        durationMinutes = Math.max(0, durationMinutes);
        questGiverNpcId = questGiverNpcId == null ? "" : questGiverNpcId;
        navigation = Objects.requireNonNull(navigation, "navigation");
        profession = profession == null ? "" : profession;
        requiredProfessionLevel = Math.max(1, requiredProfessionLevel);
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
        followUpQuestIds = followUpQuestIds == null ? List.of() : List.copyOf(followUpQuestIds);
    }

    public boolean hasTimeLimit() {
        return durationMinutes > 0;
    }

    public boolean hasNavigation() {
        return navigation.hasTarget();
    }

    public boolean isProfessionQuest() {
        return !profession.isBlank();
    }

    private static void require(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
