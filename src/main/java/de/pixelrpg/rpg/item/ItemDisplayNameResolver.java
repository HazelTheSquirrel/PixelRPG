package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.profession.CraftRecipe;
import de.pixelrpg.rpg.profession.CraftingRecipeRegistry;
import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Single source for player-facing item names across PixelRPG systems.
 * Item definitions have priority; profession recipe labels are used for
 * crafted outputs that intentionally do not have their own item definition.
 */
public final class ItemDisplayNameResolver {
    private final ItemDefinitionRegistry itemDefinitions;
    private final CraftingRecipeRegistry craftingRecipes;

    public ItemDisplayNameResolver(ItemDefinitionRegistry itemDefinitions, CraftingRecipeRegistry craftingRecipes) {
        this.itemDefinitions = Objects.requireNonNull(itemDefinitions);
        this.craftingRecipes = Objects.requireNonNull(craftingRecipes);
    }

    public Optional<String> resolve(String key) {
        if (key == null || key.isBlank()) return Optional.empty();

        Optional<ItemDefinition> definition = itemDefinitions.find(key);
        if (definition.isPresent()) return Optional.of(definition.get().name());

        Optional<CraftRecipe> recipe = craftingRecipes.find(key);
        if (recipe.isPresent()) return Optional.of(recipe.get().displayName());

        Optional<String> recipeName = craftingRecipes.findDisplayNameByResultItemId(key);
        if (recipeName.isPresent()) return recipeName;

        Material material = resolveVanillaMaterial(key);
        return material == null ? Optional.empty() : Optional.of(prettyVanillaName(material));
    }

    private static Material resolveVanillaMaterial(String key) {
        String value = key.trim();
        if (value.contains(":")) {
            String namespace = value.substring(0, value.indexOf(':'));
            if (!"minecraft".equalsIgnoreCase(namespace)) return null;
            value = value.substring(value.indexOf(':') + 1);
        }
        return Material.matchMaterial(value);
    }

    private static String prettyVanillaName(Material material) {
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
