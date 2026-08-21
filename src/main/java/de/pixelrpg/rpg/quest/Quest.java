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
        Location escortDestination,
        Location reachLocation,
        double reachRadius
) {

    public boolean hasTimeLimit() {
        return durationMinutes > 0;
    }
}
