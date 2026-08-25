package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;

import java.util.Map;

public record CraftRecipe(
        Profession profession,
        String id,
        String label,
        Material resultMaterial,
        int resultAmount,
        ItemRarity rarity,
        Map<Material, Integer> costs,
        int requiredProfessionLevel,
        long unlockPrice,
        String requiredQuestId,
        boolean unlockedByDefault,
        boolean vanillaRecipe,
        String resultItemId
) {
    public CraftRecipe {
        if (profession == null) throw new IllegalArgumentException("Profession must not be null");
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Recipe id must not be blank");
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Recipe label must not be blank");
        if (resultMaterial == null || resultMaterial.isAir()) throw new IllegalArgumentException("Result material must be valid");
        if (resultAmount <= 0) throw new IllegalArgumentException("Result amount must be positive");
        if (rarity == null) throw new IllegalArgumentException("Recipe rarity must not be null");
        if (costs == null || costs.isEmpty() || costs.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0)) {
            throw new IllegalArgumentException("Recipe costs must contain positive material amounts");
        }
        if (requiredProfessionLevel < Profession.MIN_LEVEL || requiredProfessionLevel > Profession.MAX_LEVEL) {
            throw new IllegalArgumentException("Invalid required profession level");
        }
        if (unlockPrice < 0L) throw new IllegalArgumentException("Unlock price must not be negative");
        if (requiredQuestId == null) requiredQuestId = "";
        if (resultItemId == null) resultItemId = "";
        costs = Map.copyOf(costs);
    }

    public String displayName() { return label; }
}
