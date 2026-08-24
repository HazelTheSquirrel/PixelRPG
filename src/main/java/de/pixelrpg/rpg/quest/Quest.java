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
        String questGiverNpcId,
        String targetStructureKey,
        List<String> targetBiomeKeys,
        int navigationRadius,
        boolean findUnexploredStructure,
        Location reachLocation
) {

    public Quest {
        rewardItemMaterials = rewardItemMaterials == null ? List.of() : List.copyOf(rewardItemMaterials);
        targetBiomeKeys = targetBiomeKeys == null ? List.of() : List.copyOf(targetBiomeKeys);
    }

    public boolean hasTimeLimit() {
        return durationMinutes > 0;
    }

    public boolean rewardsCompanion() {
        return rewardCompanionId != null && !rewardCompanionId.isBlank();
    }

    public boolean hasNavigationTarget() {
        return (targetStructureKey != null && !targetStructureKey.isBlank()) || !targetBiomeKeys.isEmpty() || reachLocation != null;
    }
}
