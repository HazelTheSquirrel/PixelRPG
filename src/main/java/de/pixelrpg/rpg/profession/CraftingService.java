package de.pixelrpg.rpg.profession;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Executes validated profession crafting recipes. */
public final class CraftingService {
    private final CraftingRecipeRegistry registry;
    private final ProfessionService professionService;

    public CraftingService(CraftingRecipeRegistry registry, ProfessionService professionService) {
        this.registry = Objects.requireNonNull(registry);
        this.professionService = Objects.requireNonNull(professionService);
    }

    public Optional<CraftRecipe> find(String recipeId) {
        return registry.find(recipeId);
    }

    public CraftResult craft(Player player, String recipeId) {
        var recipe = registry.find(recipeId).orElse(null);
        if (recipe == null) {
            return CraftResult.failure("Unknown recipe");
        }

        var level = professionService.getLevel(player.getUniqueId(), recipe.profession());
        if (level < recipe.requiredProfessionLevel()) {
            return CraftResult.failure("Profession level too low");
        }

        for (Map.Entry<String, Integer> cost : recipe.costs().entrySet()) {
            if (!hasMaterial(player, cost.getKey(), cost.getValue())) {
                return CraftResult.failure("Missing materials");
            }
        }

        for (Map.Entry<String, Integer> cost : recipe.costs().entrySet()) {
            removeMaterial(player, cost.getKey(), cost.getValue());
        }

        var stack = new org.bukkit.inventory.ItemStack(recipe.resultMaterial(), recipe.resultAmount());
        player.getInventory().addItem(stack);
        professionService.addExperience(player.getUniqueId(), recipe.profession(), recipe.experienceReward());
        return CraftResult.success(stack);
    }

    private boolean hasMaterial(Player player, String materialName, int amount) {
        var material = org.bukkit.Material.matchMaterial(materialName);
        if (material == null) return false;
        return player.getInventory().contains(material, amount);
    }

    private void removeMaterial(Player player, String materialName, int amount) {
        var material = org.bukkit.Material.matchMaterial(materialName);
        if (material != null) {
            player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(material, amount));
        }
    }

    public record CraftResult(boolean success, String message, org.bukkit.inventory.ItemStack result) {
        public static CraftResult success(org.bukkit.inventory.ItemStack result) {
            return new CraftResult(true, "Crafting successful", result);
        }

        public static CraftResult failure(String message) {
            return new CraftResult(false, message, null);
        }
    }
}
