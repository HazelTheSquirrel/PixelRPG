package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Executes validated profession crafting using only vanilla Minecraft item stacks. */
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
        return profileManager.getProfile(player.getUniqueId()).map(profile -> profile.hasUnlockedRecipe(recipe.id())).orElse(false);
    }

    public CraftResult craft(Player player, String recipeId) {
        Objects.requireNonNull(player, "player");
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return CraftResult.failure("Du musst registriertes Rathausmitglied sein.");
        CraftRecipe recipe = find(recipeId).orElse(null);
        if (recipe == null) return CraftResult.failure("Dieses Rezept existiert nicht.");
        if (!profile.hasLearnedProfession(recipe.profession())) return CraftResult.failure("Du hast diesen Beruf noch nicht erlernt.");
        int professionLevel = professionService.getLevel(player.getUniqueId(), recipe.profession());
        if (professionLevel < recipe.requiredProfessionLevel()) return CraftResult.failure("Dein Berufslevel ist für dieses Rezept zu niedrig.");
        if (!isUnlocked(player, recipe)) return CraftResult.failure("Dieses Rezept wurde noch nicht freigeschaltet.");
        if (!hasMaterialCosts(player, recipe.costs())) return CraftResult.failure("Dir fehlen die benötigten Materialien.");

        removeMaterialCosts(player, recipe.costs());

        ItemStack result = createResult(recipe);
        player.getInventory().addItem(result).values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        long experience = craftExperience(recipe);
        professionService.addExperience(player, recipe.profession(), experience);
        return CraftResult.success(result, experience, "Herstellung erfolgreich.");
    }

    private boolean hasMaterialCosts(Player player, Map<Material, Integer> costs) {
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            if (!player.getInventory().contains(cost.getKey(), cost.getValue())) return false;
        }
        return true;
    }

    private void removeMaterialCosts(Player player, Map<Material, Integer> costs) {
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            player.getInventory().removeItem(new ItemStack(cost.getKey(), cost.getValue()));
        }
    }

    private ItemStack createResult(CraftRecipe recipe) {
        ItemStack result = new ItemStack(recipe.resultMaterial(), recipe.resultAmount());

        if (!recipe.potionType().isBlank()) {
            PotionType potionType;
            try {
                potionType = PotionType.valueOf(recipe.potionType().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Invalid potion type for recipe " + recipe.id() + ": " + recipe.potionType(), exception);
            }
            PotionMeta meta = (PotionMeta) result.getItemMeta();
            meta.setBasePotionType(potionType);
            result.setItemMeta(meta);
        }

        if (!recipe.enchantment().isBlank()) {
            if (recipe.resultMaterial() != Material.ENCHANTED_BOOK) {
                throw new IllegalStateException("Enchanted-book recipe must produce ENCHANTED_BOOK: " + recipe.id());
            }
            var enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
            var enchantment = enchantmentRegistry.get(NamespacedKey.minecraft(recipe.enchantment().toLowerCase(Locale.ROOT)));
            if (enchantment == null) {
                throw new IllegalStateException("Unknown enchantment for recipe " + recipe.id() + ": " + recipe.enchantment());
            }
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
            meta.addStoredEnchant(enchantment, recipe.enchantmentLevel(), false);
            result.setItemMeta(meta);
        }

        return result;
    }

    private long craftExperience(CraftRecipe recipe) {
        long materialCount = recipe.costs().values().stream().mapToLong(Integer::longValue).sum();
        return Math.max(20L, recipe.requiredProfessionLevel() * 6L + materialCount * 5L);
    }

    public record CraftResult(boolean success, String message, ItemStack result, long experience) {
        public static CraftResult success(ItemStack result, long experience, String message) {
            return new CraftResult(true, message, result.clone(), experience);
        }

        public static CraftResult failure(String message) {
            return new CraftResult(false, message, null, 0L);
        }
    }
}
