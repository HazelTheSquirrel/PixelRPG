package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.CraftedItemFactory;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Executes validated profession crafting recipes for registered PixelRPG players. */
public final class CraftingService {
    private final ProfessionService professionService;
    private final PlayerProfileManager profileManager;

    public CraftingService(ProfessionService professionService, PlayerProfileManager profileManager) {
        this.professionService = Objects.requireNonNull(professionService);
        this.profileManager = Objects.requireNonNull(profileManager);
    }

    public Optional<CraftRecipe> find(String recipeId) {
        return CraftingRecipeRegistry.find(recipeId);
    }

    public List<CraftRecipe> recipes(Profession profession) {
        return CraftingRecipeRegistry.getRecipes(profession);
    }

    public CraftResult craft(Player player, String recipeId) {
        Objects.requireNonNull(player, "player");
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) return CraftResult.failure("PixelRPG registration required");

        CraftRecipe recipe = find(recipeId).orElse(null);
        if (recipe == null) return CraftResult.failure("Unknown recipe");
        if (recipe.rarity() == ItemRarity.UNIQUE) return CraftResult.failure("UNIQUE items can only be granted by an administrator");
        if (!profile.hasLearnedProfession(recipe.profession())) return CraftResult.failure("Profession not learned");
        if (!profile.hasUnlockedRecipe(recipe.id())) return CraftResult.failure("Recipe not learned");

        int level = professionService.getLevel(player.getUniqueId(), recipe.profession());
        if (level < recipe.requiredProfessionLevel()) return CraftResult.failure("Profession level too low");

        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) {
            if (!player.getInventory().contains(cost.getKey(), cost.getValue())) return CraftResult.failure("Missing materials");
        }

        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) {
            player.getInventory().removeItem(new ItemStack(cost.getKey(), cost.getValue()));
        }

        ItemStack result = CraftedItemFactory.create(
                recipe.id(), recipe.displayName(), recipe.resultMaterial(), recipe.rarity(), profile.getLevel());
        player.getInventory().addItem(result).values()
                .forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));

        long experience = craftExperience(recipe);
        professionService.addExperience(player, recipe.profession(), experience);
        return CraftResult.success(result, experience);
    }

    private long craftExperience(CraftRecipe recipe) {
        return Math.max(20L, recipe.requiredProfessionLevel() * 6L + recipe.craftSeconds() * 20L);
    }

    public record CraftResult(boolean success, String message, ItemStack result, long experience) {
        public static CraftResult success(ItemStack result, long experience) { return new CraftResult(true, "Crafting successful", result.clone(), experience); }
        public static CraftResult failure(String message) { return new CraftResult(false, message, null, 0L); }
    }
}
