package de.pixelrpg.rpg.profession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Registry for PixelRPG profession recipes and their cross-profession production chains. */
public final class CraftingRecipeRegistry {
    private static final String RECIPE_DATA_PATH = "recipes/crafting-recipes.json";
    private final Map<String, CraftRecipe> recipes = new LinkedHashMap<>();

    public void load(Plugin plugin) {
        recipes.clear();
        loadDefinitions(plugin);
        validateProfessionEconomy();
    }

    public List<CraftRecipe> getRecipes(Profession profession) {
        return recipes.values().stream().filter(recipe -> recipe.profession() == profession).toList();
    }

    public Optional<CraftRecipe> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(recipes.get(canonicalRecipeId(id)));
    }

    public Optional<String> findDisplayNameByResultItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) return Optional.empty();
        String canonical = canonicalItemId(itemId);
        List<String> labels = recipes.values().stream()
                .filter(recipe -> !recipe.resultItemId().isBlank())
                .filter(recipe -> canonicalItemId(recipe.resultItemId()).equals(canonical))
                .map(CraftRecipe::displayName).distinct().toList();
        return labels.size() == 1 ? Optional.of(labels.getFirst()) : Optional.empty();
    }

    public boolean hasResultItemId(String itemId) {
        String canonical = canonicalItemId(itemId);
        return recipes.values().stream().anyMatch(recipe -> !recipe.resultItemId().isBlank()
                && canonicalItemId(recipe.resultItemId()).equals(canonical));
    }

    private void loadDefinitions(Plugin plugin) {
        JsonObject root = new JsonDataManager(plugin).load(RECIPE_DATA_PATH);
        mergeMissingGeneratedProfessionGroups(plugin, root);
        if (root.has("generatedProfessionRecipes")) loadGeneratedDefinitions(root.getAsJsonObject("generatedProfessionRecipes"));
        if (root.has("recipes")) loadExplicitDefinitions(root.getAsJsonArray("recipes"));
        if (recipes.isEmpty()) throw new IllegalStateException("crafting-recipes.json requires 'recipes' or 'generatedProfessionRecipes'");
    }

    /** Repairs missing profession groups in an existing editable server data file without overwriting custom groups. */
    private void mergeMissingGeneratedProfessionGroups(Plugin plugin, JsonObject root) {
        if (!root.has("generatedProfessionRecipes") || !root.get("generatedProfessionRecipes").isJsonObject()) return;
        JsonObject configured = root.getAsJsonObject("generatedProfessionRecipes");
        JsonObject bundled = loadBundledRecipeDefaults(plugin).getAsJsonObject("generatedProfessionRecipes");
        boolean changed = false;
        for (Profession profession : Profession.values()) {
            String key = profession.name();
            if (!configured.has(key) && bundled.has(key)) {
                configured.add(key, bundled.get(key).deepCopy());
                changed = true;
            }
        }
        if (changed) {
            new JsonDataManager(plugin).save(RECIPE_DATA_PATH, root);
            plugin.getLogger().warning("Repaired missing profession recipe groups in " + RECIPE_DATA_PATH + " from bundled defaults.");
        }
    }

    private static JsonObject loadBundledRecipeDefaults(Plugin plugin) {
        try (var input = plugin.getResource("data/" + RECIPE_DATA_PATH)) {
            if (input == null) throw new IllegalStateException("Missing bundled JSON resource: data/" + RECIPE_DATA_PATH);
            try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                var element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) throw new IllegalStateException("Bundled crafting recipe root must be an object");
                return element.getAsJsonObject();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load bundled crafting recipe defaults", exception);
        }
    }

    private void loadExplicitDefinitions(JsonArray definitions) {
        if (definitions == null) throw new IllegalStateException("crafting-recipes.json requires a 'recipes' array");
        for (var element : definitions) {
            JsonObject json = element.getAsJsonObject();
            String id = canonicalRecipeId(required(json, "id"));
            if (recipes.containsKey(id)) throw new IllegalStateException("Duplicate crafting recipe: " + id);
            Profession profession = parseProfession(json, id);
            Material result = resolveResultMaterial(required(json, "result"));
            if (result == null || result.isAir()) throw new IllegalStateException("Unknown crafting result for " + id + ": " + required(json, "result"));
            addRecipe(id, json.has("label") ? json.get("label").getAsString() : pretty(result), profession, result,
                    json.has("resultAmount") ? json.get("resultAmount").getAsInt() : 1,
                    enumValue(ItemRarity.class, json, "rarity", id), parseCosts(json, id), parseItemCosts(json, id),
                    json.has("requiredProfessionLevel") ? json.get("requiredProfessionLevel").getAsInt() : 1,
                    json.has("unlockPrice") ? json.get("unlockPrice").getAsLong() : 0L,
                    json.has("requiredQuestId") ? json.get("requiredQuestId").getAsString() : "",
                    json.has("unlockedByDefault") && json.get("unlockedByDefault").getAsBoolean(),
                    json.has("resultItemId") ? canonicalItemId(json.get("resultItemId").getAsString()) : "",
                    json.has("potionType") ? json.get("potionType").getAsString() : "",
                    json.has("enchantment") ? json.get("enchantment").getAsString() : "",
                    json.has("enchantmentLevel") ? json.get("enchantmentLevel").getAsInt() : 0);
        }
    }

    private void loadGeneratedDefinitions(JsonObject professions) {
        Map<Profession, JsonArray> labelsByProfession = new EnumMap<>(Profession.class);
        for (Profession profession : Profession.values()) {
            String key = profession.name();
            if (!professions.has(key) || !professions.get(key).isJsonObject()) {
                throw new IllegalStateException("Missing generated recipe group for " + key);
            }
            JsonObject group = professions.getAsJsonObject(key);
            JsonArray labels = group.getAsJsonArray("labels");
            JsonArray results = group.getAsJsonArray("results");
            if (labels == null || results == null || labels.isEmpty() || labels.size() != results.size()) {
                throw new IllegalStateException("Generated recipe group " + key + " must contain matching non-empty labels and results");
            }
            labelsByProfession.put(profession, labels);
        }

        for (Profession profession : Profession.values()) {
            JsonArray labels = labelsByProfession.get(profession);
            JsonArray results = professions.getAsJsonObject(profession.name()).getAsJsonArray("results");
            for (int index = 0; index < labels.size(); index++) {
                int level = index == 0 ? 1 : Math.min(100, index * 5);
                String label = labels.get(index).getAsString();
                if (label.isBlank()) throw new IllegalStateException("Blank generated recipe label for " + profession + " #" + (index + 1));
                Material result = resolveResultMaterial(results.get(index).getAsString());
                if (result == null || result.isAir()) throw new IllegalStateException("Unknown generated result for " + profession + " #" + (index + 1));
                String recipeId = profession.name().toLowerCase(Locale.ROOT) + ":" + String.format(Locale.ROOT, "%02d_%s", index + 1, slug(label));
                if (recipes.containsKey(recipeId)) throw new IllegalStateException("Duplicate generated crafting recipe: " + recipeId);
                addRecipe(recipeId, label, profession, result, generatedResultAmount(profession, index), generatedRarity(index),
                        generatedMaterialCosts(profession, index), generatedItemCosts(profession, index, labelsByProfession), level,
                        generatedUnlockPrice(index), "", index == 0, "pixelrpg:" + recipeId,
                        generatedPotionType(profession, index), generatedEnchantment(profession, index), generatedEnchantmentLevel(profession, index));
            }
        }
    }

    private void addRecipe(String id, String label, Profession profession, Material result, int resultAmount, ItemRarity rarity,
                           Map<Material, Integer> costs, Map<String, Integer> itemCosts, int level, long unlockPrice,
                           String requiredQuestId, boolean unlockedByDefault, String resultItemId, String potionType,
                           String enchantment, int enchantmentLevel) {
        recipes.put(id, new CraftRecipe(profession, id, label, result, resultAmount, rarity, costs, itemCosts, level,
                unlockPrice, requiredQuestId, unlockedByDefault, false, resultItemId, potionType, enchantment, enchantmentLevel));
    }

    private static int generatedResultAmount(Profession profession, int index) {
        return switch (profession) {
            case FARMER, FISHERMAN -> index < 5 ? 4 : index < 12 ? 3 : 2;
            case COOK -> index < 6 ? 2 : 1;
            case MASON, WOODCUTTER -> index < 10 ? 4 : index < 17 ? 2 : 1;
            default -> 1;
        };
    }

    private static long generatedUnlockPrice(int index) { return 75L + (index * 125L) + ((long) index * index * 10L); }

    private static ItemRarity generatedRarity(int index) {
        return switch (index) {
            case 0, 1, 2, 3 -> ItemRarity.COMMON;
            case 4, 5, 6, 7 -> ItemRarity.UNCOMMON;
            case 8, 9, 10, 11 -> ItemRarity.RARE;
            case 12, 13, 14, 15 -> ItemRarity.EPIC;
            default -> ItemRarity.LEGENDARY;
        };
    }

    private static Map<String, Integer> generatedItemCosts(Profession profession, int index, Map<Profession, JsonArray> labelsByProfession) {
        Map<String, Integer> costs = new LinkedHashMap<>();
        if (index > 0) costs.put(generatedItemId(profession, labelsByProfession, index - 1), 1);

        switch (profession) {
            case BLACKSMITH -> {
                switch (index) {
                    case 7 -> costs.put(generatedItemId(profession, labelsByProfession, 1), 1);
                    case 8 -> costs.put(generatedItemId(profession, labelsByProfession, 2), 1);
                    case 9 -> costs.put(generatedItemId(profession, labelsByProfession, 8), 1);
                    case 10 -> costs.put(generatedItemId(profession, labelsByProfession, 9), 1);
                    case 11, 12, 13, 14 -> costs.put(generatedItemId(profession, labelsByProfession, 10), index == 14 ? 2 : 1);
                    case 15 -> costs.put(generatedItemId(profession, labelsByProfession, 10), 1);
                    case 16 -> costs.put(generatedItemId(profession, labelsByProfession, 11), 1);
                    case 17 -> costs.put(generatedItemId(profession, labelsByProfession, 12), 1);
                    case 18 -> costs.put(generatedItemId(profession, labelsByProfession, 13), 1);
                    case 19 -> costs.put(generatedItemId(profession, labelsByProfession, 14), 1);
                    default -> { }
                }
            }
            case SCHOLAR -> {
                if (index == 18) costs.put(generatedItemId(profession, labelsByProfession, 17), 1);
                else if (index == 15) {
                    costs.put(generatedItemId(profession, labelsByProfession, 14), 1);
                    costs.put(generatedItemId(profession, labelsByProfession, 6), 1);
                }
                if (index == 18) costs.put(generatedItemId(Profession.ALCHEMIST, labelsByProfession, 16), 1);
            }
            case FARMER, TAILOR, FISHERMAN, WOODCUTTER -> { }
            case COOK -> {
                if (index == 2 || index == 3 || index == 5 || index == 7 || index == 10 || index == 13 || index == 15) {
                    costs.put(farmerDependency(index, labelsByProfession), 1);
                }
                switch (index) {
                    case 0 -> costs.put(generatedItemId(Profession.FISHERMAN, labelsByProfession, 1), 1);
                    case 1 -> costs.put(generatedItemId(Profession.FISHERMAN, labelsByProfession, 2), 1);
                    case 11 -> costs.put(generatedItemId(Profession.FISHERMAN, labelsByProfession, 11), 1);
                    case 12 -> costs.put(generatedItemId(Profession.FISHERMAN, labelsByProfession, 12), 1);
                    default -> { }
                }
                if (index == 16) costs.put(generatedItemId(Profession.ALCHEMIST, labelsByProfession, 3), 1);
            }
            case ALCHEMIST -> {
                if (index == 2 || index == 3) costs.put(generatedItemId(Profession.FARMER, labelsByProfession, 12), 1);
                if (index == 9) costs.put(generatedItemId(Profession.FISHERMAN, labelsByProfession, 4), 1);
                if (index == 16) costs.put(generatedItemId(Profession.SCHOLAR, labelsByProfession, 14), 1);
            }
            case MASON -> {
                if (index == 11) costs.put(generatedItemId(Profession.FISHERMAN, labelsByProfession, 16), 1);
                if (index == 16) costs.put(generatedItemId(Profession.BLACKSMITH, labelsByProfession, 15), 2);
            }
        }
        return costs;
    }

    private static String farmerDependency(int index, Map<Profession, JsonArray> labelsByProfession) {
        return switch (index) {
            case 2 -> generatedItemId(Profession.FARMER, labelsByProfession, 1);
            case 3 -> generatedItemId(Profession.FARMER, labelsByProfession, 3);
            case 5 -> generatedItemId(Profession.FARMER, labelsByProfession, 5);
            case 7 -> generatedItemId(Profession.FARMER, labelsByProfession, 14);
            case 10 -> generatedItemId(Profession.FARMER, labelsByProfession, 2);
            case 13 -> generatedItemId(Profession.FARMER, labelsByProfession, 1);
            case 15 -> generatedItemId(Profession.FARMER, labelsByProfession, 3);
            default -> throw new IllegalStateException("No farmer dependency mapping for cook recipe #" + (index + 1));
        };
    }

    private static String generatedItemId(Profession profession, Map<Profession, JsonArray> labelsByProfession, int zeroBasedIndex) {
        JsonArray labels = labelsByProfession.get(profession);
        if (labels == null || zeroBasedIndex < 0 || zeroBasedIndex >= labels.size()) {
            int available = labels == null ? 0 : labels.size();
            throw new IllegalStateException("Profession " + profession + " recipe dependency references missing recipe #" + (zeroBasedIndex + 1) + " (available: " + available + ")");
        }
        return "pixelrpg:" + profession.name().toLowerCase(Locale.ROOT) + ":" + String.format(Locale.ROOT, "%02d_%s", zeroBasedIndex + 1, slug(labels.get(zeroBasedIndex).getAsString()));
    }

    private static Map<Material, Integer> generatedMaterialCosts(Profession profession, int index) {
        EnumMap<Material, Integer> costs = new EnumMap<>(Material.class);
        switch (profession) {
            case BLACKSMITH -> {
                Material raw = index < 8 ? Material.IRON_INGOT : index < 10 ? Material.GOLD_INGOT : index < 15 ? Material.DIAMOND : Material.NETHERITE_INGOT;
                costs.put(raw, index < 4 ? 2 : index < 10 ? 3 : 4); costs.put(Material.COAL, Math.min(4, 1 + index / 6));
                if (index >= 10 && index < 15) costs.put(Material.IRON_INGOT, 2); if (index >= 15) costs.put(Material.DIAMOND, 2);
            }
            case SCHOLAR -> {
                costs.put(Material.PAPER, 2 + Math.min(6, index / 4)); if (index % 3 == 0) costs.put(Material.INK_SAC, 1);
                if (index >= 4) costs.put(Material.LAPIS_LAZULI, 2 + Math.min(5, index / 4)); if (index >= 14) costs.put(Material.AMETHYST_SHARD, 2);
            }
            case FARMER -> {
                Material raw = switch (index % 6) { case 0 -> Material.WHEAT; case 1 -> Material.CARROT; case 2 -> Material.POTATO; case 3 -> Material.BEETROOT; case 4 -> Material.PUMPKIN; default -> Material.MELON_SLICE; };
                costs.put(raw, 3 + Math.min(6, index / 3)); if (index >= 10) costs.put(Material.BONE_MEAL, 2);
            }
            case COOK -> {
                Material raw = switch (index % 6) { case 0 -> Material.COD; case 1 -> Material.SALMON; case 2 -> Material.WHEAT; case 3 -> Material.CARROT; case 4 -> Material.BEEF; default -> Material.CHICKEN; };
                costs.put(raw, 2 + Math.min(5, index / 4)); if (index >= 5) costs.put(Material.SUGAR, 1);
            }
            case TAILOR -> { costs.put(index < 8 ? Material.LEATHER : Material.WHITE_WOOL, 3 + Math.min(6, index / 3)); costs.put(Material.STRING, 1 + Math.min(4, index / 5)); }
            case ALCHEMIST -> {
                costs.put(Material.GLASS_BOTTLE, 1);
                Material reagent = switch (index % 8) { case 0 -> Material.HONEY_BOTTLE; case 1 -> Material.MELON_SLICE; case 2 -> Material.SUGAR; case 3 -> Material.GLISTERING_MELON_SLICE; case 4 -> Material.BLAZE_POWDER; case 5 -> Material.MAGMA_CREAM; case 6 -> Material.GOLDEN_CARROT; default -> Material.GHAST_TEAR; };
                costs.put(reagent, 1 + Math.min(2, index / 8));
            }
            case MASON -> {
                Material raw = switch (index % 6) { case 0 -> Material.STONE; case 1 -> Material.ANDESITE; case 2 -> Material.DIORITE; case 3 -> Material.GRANITE; case 4 -> Material.DEEPSLATE; default -> Material.SANDSTONE; };
                costs.put(raw, 4 + Math.min(8, index / 3));
            }
            case FISHERMAN -> {
                Material raw = switch (index % 5) { case 0, 1 -> Material.COD; case 2 -> Material.SALMON; case 3 -> Material.TROPICAL_FISH; default -> Material.PUFFERFISH; };
                costs.put(raw, 2 + Math.min(8, index / 3));
            }
            case WOODCUTTER -> {
                Material raw = switch (index % 6) { case 0 -> Material.OAK_LOG; case 1 -> Material.SPRUCE_LOG; case 2 -> Material.BIRCH_LOG; case 3 -> Material.JUNGLE_LOG; case 4 -> Material.ACACIA_LOG; default -> Material.DARK_OAK_LOG; };
                costs.put(raw, 3 + Math.min(8, index / 3)); if (index >= 16) costs.put(Material.IRON_NUGGET, 4);
            }
        }
        return costs;
    }

    private static String generatedPotionType(Profession profession, int index) {
        if (profession != Profession.ALCHEMIST) return "";
        return switch (index) {
            case 0, 2, 15, 16 -> "WATER"; case 1, 3, 12, 19 -> "HEALING"; case 4 -> "SPEED"; case 5, 13, 18 -> "STRENGTH";
            case 6 -> "FIRE_RESISTANCE"; case 7 -> "NIGHT_VISION"; case 8, 17 -> "REGENERATION"; case 9 -> "WATER_BREATHING";
            case 10 -> "LEAPING"; case 11 -> "INVISIBILITY"; case 14 -> "RESISTANCE"; default -> "WATER";
        };
    }

    private static String generatedEnchantment(Profession profession, int index) {
        if (profession != Profession.SCHOLAR) return "";
        return switch (index) { case 8 -> "efficiency"; case 9 -> "protection"; case 10 -> "sharpness"; case 11 -> "unbreaking"; case 12 -> "fortune"; case 13 -> "mending"; case 16 -> "unbreaking"; default -> ""; };
    }

    private static int generatedEnchantmentLevel(Profession profession, int index) {
        if (profession != Profession.SCHOLAR) return 0;
        return switch (index) { case 8, 9, 10, 11, 12, 16 -> 3; case 13 -> 1; default -> 0; };
    }

    private void validateProfessionEconomy() {
        Map<String, CraftRecipe> resultOwners = new HashMap<>();
        List<String> errors = new ArrayList<>();

        for (Profession profession : Profession.values()) {
            List<CraftRecipe> professionRecipes = getRecipes(profession);
            if (professionRecipes.isEmpty()) {
                errors.add("Profession " + profession + " has no recipes after loading " + RECIPE_DATA_PATH);
                continue;
            }
            Set<String> professionIds = new HashSet<>();
            for (CraftRecipe recipe : professionRecipes) {
                if (!professionIds.add(recipe.id())) errors.add("Duplicate recipe id in " + profession + ": " + recipe.id());
                if (recipe.requiredProfessionLevel() < Profession.MIN_LEVEL || recipe.requiredProfessionLevel() > Profession.MAX_LEVEL) {
                    errors.add("Invalid profession level for " + recipe.id() + ": " + recipe.requiredProfessionLevel());
                }
                if (recipe.resultItemId().isBlank()) errors.add("Missing resultItemId for " + recipe.id());
                else if (!recipe.resultItemId().startsWith("pixelrpg:")) errors.add("Invalid RPG result identity for " + recipe.id() + ": " + recipe.resultItemId());
                String resultId = canonicalItemId(recipe.resultItemId());
                CraftRecipe previous = resultOwners.putIfAbsent(resultId, recipe);
                if (previous != null) errors.add("Duplicate result item id " + resultId + " used by " + previous.id() + " and " + recipe.id());
            }
        }

        for (CraftRecipe recipe : recipes.values()) {
            for (Map.Entry<String, Integer> entry : recipe.itemCosts().entrySet()) {
                String dependencyId = canonicalItemId(entry.getKey());
                if (!dependencyId.startsWith("pixelrpg:")) continue;
                CraftRecipe dependency = resultOwners.get(dependencyId);
                if (dependency == null) {
                    errors.add("Broken profession dependency in " + recipe.id() + ": " + dependencyId);
                    continue;
                }
                if (dependency.id().equals(recipe.id())) errors.add("Self-referencing profession dependency in " + recipe.id());
                if (dependency.requiredProfessionLevel() > recipe.requiredProfessionLevel()) {
                    errors.add("Dependency level inversion in " + recipe.id() + ": " + dependency.id() + " requires level " + dependency.requiredProfessionLevel() + " but recipe unlocks at " + recipe.requiredProfessionLevel());
                }
            }
        }

        detectDependencyCycles(resultOwners, errors);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Profession recipe validation failed:\n - " + String.join("\n - ", errors));
        }
    }

    private static void detectDependencyCycles(Map<String, CraftRecipe> resultOwners, List<String> errors) {
        Map<String, Integer> state = new HashMap<>();
        for (CraftRecipe recipe : resultOwners.values()) detectDependencyCycle(recipe, resultOwners, state, new HashSet<>(), errors);
    }

    private static void detectDependencyCycle(CraftRecipe recipe, Map<String, CraftRecipe> resultOwners, Map<String, Integer> state,
                                              Set<String> path, List<String> errors) {
        String key = canonicalRecipeId(recipe.id());
        if (state.getOrDefault(key, 0) == 2) return;
        if (state.getOrDefault(key, 0) == 1) {
            errors.add("Circular profession dependency detected at " + recipe.id() + ": " + String.join(" -> ", path));
            return;
        }
        state.put(key, 1);
        path.add(recipe.id());
        for (String itemCost : recipe.itemCosts().keySet()) {
            CraftRecipe dependency = resultOwners.get(canonicalItemId(itemCost));
            if (dependency != null) detectDependencyCycle(dependency, resultOwners, state, path, errors);
        }
        path.remove(recipe.id());
        state.put(key, 2);
    }

    private static Profession parseProfession(JsonObject json, String id) {
        String raw = required(json, "profession").trim().toUpperCase(Locale.ROOT);
        if (raw.equals("PROVISIONER")) return Profession.COOK;
        try { return Profession.valueOf(raw); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("Invalid profession for " + id + ": " + raw, exception); }
    }

    private static Material resolveResultMaterial(String raw) {
        String value = raw.trim().toUpperCase(Locale.ROOT); if (value.equals("CHAIN")) value = "IRON_CHAIN"; return Material.matchMaterial(value);
    }

    private static Map<Material, Integer> parseCosts(JsonObject json, String id) {
        JsonObject costs = json.has("costs") ? json.getAsJsonObject("costs") : null; if (costs == null || costs.isEmpty()) return Map.of();
        Map<Material, Integer> result = new EnumMap<>(Material.class);
        for (var entry : costs.entrySet()) {
            Material material = Material.matchMaterial(entry.getKey()); int amount = entry.getValue().getAsInt();
            if (material == null || material.isAir() || amount <= 0) throw new IllegalStateException("Invalid cost for " + id + ": " + entry.getKey());
            result.merge(material, amount, Integer::sum);
        }
        return result;
    }

    private static Map<String, Integer> parseItemCosts(JsonObject json, String id) {
        JsonObject costs = json.has("itemCosts") ? json.getAsJsonObject("itemCosts") : null; if (costs == null || costs.isEmpty()) return Map.of();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var entry : costs.entrySet()) {
            String itemId = canonicalItemId(entry.getKey()); int amount = entry.getValue().getAsInt();
            if (itemId.isBlank() || amount <= 0) throw new IllegalStateException("Invalid item cost for " + id + ": " + entry.getKey());
            result.merge(itemId, amount, Integer::sum);
        }
        return result;
    }

    private static String canonicalRecipeId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT); if (value.startsWith("pixelrpg:")) value = value.substring(9); return value.replace('/', ':');
    }

    private static String canonicalItemId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT); if (!value.startsWith("pixelrpg:")) value = "pixelrpg:" + value;
        String body = value.substring(9).replace('/', ':'); while (body.contains("::")) body = body.replace("::", ":"); return "pixelrpg:" + body;
    }

    private static String slug(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        return value.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
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
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' '); return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
