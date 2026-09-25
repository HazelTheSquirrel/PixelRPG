package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;

import java.util.Map;

public record CraftRecipe(
        Profession profession,
        CraftingCategory category,
        String id,
        String label,
        Material resultMaterial,
        int resultAmount,
        ItemRarity rarity,
        Map<Material, Integer> costs,
        Map<String, Integer> itemCosts,
        int requiredProfessionLevel,
        long professionXp,
        long unlockPrice,
        String requiredQuestId,
        boolean unlockedByDefault,
        boolean vanillaRecipe,
        String resultItemId,
        String potionType,
        String enchantment,
        int enchantmentLevel
) {
    public CraftRecipe {
        if (profession == null) throw new IllegalArgumentException("Profession must not be null");
        if (category == null) throw new IllegalArgumentException("Recipe category must not be null");
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Recipe id must not be blank");
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Recipe label must not be blank");
        if (resultMaterial == null || resultMaterial.isAir()) throw new IllegalArgumentException("Result material must be valid");
        if (resultAmount <= 0) throw new IllegalArgumentException("Result amount must be positive");
        if (rarity == null) throw new IllegalArgumentException("Recipe rarity must not be null");
        if (costs == null || costs.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0)) {
            throw new IllegalArgumentException("Recipe material costs must contain positive material amounts");
        }
        if (itemCosts == null || itemCosts.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() <= 0)) {
            throw new IllegalArgumentException("Recipe item costs must contain positive item amounts");
        }
        if (costs.isEmpty() && itemCosts.isEmpty()) throw new IllegalArgumentException("Recipe must have at least one cost");
        if (requiredProfessionLevel < Profession.MIN_LEVEL || requiredProfessionLevel > Profession.MAX_LEVEL) {
            throw new IllegalArgumentException("Invalid required profession level");
        }
        if (professionXp <= 0L) throw new IllegalArgumentException("Profession XP must be positive");
        if (unlockPrice < 0L) throw new IllegalArgumentException("Unlock price must not be negative");
        if (requiredQuestId == null) requiredQuestId = "";
        if (resultItemId == null) resultItemId = "";
        if (potionType == null) potionType = "";
        if (enchantment == null) enchantment = "";
        if (enchantmentLevel < 0) throw new IllegalArgumentException("Enchantment level must not be negative");
        costs = Map.copyOf(costs);
        itemCosts = Map.copyOf(itemCosts);
    }

    public String displayName() { return label; }
}
