package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.CraftedItemFactory;
import de.pixelrpg.rpg.item.ItemService;
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

/** Executes validated PixelRPG profession crafting. Vanilla crafting remains entirely independent. */
public final class CraftingService {
    private final ProfessionService professionService;
    private final PlayerProfileManager profileManager;
    private final ItemService itemService;
    private final CraftingRecipeRegistry registry;

    public CraftingService(ProfessionService professionService, PlayerProfileManager profileManager, ItemService itemService, CraftingRecipeRegistry registry) {
        this.professionService = Objects.requireNonNull(professionService);
        this.profileManager = Objects.requireNonNull(profileManager);
        this.itemService = Objects.requireNonNull(itemService);
        this.registry = Objects.requireNonNull(registry);
    }

    public Optional<CraftRecipe> find(String recipeId) { return registry.find(recipeId); }
    public List<CraftRecipe> recipes(Profession profession) { return registry.getRecipes(profession); }
    public ProfessionService.UnlockResult unlockRecipe(Player player, CraftRecipe recipe) { return professionService.unlockRecipe(player, recipe); }

    public boolean isUnlocked(Player player, CraftRecipe recipe) {
        if (recipe.unlockedByDefault()) return true;
        return profileManager.getProfile(player.getUniqueId())
                .map(profile -> profile.hasUnlockedRecipe(recipe.id()))
                .orElse(false);
    }

    public CraftResult craft(Player player, String recipeId) {
        Objects.requireNonNull(player, "player");
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return CraftResult.failure("PixelRPG registration required");

        CraftRecipe recipe = find(recipeId).orElse(null);
        if (recipe == null) return CraftResult.failure("Unknown recipe");
        if (!profile.hasLearnedProfession(recipe.profession())) return CraftResult.failure("Profession not learned");

        int professionLevel = professionService.getLevel(player.getUniqueId(), recipe.profession());
        if (professionLevel < recipe.requiredProfessionLevel()) return CraftResult.failure("Profession level too low");
        if (!isUnlocked(player, recipe)) return CraftResult.failure("Recipe not unlocked");
        if (recipe.rarity() == ItemRarity.UNIQUE) return CraftResult.failure("UNIQUE items cannot be crafted");

        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) {
            if (!player.getInventory().contains(cost.getKey(), cost.getValue())) return CraftResult.failure("Missing materials");
        }
        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) {
            player.getInventory().removeItem(new ItemStack(cost.getKey(), cost.getValue()));
        }

        ItemRarity rolledRarity = CraftingRarityRoller.roll(recipe.rarity());
        ItemStack result = createResult(recipe, rolledRarity, Math.min(99, professionLevel));
        player.getInventory().addItem(result).values()
                .forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));

        long experience = craftExperience(recipe);
        professionService.addExperience(player, recipe.profession(), experience);
        return CraftResult.success(result, experience);
    }

    private ItemStack createResult(CraftRecipe recipe, ItemRarity rarity, int itemLevel) {
        if (!recipe.resultItemId().isBlank()) {
            return itemService.createItem(recipe.resultItemId())
                    .orElseThrow(() -> new IllegalStateException("Unable to create crafting result item: " + recipe.resultItemId()));
        }
        ItemStack item = CraftedItemFactory.create(recipe.id(), recipe.displayName(), recipe.resultMaterial(), rarity, itemLevel);
        if (recipe.resultAmount() > 1) item.setAmount(recipe.resultAmount());
        return item;
    }

    private long craftExperience(CraftRecipe recipe) {
        return Math.max(20L, recipe.requiredProfessionLevel() * 6L + recipe.costs().values().stream().mapToLong(Integer::longValue).sum() * 5L);
    }

    public record CraftResult(boolean success, String message, ItemStack result, long experience) {
        public static CraftResult success(ItemStack result, long experience) { return new CraftResult(true, "Crafting successful", result.clone(), experience); }
        public static CraftResult failure(String message) { return new CraftResult(false, message, null, 0L); }
    }
}
