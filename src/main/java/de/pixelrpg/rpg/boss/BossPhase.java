// src/main/java/de/pixelrpg/rpg/boss/BossPhase.java
package de.pixelrpg.rpg.boss;

import java.util.List;

public record BossPhase(
        double healthPercentageThreshold,
        List<String> attackPatternIds,
        int attackIntervalTicks,
        String announcementMessage
) {
}