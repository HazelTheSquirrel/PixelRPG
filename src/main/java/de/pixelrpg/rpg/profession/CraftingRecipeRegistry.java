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

/** Registry for PixelRPG-only profession recipes with one canonical ID format. */
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
        return Optional.ofNullable(recipes.get(canonicalRecipeId(id)));
    }

    /** Resolves the player-facing recipe label for a crafted PixelRPG result item. */
    public Optional<String> findDisplayNameByResultItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) return Optional.empty();
        String canonical = canonicalItemId(itemId);
        List<String> labels = recipes.values().stream()
                .filter(recipe -> !recipe.resultItemId().isBlank())
                .filter(recipe -> canonicalItemId(recipe.resultItemId()).equals(canonical))
                .map(CraftRecipe::displayName)
                .distinct()
                .toList();
        return labels.size() == 1 ? Optional.of(labels.getFirst()) : Optional.empty();
    }

    public boolean hasResultItemId(String itemId) {
        String canonical = canonicalItemId(itemId);
        return recipes.values().stream().anyMatch(recipe -> !recipe.resultItemId().isBlank()
                && canonicalItemId(recipe.resultItemId()).equals(canonical));
    }

    private void loadDefinitions(Plugin plugin) {
        JsonObject root = new JsonDataManager(plugin).load(RECIPE_DATA_PATH);
        if (root.has("generatedProfessionRecipes")) {
            loadGeneratedDefinitions(root.getAsJsonObject("generatedProfessionRecipes"));
        }
        if (root.has("recipes")) {
            loadExplicitDefinitions(root.getAsJsonArray("recipes"));
        }
        if (recipes.isEmpty()) {
            throw new IllegalStateException("crafting-recipes.json requires 'recipes' or 'generatedProfessionRecipes'");
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
            addRecipe(
                    id,
                    json.has("label") ? json.get("label").getAsString() : pretty(result),
                    profession,
                    result,
                    json.has("resultAmount") ? json.get("resultAmount").getAsInt() : 1,
                    enumValue(ItemRarity.class, json, "rarity", id),
                    parseCosts(json, id),
                    parseItemCosts(json, id),
                    json.has("requiredProfessionLevel") ? json.get("requiredProfessionLevel").getAsInt() : 1,
                    json.has("unlockPrice") ? json.get("unlockPrice").getAsLong() : 0L,
                    json.has("requiredQuestId") ? json.get("requiredQuestId").getAsString() : "",
                    json.has("unlockedByDefault") && json.get("unlockedByDefault").getAsBoolean(),
                    json.has("resultItemId") ? canonicalItemId(json.get("resultItemId").getAsString()) : "",
                    json.has("potionType") ? json.get("potionType").getAsString() : "",
                    json.has("enchantment") ? json.get("enchantment").getAsString() : "",
                    json.has("enchantmentLevel") ? json.get("enchantmentLevel").getAsInt() : 0
            );
        }
    }

    private void loadGeneratedDefinitions(JsonObject professions) {
        for (Profession profession : Profession.values()) {
            String key = profession.name();
            if (!professions.has(key)) throw new IllegalStateException("Missing generated recipe group for " + key);
            JsonObject group = professions.getAsJsonObject(key);
            JsonArray labels = group.getAsJsonArray("labels");
            JsonArray results = group.getAsJsonArray("results");
            if (labels == null || results == null || labels.size() != 20 || results.size() != 20) {
                throw new IllegalStateException("Generated recipe group " + key + " must contain exactly 20 labels and 20 results");
            }

            for (int index = 0; index < 20; index++) {
                int level = index == 0 ? 1 : index * 5;
                String label = labels.get(index).getAsString();
                Material result = resolveResultMaterial(results.get(index).getAsString());
                if (result == null || result.isAir()) {
                    throw new IllegalStateException("Unknown generated result for " + key + " #" + (index + 1));
                }

                String recipeId = key.toLowerCase(Locale.ROOT) + ":" + String.format(Locale.ROOT, "%02d_%s", index + 1, slug(label));
                String resultItemId = "pixelrpg:" + recipeId.replace(':', '/');

                Map<Material, Integer> costs = generatedMaterialCosts(profession, index, result);
                Map<String, Integer> itemCosts = index == 0
                        ? Map.of()
                        : Map.of(previousGeneratedItemId(profession, labels, index), 1);

                String potionType = "";
                String enchantment = "";
                int enchantmentLevel = 0;
                if (profession == Profession.ALCHEMIST) {
                    potionType = (index == 3 || index == 8 || index == 12 || index == 17) ? "HEALING" : "WATER";
                } else if (profession == Profession.SCHOLAR) {
                    switch (index) {
                        case 7 -> { enchantment = "efficiency"; enchantmentLevel = 3; }
                        case 8 -> { enchantment = "protection"; enchantmentLevel = 3; }
                        case 9 -> { enchantment = "sharpness"; enchantmentLevel = 3; }
                        case 10 -> { enchantment = "unbreaking"; enchantmentLevel = 2; }
                        case 11 -> { enchantment = "fortune"; enchantmentLevel = 2; }
                        case 12 -> { enchantment = "mending"; enchantmentLevel = 1; }
                        default -> { }
                    }
                }

                addRecipe(
                        recipeId,
                        label,
                        profession,
                        result,
                        1,
                        generatedRarity(index),
                        costs,
                        itemCosts,
                        level,
                        100L + (index * 250L),
                        "",
                        false,
                        resultItemId,
                        potionType,
                        enchantment,
                        enchantmentLevel
                );
            }
        }
    }

    private void addRecipe(
            String id,
            String label,
            Profession profession,
            Material result,
            int resultAmount,
            ItemRarity rarity,
            Map<Material, Integer> costs,
            Map<String, Integer> itemCosts,
            int level,
            long unlockPrice,
            String requiredQuestId,
            boolean unlockedByDefault,
            String resultItemId,
            String potionType,
            String enchantment,
            int enchantmentLevel
    ) {
        recipes.put(id, new CraftRecipe(
                profession,
                id,
                label,
                result,
                resultAmount,
                rarity,
                costs,
                itemCosts,
                level,
                unlockPrice,
                requiredQuestId,
                unlockedByDefault,
                false,
                resultItemId,
                potionType,
                enchantment,
                enchantmentLevel
        ));
    }

    private static ItemRarity generatedRarity(int index) {
        return switch (index) {
            case 0, 1, 2, 3 -> ItemRarity.COMMON;
            case 4, 5, 6, 7 -> ItemRarity.UNCOMMON;
            case 8, 9, 10, 11 -> ItemRarity.RARE;
            case 12, 13, 14, 15 -> ItemRarity.EPIC;
            default -> ItemRarity.LEGENDARY;
        };
    }

    private static String previousGeneratedItemId(Profession profession, JsonArray labels, int index) {
        String previousLabel = labels.get(index - 1).getAsString();
        String recipeId = profession.name().toLowerCase(Locale.ROOT) + ":" +
                String.format(Locale.ROOT, "%02d_%s", index, slug(previousLabel));
        return "pixelrpg:" + recipeId.replace(':', '/');
    }

    private static Map<Material, Integer> generatedMaterialCosts(Profession profession, int index, Material result) {
        EnumMap<Material, Integer> costs = new EnumMap<>(Material.class);
        switch (profession) {
            case BLACKSMITH -> {
                Material raw = index < 9 ? Material.IRON_INGOT : index < 12 ? Material.GOLD_INGOT : index < 17 ? Material.DIAMOND : Material.NETHERITE_INGOT;
                costs.put(raw, index < 4 ? 2 : index < 12 ? 3 : 4);
                costs.put(Material.COAL, Math.min(4, 1 + index / 6));
            }
            case SCHOLAR -> {
                costs.put(Material.PAPER, 2 + Math.min(6, index / 4));
                if (index % 3 == 0) costs.put(Material.INK_SAC, 1);
                if (index >= 4) costs.put(Material.LAPIS_LAZULI, 2 + Math.min(5, index / 4));
                if (index >= 14) costs.put(Material.AMETHYST_SHARD, 2);
            }
            case FARMER -> {
                Material raw = switch (index % 6) {
                    case 0 -> Material.WHEAT;
                    case 1 -> Material.CARROT;
                    case 2 -> Material.POTATO;
                    case 3 -> Material.BEETROOT;
                    case 4 -> Material.PUMPKIN;
                    default -> Material.MELON_SLICE;
                };
                costs.put(raw, 3 + Math.min(6, index / 3));
                if (index >= 10) costs.put(Material.BONE_MEAL, 2);
            }
            case COOK -> {
                Material raw = switch (index % 6) {
                    case 0 -> Material.COD;
                    case 1 -> Material.SALMON;
                    case 2 -> Material.WHEAT;
                    case 3 -> Material.CARROT;
                    case 4 -> Material.BEEF;
                    default -> Material.CHICKEN;
                };
                costs.put(raw, 2 + Math.min(5, index / 4));
                if (index >= 5) costs.put(Material.SUGAR, 1);
            }
            case TAILOR -> {
                Material raw = index < 8 ? Material.LEATHER : Material.WHITE_WOOL;
                costs.put(raw, 3 + Math.min(6, index / 3));
                costs.put(Material.STRING, 1 + Math.min(4, index / 5));
            }
            case ALCHEMIST -> {
                costs.put(Material.GLASS_BOTTLE, index < 16 ? 1 : 2);
                Material reagent = switch (index % 8) {
                    case 0 -> Material.HONEY_BOTTLE;
                    case 1 -> Material.MELON_SLICE;
                    case 2 -> Material.SUGAR;
                    case 3 -> Material.GLISTERING_MELON_SLICE;
                    case 4 -> Material.BLAZE_POWDER;
                    case 5 -> Material.MAGMA_CREAM;
                    case 6 -> Material.GOLDEN_CARROT;
                    default -> Material.GHAST_TEAR;
                };
                costs.put(reagent, 1 + Math.min(2, index / 8));
            }
            case MASON -> {
                Material raw = switch (index % 6) {
                    case 0 -> Material.STONE;
                    case 1 -> Material.ANDESITE;
                    case 2 -> Material.DIORITE;
                    case 3 -> Material.GRANITE;
                    case 4 -> Material.DEEPSLATE;
                    default -> Material.SANDSTONE;
                };
                costs.put(raw, 4 + Math.min(8, index / 3));
            }
            case FISHERMAN -> {
                Material raw = switch (index % 5) {
                    case 0, 1 -> Material.COD;
                    case 2 -> Material.SALMON;
                    case 3 -> Material.TROPICAL_FISH;
                    default -> Material.PUFFERFISH;
                };
                costs.put(raw, 2 + Math.min(8, index / 3));
                if (index >= 15) costs.put(Material.NAUTILUS_SHELL, 1);
            }
            case WOODCUTTER -> {
                Material raw = switch (index % 6) {
                    case 0 -> Material.OAK_LOG;
                    case 1 -> Material.SPRUCE_LOG;
                    case 2 -> Material.BIRCH_LOG;
                    case 3 -> Material.JUNGLE_LOG;
                    case 4 -> Material.ACACIA_LOG;
                    default -> Material.DARK_OAK_LOG;
                };
                costs.put(raw, 3 + Math.min(8, index / 3));
                if (index >= 12) costs.put(Material.IRON_NUGGET, 2);
            }
        }
        return costs;
    }

    private static Profession parseProfession(JsonObject json, String id) {
        String raw = required(json, "profession").trim().toUpperCase(Locale.ROOT);
        if (raw.equals("PROVISIONER")) return Profession.COOK;
        try {
            return Profession.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid profession for " + id + ": " + raw, exception);
        }
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

    private static Map<String, Integer> parseItemCosts(JsonObject json, String id) {
        JsonObject costs = json.has("itemCosts") ? json.getAsJsonObject("itemCosts") : null;
        if (costs == null || costs.isEmpty()) return Map.of();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var entry : costs.entrySet()) {
            String itemId = canonicalItemId(entry.getKey());
            int amount = entry.getValue().getAsInt();
            if (itemId.isBlank() || amount <= 0) throw new IllegalStateException("Invalid item cost for " + id + ": " + entry.getKey());
            result.merge(itemId, amount, Integer::sum);
        }
        return result;
    }

    private static String canonicalRecipeId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("pixelrpg:")) value = value.substring("pixelrpg:".length());
        return value.replace(':', '/');
    }

    private static String canonicalItemId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith("pixelrpg:")) value = "pixelrpg:" + value;
        String body = value.substring("pixelrpg:".length()).replace(':', '/');
        while (body.contains("//")) body = body.replace("//", "/");
        return "pixelrpg:" + body;
    }

    private static String slug(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        value = value.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        return value.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
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
