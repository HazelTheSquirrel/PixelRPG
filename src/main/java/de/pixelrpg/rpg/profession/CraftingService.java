package de.pixelrpg.rpg.profession;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Executes validated profession crafting recipes. */
public final class CraftingService {
    private final ProfessionService professionService;

    public CraftingService(ProfessionService professionService) {
        this.professionService = Objects.requireNonNull(professionService);
    }

    public Optional<CraftRecipe> find(String recipeId) {
        return CraftingRecipeRegistry.find(recipeId);
    }

    public CraftResult craft(Player player, String recipeId) {
        Objects.requireNonNull(player, "player");
        var recipe = find(recipeId).orElse(null);
        if (recipe == null) return CraftResult.failure("Unknown recipe");

        int level = professionService.getLevel(player.getUniqueId(), recipe.profession());
        if (level < recipe.requiredProfessionLevel()) return CraftResult.failure("Profession level too low");

        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) {
            if (!player.getInventory().contains(cost.getKey(), cost.getValue())) return CraftResult.failure("Missing materials");
        }
        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) {
            player.getInventory().removeItem(new ItemStack(cost.getKey(), cost.getValue()));
        }

        ItemStack result = new ItemStack(recipe.resultMaterial());
        player.getInventory().addItem(result).values()
                .forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));

        long experience = Math.max(25L, recipe.craftSeconds() * 25L);
        professionService.addExperience(player, recipe.profession(), experience);
        return CraftResult.success(result, experience);
    }

    public record CraftResult(boolean success, String message, ItemStack result, long experience) {
        public static CraftResult success(ItemStack result, long experience) {
            return new CraftResult(true, "Crafting successful", result.clone(), experience);
        }

        public static CraftResult failure(String message) {
            return new CraftResult(false, message, null, 0L);
        }
    }
}
