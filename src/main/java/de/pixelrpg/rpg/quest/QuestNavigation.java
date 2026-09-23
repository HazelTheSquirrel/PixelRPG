package de.pixelrpg.rpg.quest;

import java.util.List;

public record QuestNavigation(
        String structure,
        List<String> biomes,
        int radius,
        boolean findUnexplored
) {
    public QuestNavigation {
        structure = structure == null ? "" : structure;
        biomes = biomes == null ? List.of() : List.copyOf(biomes);
        radius = Math.max(1, radius);
    }

    public boolean hasTarget() {
        return !structure.isBlank() || !biomes.isEmpty();
    }
}
