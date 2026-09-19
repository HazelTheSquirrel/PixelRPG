package de.pixelrpg.rpg.profession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Registry for profession recipes whose inputs and outputs are vanilla Minecraft items. */
public final class CraftingRecipeRegistry {
    private static final String RECIPE_DATA_PATH = "recipes/crafting-recipes.json";
    private final Map<String, CraftRecipe> recipes = new LinkedHashMap<>();

    public void load(Plugin plugin) {
        recipes.clear();
        loadDefinitions(plugin);
        validateDependencies();
    }

    public List<CraftRecipe> getRecipes(Profession profession) {
        return recipes.values().stream().filter(recipe -> recipe.profession() == profession).toList();
    }

    public Optional<CraftRecipe> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(recipes.get(canonicalRecipeId(id)));
    }

    public Optional<String> findDisplayNameByResultItemId(String itemId) {
        return Optional.empty();
    }

    public boolean hasResultItemId(String itemId) {
        return false;
    }

    private void loadDefinitions(Plugin plugin) {
        JsonObject root = new JsonDataManager(plugin).load(RECIPE_DATA_PATH);
        JsonArray definitions = root.getAsJsonArray("recipes");
        if (definitions == null) throw new IllegalStateException("crafting-recipes.json requires a 'recipes' array");

        for (var element : definitions) {
            if (!element.isJsonObject()) throw new IllegalStateException("Each crafting recipe definition must be a JSON object");
            JsonObject json = element.getAsJsonObject();
            String id = canonicalRecipeId(required(json, "id"));
            if (recipes.containsKey(id)) throw new IllegalStateException("Duplicate crafting recipe: " + id);

            Profession profession = enumValue(Profession.class, json, "profession", id);
            if (profession == Profession.WOODCUTTER || profession == Profession.FISHERMAN) {
                throw new IllegalStateException("Passive profession cannot define crafting recipes: " + id);
            }

            Material result = resolveResultMaterial(required(json, "result"));
            if (result == null || result.isAir()) throw new IllegalStateException("Unknown crafting result for " + id + ": " + required(json, "result"));

            ItemRarity maximumRarity = enumValue(ItemRarity.class, json, "rarity", id);
            if (maximumRarity == ItemRarity.UNIQUE) throw new IllegalStateException("UNIQUE is not valid for craftable recipe " + id);

            Map<Material, Integer> costs = parseCosts(json, id);
            if (json.has("itemCosts") && !json.getAsJsonObject("itemCosts").isEmpty()) {
                throw new IllegalStateException("Custom PixelRPG itemCosts are not allowed for vanilla profession recipes: " + id);
            }
            Map<String, Integer> itemCosts = Map.of();

            int level = json.has("requiredProfessionLevel") ? json.get("requiredProfessionLevel").getAsInt() : 1;
            if (level < Profession.MIN_LEVEL || level > Profession.MAX_LEVEL) {
                throw new IllegalStateException("Invalid requiredProfessionLevel for " + id + ": " + level);
            }

            int amount = json.has("resultAmount") ? json.get("resultAmount").getAsInt() : 1;
            if (amount <= 0) throw new IllegalStateException("Invalid resultAmount for " + id + ": " + amount);

            long price = json.has("unlockPrice") ? json.get("unlockPrice").getAsLong() : 0L;
            if (price < 0L) throw new IllegalStateException("Invalid unlockPrice for " + id + ": " + price);

            String quest = json.has("requiredQuestId") ? json.get("requiredQuestId").getAsString() : "";
            boolean defaultUnlocked = json.has("unlockedByDefault") && json.get("unlockedByDefault").getAsBoolean();
            String label = json.has("label") ? json.get("label").getAsString() : pretty(result);
            if (json.has("resultItemId") && !json.get("resultItemId").getAsString().isBlank()) {
                throw new IllegalStateException("Custom PixelRPG resultItemId is not allowed for vanilla profession recipes: " + id);
            }

            String potionType = json.has("potionType") ? json.get("potionType").getAsString() : "";
            String enchantment = json.has("enchantment") ? json.get("enchantment").getAsString() : "";
            int enchantmentLevel = json.has("enchantmentLevel") ? json.get("enchantmentLevel").getAsInt() : 0;

            recipes.put(id, new CraftRecipe(profession, id, label, result, amount, maximumRarity, costs, itemCosts,
                    level, price, quest, defaultUnlocked, false, "", potionType, enchantment, enchantmentLevel));
        }
    }

    private void validateDependencies() {
        // Kept as a compatibility guard: vanilla profession recipes intentionally have no recipe dependencies.
        for (CraftRecipe recipe : recipes.values()) {
            if (!recipe.itemCosts().isEmpty()) {
                throw new IllegalStateException("Vanilla profession recipes cannot depend on custom items: " + recipe.id());
            }
        }

        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String recipeId : recipes.keySet()) {
            validateDependencyPath(recipeId, visiting, visited);
        }
    }

    private void validateDependencyPath(String recipeId, Set<String> visiting, Set<String> visited) {
        if (visited.contains(recipeId)) return;
        if (!visiting.add(recipeId)) throw new IllegalStateException("Circular crafting recipe dependency detected at " + recipeId);
        visiting.remove(recipeId);
        visited.add(recipeId);
    }

    private static Material resolveResultMaterial(String raw) {
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (value.equals("CHAIN")) value = "IRON_CHAIN";
        return Material.matchMaterial(value);
    }

    private static Map<Material, Integer> parseCosts(JsonObject json, String id) {
        JsonObject costs = json.has("costs") ? json.getAsJsonObject("costs") : null;
        if (costs == null || costs.isEmpty()) return Map.of();
        Map<Material, Integer> result = new EnumMap<>(Material.class);
        for (var entry : costs.entrySet()) {
            Material material = Material.matchMaterial(entry.getKey());
            int amount = entry.getValue().getAsInt();
            if (material == null || material.isAir() || amount <= 0) throw new IllegalStateException("Invalid cost for " + id + ": " + entry.getKey());
            result.merge(material, amount, Integer::sum);
        }
        return result;
    }

    private static String canonicalRecipeId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("pixelrpg:")) value = value.substring("pixelrpg:".length());
        return value.replace('/', ':');
    }

    private static String required(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) throw new IllegalStateException("Missing '" + key + "' in crafting recipe");
        return json.get(key).getAsString();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, JsonObject json, String key, String id) {
        try {
            return Enum.valueOf(type, required(json, key).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid " + key + " for " + id, exception);
        }
    }

    private static String pretty(Material material) {
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
