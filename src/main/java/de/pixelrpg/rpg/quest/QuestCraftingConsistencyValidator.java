package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.item.ItemDefinitionRegistry;
import de.pixelrpg.rpg.profession.CraftRecipe;
import de.pixelrpg.rpg.profession.CraftingRecipeRegistry;
import de.pixelrpg.rpg.profession.Profession;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cross-validates quest and crafting content after both registries are loaded.
 * This prevents content drift from reaching runtime crafting/quest handling.
 */
public final class QuestCraftingConsistencyValidator {
    private QuestCraftingConsistencyValidator() {
    }

    public static void validate(
            Map<String, Quest> questsById,
            CraftingRecipeRegistry recipes,
            ItemDefinitionRegistry itemDefinitions
    ) {
        Map<String, CraftRecipe> recipesById = new HashMap<>();
        Map<String, String> recipeByResult = new HashMap<>();

        for (Profession profession : Profession.values()) {
            for (CraftRecipe recipe : recipes.getRecipes(profession)) {
                String previous = recipesById.put(recipe.id(), recipe.id());
                if (previous != null) {
                    throw new IllegalStateException("Duplicate recipe ID after normalization: " + recipe.id());
                }

                if (recipe.requiredQuestId() != null && !recipe.requiredQuestId().isBlank()
                        && !questsById.containsKey(recipe.requiredQuestId())) {
                    throw new IllegalStateException("Recipe '" + recipe.id()
                            + "' references unknown required quest '" + recipe.requiredQuestId() + "'.");
                }

                if (!recipe.resultItemId().isBlank()) {
                    String resultId = canonicalItemId(recipe.resultItemId());
                    String previousRecipe = recipeByResult.put(resultId, recipe.id());
                    if (previousRecipe != null && !previousRecipe.equals(recipe.id())) {
                        throw new IllegalStateException("Recipes '" + previousRecipe + "' and '"
                                + recipe.id() + "' produce the same PixelRPG item '" + resultId + "'.");
                    }
                }

                for (String itemCost : recipe.itemCosts().keySet()) {
                    String itemId = canonicalItemId(itemCost);
                    if (!itemId.startsWith("pixelrpg:")) {
                        throw new IllegalStateException("Recipe '" + recipe.id()
                                + "' has invalid custom item cost '" + itemCost + "'.");
                    }
                    if (!itemDefinitions.find(itemId).isPresent() && !recipeByResult.containsKey(itemId)) {
                        // Earlier recipes are already indexed. Later references are
                        // checked in the second pass below.
                    }
                }
            }
        }

        for (Profession profession : Profession.values()) {
            for (CraftRecipe recipe : recipes.getRecipes(profession)) {
                for (String itemCost : recipe.itemCosts().keySet()) {
                    String itemId = canonicalItemId(itemCost);
                    if (!itemDefinitions.find(itemId).isPresent() && !recipeByResult.containsKey(itemId)) {
                        throw new IllegalStateException("Recipe '" + recipe.id()
                                + "' references unknown crafted/item cost '" + itemCost + "'.");
                    }
                }
            }
        }

        for (Quest quest : questsById.values()) {
            if (quest.type() != QuestType.COLLECT || !quest.targetKey().startsWith("pixelrpg:")) continue;

            String targetId = canonicalItemId(quest.targetKey());
            if (!itemDefinitions.find(targetId).isPresent() && !recipeByResult.containsKey(targetId)) {
                throw new IllegalStateException("Quest '" + quest.id()
                        + "' references unknown PixelRPG collect item '" + quest.targetKey() + "'.");
            }
        }
    }

    private static String canonicalItemId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith("pixelrpg:")) return value;
        String body = value.substring("pixelrpg:".length()).replace('/', ':');
        while (body.contains("::")) body = body.replace("::", ":");
        return "pixelrpg:" + body;
    }
}
