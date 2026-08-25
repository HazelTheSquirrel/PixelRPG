package de.pixelrpg.rpg.boss;

import java.util.List;

public record BossLootConfig(
        List<String> guaranteedMaterials,
        List<BossLootEntry> chanceDrops,
        double moneyReward,
        long expReward
) {
    public BossLootConfig {
        guaranteedMaterials = guaranteedMaterials == null ? List.of() : List.copyOf(guaranteedMaterials);
        chanceDrops = chanceDrops == null ? List.of() : List.copyOf(chanceDrops);
    }
}
