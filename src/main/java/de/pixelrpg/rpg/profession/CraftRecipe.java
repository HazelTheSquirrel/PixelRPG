package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;

import java.util.Map;

public record CraftRecipe(
        Profession profession,
        String id,
        String label,
        Material resultMaterial,
        ItemRarity rarity,
        Map<Material, Integer> costs,
        int requiredProfessionLevel,
        int craftSeconds
) {
    public CraftRecipe {
        if (!profession.name().equals(profession.name())) {
            throw new IllegalArgumentException("Invalid profession");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Recipe id must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Recipe label must not be blank");
        }
        if (resultMaterial == null) {
            throw new IllegalArgumentException("Result material must not be null");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("Rarity must not be null");
        }
        if (costs == null || costs.isEmpty() || costs.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0)) {
            throw new IllegalArgumentException("Recipe costs must contain positive material amounts");
        }
        if (requiredProfessionLevel < Profession.MIN_LEVEL || requiredProfessionLevel > Profession.MAX_LEVEL) {
            throw new IllegalArgumentException("Invalid required profession level");
        }
        if (craftSeconds <= 0) {
            throw new IllegalArgumentException("Craft duration must be positive");
        }
        costs = Map.copyOf(costs);
    }
}
