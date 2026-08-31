package de.pixelrpg.rpg.profession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Registry for PixelRPG-only profession recipes. Recipe data is kept under data/recipes for clean content separation. */
public final class CraftingRecipeRegistry {
    private static final String RECIPE_DATA_PATH = "recipes/crafting-recipes.json";
    private final Map<String, CraftRecipe> recipes = new LinkedHashMap<>();

    public void load(Plugin plugin) {
        recipes.clear();
        loadDefinitions(plugin);
    }

    public List<CraftRecipe> getRecipes(Profession profession) {
        return recipes.values().stream().filter(recipe -> recipe.profession() == profession).toList();
    }

    public Optional<CraftRecipe> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(recipes.get(id.toLowerCase(Locale.ROOT)));
    }

    private void loadDefinitions(Plugin plugin) {
        JsonObject root = new JsonDataManager(plugin).load(RECIPE_DATA_PATH);
        JsonArray definitions = root.getAsJsonArray("recipes");
        if (definitions == null) throw new IllegalStateException("crafting-recipes.json requires a 'recipes' array");
        for (var element : definitions) {
            JsonObject json = element.getAsJsonObject();
            String id = required(json, "id").toLowerCase(Locale.ROOT);
            if (recipes.containsKey(id)) throw new IllegalStateException("Duplicate crafting recipe: " + id);
            Profession profession = enumValue(Profession.class, json, "profession", id);
            Material result = Material.matchMaterial(required(json, "result"));
            if (result == null || result.isAir()) throw new IllegalStateException("Unknown crafting result for " + id);
            ItemRarity maximumRarity = enumValue(ItemRarity.class, json, "rarity", id);
            if (maximumRarity == ItemRarity.UNIQUE) throw new IllegalStateException("UNIQUE is not valid for craftable recipe " + id);
            Map<Material, Integer> costs = parseCosts(json, id);
            Map<String, Integer> itemCosts = parseItemCosts(json, id);
            int level = json.has("requiredProfessionLevel") ? json.get("requiredProfessionLevel").getAsInt() : 1;
            int amount = json.has("resultAmount") ? json.get("resultAmount").getAsInt() : 1;
            long price = json.has("unlockPrice") ? json.get("unlockPrice").getAsLong() : 0L;
            String quest = json.has("requiredQuestId") ? json.get("requiredQuestId").getAsString() : "";
            boolean defaultUnlocked = json.has("unlockedByDefault") && json.get("unlockedByDefault").getAsBoolean();
            String label = json.has("label") ? json.get("label").getAsString() : pretty(result);
            String resultItemId = json.has("resultItemId") ? json.get("resultItemId").getAsString() : "";
            String potionType = json.has("potionType") ? json.get("potionType").getAsString() : "";
            String enchantment = json.has("enchantment") ? json.get("enchantment").getAsString() : "";
            int enchantmentLevel = json.has("enchantmentLevel") ? json.get("enchantmentLevel").getAsInt() : 0;
            recipes.put(id, new CraftRecipe(profession, id, label, result, amount, maximumRarity, costs, itemCosts,
                    level, price, quest, defaultUnlocked, false, resultItemId, potionType, enchantment, enchantmentLevel));
        }
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

    private static Map<String, Integer> parseItemCosts(JsonObject json, String id) {
        JsonObject costs = json.has("itemCosts") ? json.getAsJsonObject("itemCosts") : null;
        if (costs == null || costs.isEmpty()) return Map.of();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var entry : costs.entrySet()) {
            String itemId = entry.getKey().trim().toLowerCase(Locale.ROOT);
            int amount = entry.getValue().getAsInt();
            if (itemId.isBlank() || amount <= 0) throw new IllegalStateException("Invalid item cost for " + id + ": " + entry.getKey());
            result.merge(itemId, amount, Integer::sum);
        }
        return result;
    }

    private static String required(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) throw new IllegalStateException("Missing '" + key + "' in crafting recipe");
        return json.get(key).getAsString();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, JsonObject json, String key, String id) {
        try { return Enum.valueOf(type, required(json, key).toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("Invalid " + key + " for " + id, exception); }
    }

    private static String pretty(Material material) {
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
