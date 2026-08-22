package de.pixelrpg.rpg.quest;

import org.bukkit.Location;

import java.util.List;

public record Quest(
        String id,
        String title,
        String description,
        QuestType type,
        String targetKey,
        int requiredAmount,
        int requiredLevel,
        int categoryLevel,
        double rewardMoney,
        long rewardExp,
        int durationMinutes,
        List<String> rewardItemMaterials,
        String rewardCompanionId,
        Location escortDestination,
        Location reachLocation,
        double reachRadius
) {

    public boolean hasTimeLimit() {
        return durationMinutes > 0;
    }

    public boolean rewardsCompanion() {
        return rewardCompanionId != null && !rewardCompanionId.isBlank();
    }
}
