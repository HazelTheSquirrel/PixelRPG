package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CraftingRecipeRegistry {
    private static final Map<Profession, List<CraftRecipe>> RECIPES = createRecipes();

    private CraftingRecipeRegistry() {
    }

    public static List<CraftRecipe> getRecipes(Profession profession) {
        return RECIPES.getOrDefault(profession, List.of());
    }

    private static Map<Profession, List<CraftRecipe>> createRecipes() {
        Map<Profession, List<CraftRecipe>> recipes = new EnumMap<>(Profession.class);

        recipes.put(Profession.BLACKSMITHING, List.of(new CraftRecipe(
                Profession.BLACKSMITHING, "filler_blacksmith_iron_sword", "Wachenklinge",
                Material.IRON_SWORD, ItemRarity.RARE,
                Map.of(Material.IRON_INGOT, 2, Material.STICK, 1), 1, 3)));

        recipes.put(Profession.LEATHERWORKING, List.of(new CraftRecipe(
                Profession.LEATHERWORKING, "filler_leatherworking_leather_boots", "Lederstiefel",
                Material.LEATHER_BOOTS, ItemRarity.COMMON,
                Map.of(Material.LEATHER, 4), 1, 2)));

        recipes.put(Profession.TAILORING, List.of(new CraftRecipe(
                Profession.TAILORING, "filler_tailoring_wool_tunic", "Wolltunika",
                Material.WHITE_WOOL, ItemRarity.COMMON,
                Map.of(Material.WHITE_WOOL, 4), 1, 2)));

        recipes.put(Profession.ALCHEMY, List.of(new CraftRecipe(
                Profession.ALCHEMY, "filler_alchemy_awkward_potion", "Übungstrank",
                Material.POTION, ItemRarity.COMMON,
                Map.of(Material.NETHER_WART, 1, Material.GLASS_BOTTLE, 1), 1, 3)));

        recipes.put(Profession.COOKING, List.of(new CraftRecipe(
                Profession.COOKING, "filler_cooking_baked_potato", "Gebackene Kartoffel",
                Material.BAKED_POTATO, ItemRarity.COMMON,
                Map.of(Material.POTATO, 1), 1, 2)));

        return Map.copyOf(recipes);
    }
}
