// src/main/java/de/pixelrpg/rpg/boss/BossLootConfig.java
package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.item.ItemRarity;

import java.util.List;

public record BossLootConfig(
        List<String> materialPool,
        ItemRarity guaranteedRarity,
        double moneyReward,
        long expReward
) {
}