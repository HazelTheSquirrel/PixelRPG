package de.pixelrpg.rpg.profession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Registry for profession-aware crafting. Vanilla recipes are discovered from the server; PixelRPG-only recipes come from JSON. */
public final class CraftingRecipeRegistry {
    private final Map<String, CraftRecipe> recipes = new LinkedHashMap<>();

    public void load(Plugin plugin) {
        recipes.clear();
        loadVanilla(plugin);
        loadDefinitions(plugin);
    }

    public List<CraftRecipe> getRecipes(Profession profession) {
        return recipes.values().stream().filter(recipe -> recipe.profession() == profession).toList();
    }

    public Optional<CraftRecipe> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(recipes.get(id.toLowerCase(Locale.ROOT)));
    }

    private void loadVanilla(Plugin plugin) {
        var iterator = plugin.getServer().recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe == null || recipe.getResult() == null || recipe.getResult().getType().isAir()) continue;
            Map<Material, Integer> costs = ingredientCosts(recipe);
            if (costs.isEmpty()) continue;
            Material result = recipe.getResult().getType();
            Profession profession = classify(result);
            if (profession == null) continue;
            String id = "vanilla:" + stableId(result, costs);
            ItemStack output = recipe.getResult();
            recipes.putIfAbsent(id, new CraftRecipe(profession, id, pretty(result), result, output.getAmount(), ItemRarity.COMMON,
                    costs, 1, 0L, "", true, true, ""));
        }
    }

    private void loadDefinitions(Plugin plugin) {
        JsonObject root = new JsonDataManager(plugin).load("crafting-recipes.json");
        JsonArray definitions = root.getAsJsonArray("recipes");
        if (definitions == null) throw new IllegalStateException("crafting-recipes.json requires a 'recipes' array");
        for (var element : definitions) {
            JsonObject json = element.getAsJsonObject();
            String id = required(json, "id").toLowerCase(Locale.ROOT);
            if (recipes.containsKey(id)) throw new IllegalStateException("Duplicate crafting recipe: " + id);
            Profession profession = enumValue(Profession.class, json, "profession", id);
            Material result = Material.matchMaterial(required(json, "result"));
            if (result == null || result.isAir()) throw new IllegalStateException("Unknown crafting result for " + id);
            ItemRarity rarity = enumValue(ItemRarity.class, json, "rarity", id);
            Map<Material, Integer> costs = parseCosts(json, id);
            int level = json.has("requiredProfessionLevel") ? json.get("requiredProfessionLevel").getAsInt() : 1;
            int amount = json.has("resultAmount") ? json.get("resultAmount").getAsInt() : 1;
            long price = json.has("unlockPrice") ? json.get("unlockPrice").getAsLong() : 0L;
            String quest = json.has("requiredQuestId") ? json.get("requiredQuestId").getAsString() : "";
            boolean defaultUnlocked = json.has("unlockedByDefault") && json.get("unlockedByDefault").getAsBoolean();
            String label = json.has("label") ? json.get("label").getAsString() : pretty(result);
            String resultItemId = json.has("resultItemId") ? json.get("resultItemId").getAsString() : "";
            recipes.put(id, new CraftRecipe(profession, id, label, result, amount, rarity, costs, level, price, quest, defaultUnlocked, false, resultItemId));
        }
    }

    private static Map<Material, Integer> ingredientCosts(Recipe recipe) {
        List<Material> materials = new ArrayList<>();
        if (recipe instanceof ShapedRecipe shaped) {
            for (ItemStack item : shaped.getIngredientMap().values()) if (item != null && !item.getType().isAir()) materials.add(item.getType());
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            for (ItemStack item : shapeless.getIngredientList()) if (item != null && !item.getType().isAir()) materials.add(item.getType());
        } else if (recipe instanceof StonecuttingRecipe stonecutting) {
            addSingleChoice(materials, stonecutting.getInputChoice());
        } else if (recipe instanceof FurnaceRecipe furnace) {
            addSingleChoice(materials, furnace.getInputChoice());
        } else if (recipe instanceof BlastingRecipe blasting) {
            addSingleChoice(materials, blasting.getInputChoice());
        } else if (recipe instanceof SmokingRecipe smoking) {
            addSingleChoice(materials, smoking.getInputChoice());
        } else if (recipe instanceof CampfireRecipe campfire) {
            addSingleChoice(materials, campfire.getInputChoice());
        }
        Map<Material, Integer> costs = new EnumMap<>(Material.class);
        for (Material material : materials) costs.merge(material, 1, Integer::sum);
        return costs;
    }

    private static void addSingleChoice(List<Material> materials, RecipeChoice choice) {
        List<Material> choices = choiceMaterials(choice);
        if (choices.size() == 1) materials.add(choices.getFirst());
    }

    private static List<Material> choiceMaterials(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.MaterialChoice materialChoice) return materialChoice.getChoices();
        if (choice instanceof RecipeChoice.ExactChoice exactChoice) return exactChoice.getChoices().stream().map(ItemStack::getType).toList();
        return List.of();
    }

    private static Profession classify(Material result) {
        if (result.isEdible()) return Profession.PROVISIONER;
        String name = result.name();
        if (name.equals("POTION") || name.equals("SPLASH_POTION") || name.equals("LINGERING_POTION") || name.contains("BREWING") || name.equals("BLAZE_POWDER") || name.equals("GHAST_TEAR") || name.equals("FERMENTED_SPIDER_EYE") || name.equals("GLISTERING_MELON_SLICE") || name.equals("MAGMA_CREAM")) return Profession.ALCHEMIST;
        if (name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.equals("SHIELD") || name.equals("MACE") || name.contains("ANVIL") || name.equals("SMITHING_TABLE") || name.equals("GRINDSTONE") || name.equals("STONECUTTER") || name.equals("CAULDRON")) return Profession.BLACKSMITH;
        if (name.contains("BOOK") || name.contains("MAP") || name.equals("COMPASS") || name.equals("CLOCK") || name.equals("ENCHANTING_TABLE") || name.equals("LODESTONE") || name.equals("BEACON") || name.equals("ENDER_EYE") || name.equals("EXPERIENCE_BOTTLE")) return Profession.SCHOLAR;
        if (name.contains("SEEDS") || name.contains("WHEAT") || name.contains("CARROT") || name.contains("POTATO") || name.contains("BOWL") || name.contains("HONEY") || name.equals("CAKE") || name.equals("PUMPKIN_PIE")) return Profession.PROVISIONER;
        return null;
    }

    private static Map<Material, Integer> parseCosts(JsonObject json, String id) {
        JsonObject costs = json.getAsJsonObject("costs");
        if (costs == null || costs.isEmpty()) throw new IllegalStateException("Missing costs for " + id);
        Map<Material, Integer> result = new EnumMap<>(Material.class);
        for (var entry : costs.entrySet()) {
            Material material = Material.matchMaterial(entry.getKey());
            int amount = entry.getValue().getAsInt();
            if (material == null || material.isAir() || amount <= 0) throw new IllegalStateException("Invalid cost for " + id + ": " + entry.getKey());
            result.merge(material, amount, Integer::sum);
        }
        return result;
    }

    private static String stableId(Material result, Map<Material, Integer> costs) {
        String ingredients = costs.entrySet().stream().map(entry -> entry.getKey().name() + "x" + entry.getValue()).sorted().reduce((a, b) -> a + ";" + b).orElse("none");
        return result.name().toLowerCase(Locale.ROOT) + ":" + Integer.toUnsignedString((result.name() + "|" + ingredients).hashCode(), 36);
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
