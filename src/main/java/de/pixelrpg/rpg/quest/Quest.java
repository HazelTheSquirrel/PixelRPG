// src/main/java/de/pixelrpg/rpg/quest/Quest.java (VOLLSTÄNDIG, ersetzt alte Datei — category für Rang-Untermenüs)
package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.core.Rank;
import org.bukkit.Location;

import java.util.List;

public record Quest(
        String id,
        String title,
        String description,
        QuestType type,
        String targetKey,
        int requiredAmount,
        Rank requiredRank,
        Rank category,
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