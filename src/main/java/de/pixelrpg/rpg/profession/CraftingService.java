package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.CraftedItemFactory;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Executes validated profession crafting and respects vanilla recipe discovery for vanilla recipes. */
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
        if (recipe.vanillaRecipe()) {
            NamespacedKey key = vanillaKey(recipe);
            return key != null && player.hasDiscoveredRecipe(key);
        }
        return profileManager.getProfile(player.getUniqueId()).map(profile -> profile.hasUnlockedRecipe(recipe.id())).orElse(false);
    }

    public CraftResult craft(Player player, String recipeId) {
        Objects.requireNonNull(player, "player");
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return CraftResult.failure("PixelRPG registration required");

        CraftRecipe recipe = find(recipeId).orElse(null);
        if (recipe == null) return CraftResult.failure("Unknown recipe");
        if (!profile.hasLearnedProfession(recipe.profession())) return CraftResult.failure("Profession not learned");

        int level = professionService.getLevel(player.getUniqueId(), recipe.profession());
        if (level < recipe.requiredProfessionLevel()) return CraftResult.failure("Profession level too low");
        if (!isUnlocked(player, recipe)) return CraftResult.failure(recipe.vanillaRecipe() ? "Vanilla recipe not discovered" : "Recipe not unlocked");
        if (recipe.rarity() == ItemRarity.UNIQUE) return CraftResult.failure("UNIQUE items cannot be crafted");

        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) {
            if (!player.getInventory().contains(cost.getKey(), cost.getValue())) return CraftResult.failure("Missing materials");
        }

        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) {
            player.getInventory().removeItem(new ItemStack(cost.getKey(), cost.getValue()));
        }

        ItemStack result = createResult(recipe, profile.getLevel());
        player.getInventory().addItem(result).values()
                .forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));

        long experience = craftExperience(recipe);
        professionService.addExperience(player, recipe.profession(), experience);
        return CraftResult.success(result, experience);
    }

    private NamespacedKey vanillaKey(CraftRecipe recipe) {
        if (!recipe.id().startsWith("vanilla:")) return null;
        String keyText = recipe.id().substring("vanilla:".length());
        return NamespacedKey.fromString(keyText);
    }

    private ItemStack createResult(CraftRecipe recipe, int playerLevel) {
        if (recipe.vanillaRecipe()) return new ItemStack(recipe.resultMaterial(), recipe.resultAmount());
        if (!recipe.resultItemId().isBlank()) {
            return itemService.createItem(recipe.resultItemId())
                    .orElseThrow(() -> new IllegalStateException("Unable to create crafting result item: " + recipe.resultItemId()));
        }
        ItemStack item = CraftedItemFactory.create(recipe.id(), recipe.displayName(), recipe.resultMaterial(), recipe.rarity(), playerLevel);
        if (recipe.resultAmount() == 1) return item;
        item.setAmount(recipe.resultAmount());
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
