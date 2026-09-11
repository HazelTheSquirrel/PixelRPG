package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemRarity;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Locale;
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
        if (recipe.rarity() == ItemRarity.UNIQUE) return CraftResult.failure("Einzigartige Gegenstände können nicht hergestellt werden.");
        if (!hasMaterialCosts(player, recipe.costs()) || !hasItemCosts(player, recipe.itemCosts())) {
            return CraftResult.failure("Dir fehlen die benötigten Materialien.");
        }

        removeMaterialCosts(player, recipe.costs());
        removeItemCosts(player, recipe.itemCosts());

        ItemRarity rolledRarity = CraftingRarityRoller.roll(recipe.rarity());
        ItemStack result = createResult(recipe, rolledRarity, Math.min(99, professionLevel));
        makeCraftResultStackable(result);
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

    private boolean hasItemCosts(Player player, Map<String, Integer> costs) {
        for (Map.Entry<String, Integer> cost : costs.entrySet()) {
            if (countItemId(player, cost.getKey()) < cost.getValue()) return false;
        }
        return true;
    }

    private int countItemId(Player player, String rawItemId) {
        String itemId = resolveItemId(rawItemId);
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.isEmpty()) continue;
            if (itemService.getItemId(item).map(this::canonicalItemId).filter(itemId::equals).isPresent()) count += item.getAmount();
        }
        return count;
    }

    private void removeMaterialCosts(Player player, Map<Material, Integer> costs) {
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            player.getInventory().removeItem(new ItemStack(cost.getKey(), cost.getValue()));
        }
    }

    private void removeItemCosts(Player player, Map<String, Integer> costs) {
        for (Map.Entry<String, Integer> cost : costs.entrySet()) {
            int remaining = cost.getValue();
            String requiredId = resolveItemId(cost.getKey());
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                ItemStack item = contents[slot];
                if (item == null || item.isEmpty()) continue;
                boolean matches = itemService.getItemId(item).map(this::canonicalItemId).filter(requiredId::equals).isPresent();
                if (!matches) continue;
                int moved = Math.min(remaining, item.getAmount());
                if (moved == item.getAmount()) contents[slot] = null;
                else item.setAmount(item.getAmount() - moved);
                remaining -= moved;
            }
            player.getInventory().setStorageContents(contents);
        }
    }

    private String resolveItemId(String rawItemId) {
        String normalized = canonicalItemId(rawItemId);
        String recipeId = normalized.startsWith("pixelrpg:") ? normalized.substring("pixelrpg:".length()) : normalized;
        CraftRecipe recipe = registry.find(recipeId).orElse(null);
        if (recipe != null && !recipe.resultItemId().isBlank()) return canonicalItemId(recipe.resultItemId());
        return normalized;
    }

    private String canonicalItemId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith("pixelrpg:")) value = "pixelrpg:" + value;
        String body = value.substring("pixelrpg:".length());
        int slash = body.indexOf('/');
        if (slash > 0) body = body.substring(0, slash) + ":" + body.substring(slash + 1);
        return "pixelrpg:" + body;
    }

    private ItemStack createResult(CraftRecipe recipe, ItemRarity rarity, int itemLevel) {
        String itemId = recipe.resultItemId().isBlank() ? "pixelrpg:" + recipe.id() : recipe.resultItemId();
        if (!recipe.potionType().isBlank()) {
            PotionType potionType;
            try { potionType = PotionType.valueOf(recipe.potionType().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException exception) { throw new IllegalStateException("Invalid potion type for recipe " + recipe.id() + ": " + recipe.potionType(), exception); }
            ItemStack potion = new ItemStack(recipe.resultMaterial(), recipe.resultAmount());
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            meta.setBasePotionType(potionType);
            meta.displayName(net.kyori.adventure.text.Component.text(recipe.displayName()));
            tagSpecialCraftResult(meta, itemId);
            potion.setItemMeta(meta);
            return potion;
        }
        if (!recipe.enchantment().isBlank()) {
            if (recipe.resultMaterial() != Material.ENCHANTED_BOOK) throw new IllegalStateException("Enchanted-book recipe must produce ENCHANTED_BOOK: " + recipe.id());
            var enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
            var enchantment = enchantmentRegistry.get(NamespacedKey.minecraft(recipe.enchantment().toLowerCase(Locale.ROOT)));
            if (enchantment == null) throw new IllegalStateException("Unknown enchantment for recipe " + recipe.id() + ": " + recipe.enchantment());
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, recipe.resultAmount());
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
            meta.addStoredEnchant(enchantment, recipe.enchantmentLevel(), false);
            meta.displayName(net.kyori.adventure.text.Component.text(recipe.displayName()));
            tagSpecialCraftResult(meta, itemId);
            book.setItemMeta(meta);
            return book;
        }
        return itemService.createCraftedItem(canonicalItemId(itemId), recipe.displayName(), recipe.resultMaterial(), rarity, itemLevel);
    }

    private void tagSpecialCraftResult(ItemMeta meta, String itemId) {
        var pdc = meta.getPersistentDataContainer();
        String canonicalId = canonicalItemId(itemId);
        String resourcepackId = itemService.definitions().stream()
                .filter(definition -> definition.id().equals(canonicalId))
                .map(ItemDefinition::resourcepackId)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(canonicalId);
        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, canonicalId);
        pdc.set(RPGKeys.Item.resourcepackId(), PersistentDataType.STRING, resourcepackId);
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);
    }

    private void makeCraftResultStackable(ItemStack result) {
        if (result == null || result.isEmpty() || !result.hasItemMeta()) return;
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().remove(RPGKeys.Item.instanceId());
        result.setItemMeta(meta);
    }

    private long craftExperience(CraftRecipe recipe) {
        long materialCount = recipe.costs().values().stream().mapToLong(Integer::longValue).sum();
        long craftedItemCount = recipe.itemCosts().values().stream().mapToLong(Integer::longValue).sum();
        return Math.max(20L, recipe.requiredProfessionLevel() * 6L + (materialCount + craftedItemCount) * 5L);
    }

    public record CraftResult(boolean success, String message, ItemStack result, long experience) {
        public static CraftResult success(ItemStack result, long experience, String message) { return new CraftResult(true, message, result.clone(), experience); }
        public static CraftResult failure(String message) { return new CraftResult(false, message, null, 0L); }
    }
}
