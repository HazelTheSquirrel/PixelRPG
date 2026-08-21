package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Central catalogue for the PixelRPG profession economy. Recipes deliberately consume materials produced by other professions. */
public final class CraftingRecipeRegistry {
    private static final Map<Profession, List<CraftRecipe>> RECIPES = createRecipes();

    private CraftingRecipeRegistry() { }

    public static List<CraftRecipe> getRecipes(Profession profession) {
        return RECIPES.getOrDefault(profession, List.of());
    }

    public static java.util.Optional<CraftRecipe> find(String id) {
        if (id == null || id.isBlank()) return java.util.Optional.empty();
        return RECIPES.values().stream().flatMap(List::stream)
                .filter(recipe -> recipe.id().equalsIgnoreCase(id)).findFirst();
    }

    private static Map<Profession, List<CraftRecipe>> createRecipes() {
        Map<Profession, List<CraftRecipe>> recipes = new EnumMap<>(Profession.class);

        recipes.put(Profession.BLACKSMITH, List.of(
                recipe(Profession.BLACKSMITH, "blacksmith_iron_sword", "Eisenschwert", Material.IRON_SWORD, ItemRarity.COMMON, Map.of(Material.IRON_INGOT, 2, Material.STICK, 1), 1, 3),
                recipe(Profession.BLACKSMITH, "blacksmith_iron_pickaxe", "Eisenspitzhacke", Material.IRON_PICKAXE, ItemRarity.COMMON, Map.of(Material.IRON_INGOT, 3, Material.STICK, 2), 10, 4),
                recipe(Profession.BLACKSMITH, "blacksmith_iron_shield", "Eisenschild", Material.SHIELD, ItemRarity.UNCOMMON, Map.of(Material.IRON_INGOT, 1, Material.OAK_PLANKS, 6), 20, 5),
                recipe(Profession.BLACKSMITH, "blacksmith_diamond_sword", "Diamantschwert", Material.DIAMOND_SWORD, ItemRarity.RARE, Map.of(Material.DIAMOND, 2, Material.STICK, 1), 40, 7),
                recipe(Profession.BLACKSMITH, "blacksmith_diamond_chestplate", "Diamantbrustplatte", Material.DIAMOND_CHESTPLATE, ItemRarity.EPIC, Map.of(Material.DIAMOND, 8, Material.LEATHER, 2), 60, 10),
                recipe(Profession.BLACKSMITH, "blacksmith_netherite_pickaxe", "Netheritspitzhacke", Material.NETHERITE_PICKAXE, ItemRarity.LEGENDARY, Map.of(Material.NETHERITE_INGOT, 1, Material.DIAMOND_PICKAXE, 1), 90, 15),
                recipe(Profession.BLACKSMITH, "blacksmith_netherite_sword", "Netheritschwert", Material.NETHERITE_SWORD, ItemRarity.LEGENDARY, Map.of(Material.NETHERITE_INGOT, 1, Material.DIAMOND_SWORD, 1), 99, 20)
        ));

        recipes.put(Profession.PROVISIONER, List.of(
                recipe(Profession.PROVISIONER, "provisioner_bread", "Brot", Material.BREAD, ItemRarity.COMMON, Map.of(Material.WHEAT, 3), 1, 2),
                recipe(Profession.PROVISIONER, "provisioner_cooked_beef", "Gebratenes Rindfleisch", Material.COOKED_BEEF, ItemRarity.COMMON, Map.of(Material.BEEF, 1), 5, 2),
                recipe(Profession.PROVISIONER, "provisioner_cake", "Kuchen", Material.CAKE, ItemRarity.UNCOMMON, Map.of(Material.WHEAT, 3, Material.SUGAR, 2, Material.EGG, 1, Material.MILK_BUCKET, 3), 15, 5),
                recipe(Profession.PROVISIONER, "provisioner_golden_carrot", "Goldene Karotte", Material.GOLDEN_CARROT, ItemRarity.RARE, Map.of(Material.CARROT, 1, Material.GOLD_NUGGET, 8), 35, 6),
                recipe(Profession.PROVISIONER, "provisioner_golden_apple", "Goldener Apfel", Material.GOLDEN_APPLE, ItemRarity.EPIC, Map.of(Material.APPLE, 1, Material.GOLD_BLOCK, 8), 65, 12),
                recipe(Profession.PROVISIONER, "provisioner_enchanted_golden_apple", "Verzauberter goldener Apfel", Material.ENCHANTED_GOLDEN_APPLE, ItemRarity.LEGENDARY, Map.of(Material.GOLD_BLOCK, 8, Material.APPLE, 1), 99, 25)
        ));

        recipes.put(Profession.ALCHEMIST, List.of(
                recipe(Profession.ALCHEMIST, "alchemist_awkward_potion", "Seltsamer Trank", Material.POTION, ItemRarity.COMMON, Map.of(Material.NETHER_WART, 1, Material.GLASS_BOTTLE, 1), 1, 3),
                recipe(Profession.ALCHEMIST, "alchemist_blaze_powder", "Brennpulver", Material.BLAZE_POWDER, ItemRarity.UNCOMMON, Map.of(Material.BLAZE_ROD, 1), 10, 3),
                recipe(Profession.ALCHEMIST, "alchemist_glistering_melon", "Glitzernde Melonenscheibe", Material.GLISTERING_MELON_SLICE, ItemRarity.UNCOMMON, Map.of(Material.MELON_SLICE, 1, Material.GOLD_NUGGET, 8), 20, 4),
                recipe(Profession.ALCHEMIST, "alchemist_fermented_eye", "Fermentiertes Spinnenauge", Material.FERMENTED_SPIDER_EYE, ItemRarity.RARE, Map.of(Material.SPIDER_EYE, 1, Material.SUGAR, 1, Material.BROWN_MUSHROOM, 1), 35, 5),
                recipe(Profession.ALCHEMIST, "alchemist_fire_resistance", "Feuerresistenz-Trank", Material.POTION, ItemRarity.EPIC, Map.of(Material.MAGMA_CREAM, 1, Material.GLASS_BOTTLE, 1), 60, 8),
                recipe(Profession.ALCHEMIST, "alchemist_strong_reagent", "Reines Reagenz", Material.GLOWSTONE_DUST, ItemRarity.LEGENDARY, Map.of(Material.GLOWSTONE, 1, Material.NETHER_WART, 2, Material.GHAST_TEAR, 1), 99, 15)
        ));

        recipes.put(Profession.SCHOLAR, List.of(
                recipe(Profession.SCHOLAR, "scholar_book", "Buch", Material.BOOK, ItemRarity.COMMON, Map.of(Material.PAPER, 3, Material.LEATHER, 1), 1, 2),
                recipe(Profession.SCHOLAR, "scholar_compass", "Kompass", Material.COMPASS, ItemRarity.UNCOMMON, Map.of(Material.IRON_INGOT, 4, Material.REDSTONE, 1), 15, 4),
                recipe(Profession.SCHOLAR, "scholar_map", "Karte", Material.MAP, ItemRarity.UNCOMMON, Map.of(Material.PAPER, 8, Material.COMPASS, 1), 25, 5),
                recipe(Profession.SCHOLAR, "scholar_enchanting_table", "Zaubertisch", Material.ENCHANTING_TABLE, ItemRarity.RARE, Map.of(Material.BOOK, 1, Material.DIAMOND, 2, Material.OBSIDIAN, 4), 40, 8),
                recipe(Profession.SCHOLAR, "scholar_ender_eye", "Enderauge", Material.ENDER_EYE, ItemRarity.EPIC, Map.of(Material.ENDER_PEARL, 1, Material.BLAZE_POWDER, 1), 70, 10),
                recipe(Profession.SCHOLAR, "scholar_knowledge_core", "Wissenskern", Material.EXPERIENCE_BOTTLE, ItemRarity.LEGENDARY, Map.of(Material.BOOK, 2, Material.DIAMOND, 1, Material.GLOWSTONE_DUST, 4, Material.ENDER_PEARL, 1), 99, 18)
        ));

        return Map.copyOf(recipes);
    }

    private static CraftRecipe recipe(Profession profession, String id, String label, Material result,
                                      ItemRarity rarity, Map<Material, Integer> costs, int level, int seconds) {
        return new CraftRecipe(profession, id, label, result, rarity, costs, level, seconds);
    }
}
