package de.pixelrpg.rpg.quest;

import java.util.List;

public record QuestReward(
        double money,
        long experience,
        List<String> items,
        String companionId
) {
    public QuestReward {
        if (!Double.isFinite(money) || money < 0.0D) throw new IllegalArgumentException("Invalid quest reward money");
        experience = Math.max(0L, experience);
        items = items == null ? List.of() : List.copyOf(items);
        companionId = companionId == null ? "" : companionId;
    }

    public boolean hasCompanion() {
        return !companionId.isBlank();
    }
}
