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
        if (!hasItemCosts(player, recipe.itemCosts())) return CraftResult.failure("Dir fehlen die benötigten PixelRPG-Gegenstände.");

        ItemStack result = createResult(recipe);
        if (result == null || result.isEmpty()) return CraftResult.failure("Das Rezept konnte keinen gültigen Gegenstand erzeugen.");

        removeMaterialCosts(player, recipe.costs());
        removeItemCosts(player, recipe.itemCosts());
        player.getInventory().addItem(result).values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        long experience = recipe.professionXp();
        professionService.addExperience(player, recipe.profession(), experience);
        return CraftResult.success(result, experience, "Herstellung erfolgreich.");
    }

    private boolean hasMaterialCosts(Player player, Map<Material, Integer> costs) {
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            if (!player.getInventory().contains(cost.getKey(), cost.getValue())) return false;
        }
        return true;
    }

    private boolean hasItemCosts(Player player, Map<String, Integer> costs) {
        for (Map.Entry<String, Integer> cost : costs.entrySet()) {
            if (countItem(player, cost.getKey()) < cost.getValue()) return false;
        }
        return true;
    }

    private void removeMaterialCosts(Player player, Map<Material, Integer> costs) {
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            player.getInventory().removeItem(new ItemStack(cost.getKey(), cost.getValue()));
        }
    }

    private void removeItemCosts(Player player, Map<String, Integer> costs) {
        for (Map.Entry<String, Integer> cost : costs.entrySet()) {
            int remaining = cost.getValue();
            for (int slot = 0; slot < player.getInventory().getSize() && remaining > 0; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item == null || item.isEmpty()) continue;
                String itemId = itemService.getItemId(item).orElse("");
                if (!canonicalItemId(itemId).equals(canonicalItemId(cost.getKey()))) continue;
                int take = Math.min(remaining, item.getAmount());
                if (take == item.getAmount()) player.getInventory().setItem(slot, null);
                else item.setAmount(item.getAmount() - take);
                remaining -= take;
            }
        }
    }

    private int countItem(Player player, String itemId) {
        int count = 0;
        String normalized = canonicalItemId(itemId);
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.isEmpty()) continue;
            if (canonicalItemId(itemService.getItemId(item).orElse("")).equals(normalized)) count += item.getAmount();
        }
        return count;
    }

    private String canonicalItemId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith("pixelrpg:")) return value;
        return value;
    }

    private ItemStack createResult(CraftRecipe recipe) {
        if (!recipe.resultItemId().isBlank()) {
            return itemService.createItem(recipe.resultItemId()).orElse(null);
        }

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

    public record CraftResult(boolean success, String message, ItemStack result, long experience) {
        public static CraftResult success(ItemStack result, long experience, String message) {
            return new CraftResult(true, message, result.clone(), experience);
        }

        public static CraftResult failure(String message) {
            return new CraftResult(false, message, null, 0L);
        }
    }
}
