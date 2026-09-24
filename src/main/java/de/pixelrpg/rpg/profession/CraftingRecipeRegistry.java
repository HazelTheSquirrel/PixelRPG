package de.pixelrpg.rpg.profession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.item.ItemRarity;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

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
        Material material = resolveMaterial(itemId);
        if (material == null || material.isAir()) return Optional.empty();
        return recipes.values().stream()
                .filter(recipe -> recipe.resultMaterial() == material)
                .map(CraftRecipe::displayName)
                .findFirst();
    }

    public boolean hasResultItemId(String itemId) {
        return findDisplayNameByResultItemId(itemId).isPresent();
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
            CraftingCategory category = CraftingCategory.from(required(json, "category"), id);
            if (profession == Profession.WOODCUTTER || profession == Profession.FISHERMAN) {
                throw new IllegalStateException("Passive profession cannot define crafting recipes: " + id);
            }

            Material result = resolveMaterial(required(json, "result"));
            if (result == null || result.isAir()) throw new IllegalStateException("Unknown crafting result for " + id + ": " + required(json, "result"));

            ItemRarity maximumRarity = enumValue(ItemRarity.class, json, "rarity", id);
            if (maximumRarity == ItemRarity.UNIQUE) throw new IllegalStateException("UNIQUE is not valid for craftable recipe " + id);

            Map<Material, Integer> costs = parseCosts(json, id);
            if (json.has("itemCosts") && !json.getAsJsonObject("itemCosts").isEmpty()) {
                throw new IllegalStateException("Custom PixelRPG itemCosts are not allowed for vanilla profession recipes: " + id);
            }
            Map<String, Integer> itemCosts = Map.of();

            int level = integerField(json, "requiredProfessionLevel", id, 1);
            if (level < Profession.MIN_LEVEL || level > Profession.MAX_LEVEL) {
                throw new IllegalStateException("Invalid requiredProfessionLevel for " + id + ": " + level);
            }

            int amount = integerField(json, "resultAmount", id, 1);
            if (amount <= 0) throw new IllegalStateException("Invalid resultAmount for " + id + ": " + amount);

            long price = longField(json, "unlockPrice", id, 0L);
            if (price < 0L) throw new IllegalStateException("Invalid unlockPrice for " + id + ": " + price);

            String quest = json.has("requiredQuestId") ? json.get("requiredQuestId").getAsString() : "";
            boolean defaultUnlocked = json.has("unlockedByDefault") && json.get("unlockedByDefault").getAsBoolean();
            String label = json.has("label") ? json.get("label").getAsString() : pretty(result);
            if (json.has("resultItemId") && !json.get("resultItemId").getAsString().isBlank()) {
                throw new IllegalStateException("Custom PixelRPG resultItemId is not allowed for vanilla profession recipes: " + id);
            }

            String potionType = json.has("potionType") ? json.get("potionType").getAsString() : "";
            String enchantment = json.has("enchantment") ? json.get("enchantment").getAsString() : "";
            int enchantmentLevel = integerField(json, "enchantmentLevel", id, 0);
            validateSpecialFields(id, result, potionType, enchantment, enchantmentLevel);

            recipes.put(id, new CraftRecipe(profession, category, id, label, result, amount, maximumRarity, costs, itemCosts,
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

    private static Material resolveMaterial(String raw) {
        String value = raw.trim().toUpperCase(Locale.ROOT);

        // Keep compatibility with the historical recipe data names while resolving
        // to the canonical Paper 26.2 Material names. Do not use a broad GOLD_*
        // prefix replacement: GOLD_INGOT, GOLD_NUGGET, GOLD_BLOCK and GOLD_ORE
        // are already canonical names and have no GOLDEN_* counterpart.
        value = switch (value) {
            case "GOLD_AXE" -> "GOLDEN_AXE";
            case "GOLD_BOOTS" -> "GOLDEN_BOOTS";
            case "GOLD_CHESTPLATE" -> "GOLDEN_CHESTPLATE";
            case "GOLD_HELMET" -> "GOLDEN_HELMET";
            case "GOLD_HOE" -> "GOLDEN_HOE";
            case "GOLD_LEGGINGS" -> "GOLDEN_LEGGINGS";
            case "GOLD_PICKAXE" -> "GOLDEN_PICKAXE";
            case "GOLD_SHOVEL" -> "GOLDEN_SHOVEL";
            case "GOLD_SPEAR" -> "GOLDEN_SPEAR";
            case "GOLD_SWORD" -> "GOLDEN_SWORD";
            // Historical aliases used by older PixelRPG data.
            case "DIAMOND_INGOT" -> "DIAMOND";
            case "CHAIN" -> "IRON_CHAIN";
            default -> value;
        };

        return Material.matchMaterial(value);
    }

    private static void validateSpecialFields(String id, Material result, String potionType, String enchantment, int enchantmentLevel) {
        if (!potionType.isBlank()) {
            if (result != Material.POTION && result != Material.SPLASH_POTION && result != Material.LINGERING_POTION) {
                throw new IllegalStateException("potionType requires a potion result for " + id + ": " + result);
            }
            try {
                PotionType.valueOf(potionType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Invalid potionType for " + id + ": " + potionType, exception);
            }
        }

        if (!enchantment.isBlank()) {
            if (result != Material.ENCHANTED_BOOK) {
                throw new IllegalStateException("Enchantment recipe must produce ENCHANTED_BOOK: " + id);
            }
            if (enchantmentLevel <= 0) {
                throw new IllegalStateException("Invalid enchantmentLevel for " + id + ": " + enchantmentLevel);
            }
            var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
            var value = registry.get(NamespacedKey.minecraft(enchantment.trim().toLowerCase(Locale.ROOT)));
            if (value == null) {
                throw new IllegalStateException("Unknown enchantment for " + id + ": " + enchantment);
            }
            if (enchantmentLevel > value.getMaxLevel()) {
                throw new IllegalStateException("enchantmentLevel exceeds vanilla maximum for " + id + ": "
                        + enchantment + " " + enchantmentLevel + " > " + value.getMaxLevel());
            }
        } else if (enchantmentLevel != 0) {
            throw new IllegalStateException("enchantmentLevel requires an enchantment for " + id + ": " + enchantmentLevel);
        }
    }

    private static int integerField(JsonObject json, String key, String id, int defaultValue) {
        if (!json.has(key)) return defaultValue;
        if (!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isNumber()) {
            throw new IllegalStateException(key + " must be a JSON number for " + id);
        }
        return json.get(key).getAsInt();
    }

    private static long longField(JsonObject json, String key, String id, long defaultValue) {
        if (!json.has(key)) return defaultValue;
        if (!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isNumber()) {
            throw new IllegalStateException(key + " must be a JSON number for " + id);
        }
        return json.get(key).getAsLong();
    }

    private static Map<Material, Integer> parseCosts(JsonObject json, String id) {
        JsonObject costs = json.has("costs") ? json.getAsJsonObject("costs") : null;
        if (costs == null || costs.isEmpty()) return Map.of();
        Map<Material, Integer> result = new EnumMap<>(Material.class);
        for (var entry : costs.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                throw new IllegalStateException("Cost amount must be a JSON number for " + id + ": " + entry.getKey());
            }
            Material material = resolveMaterial(entry.getKey());
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
