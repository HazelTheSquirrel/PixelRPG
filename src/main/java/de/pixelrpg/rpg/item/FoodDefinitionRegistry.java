package de.pixelrpg.rpg.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Loads and validates the complete concrete PixelRPG food catalogue. */
public final class FoodDefinitionRegistry {
    private final Map<String, FoodDefinition> definitions = new LinkedHashMap<>();

    public FoodDefinitionRegistry(Plugin plugin) {
        load(plugin);
    }

    public Optional<FoodDefinition> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(definitions.get(normalize(id)));
    }

    private void load(Plugin plugin) {
        JsonObject root = new JsonDataManager(plugin).load("food-definitions.json");
        JsonArray items = root.getAsJsonArray("items");
        if (items == null) throw new IllegalStateException("food-definitions.json requires an 'items' array");

        for (var element : items) {
            FoodDefinition definition = parse(element.getAsJsonObject());
            if (definitions.put(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate PixelRPG food definition: " + definition.id());
            }
        }
    }

    private FoodDefinition parse(JsonObject json) {
        String id = required(json, "id");
        int nutrition = integer(json, "nutrition", 1);
        float saturation = (float) number(json, "saturation", 0.5D);
        boolean canAlwaysEat = booleanValue(json, "canAlwaysEat", false);
        String effectType = json.has("effectType") ? json.get("effectType").getAsString() : "";
        int effectDurationSeconds = integer(json, "effectDurationSeconds", 0);
        int effectAmplifier = integer(json, "effectAmplifier", 0);
        return new FoodDefinition(id, nutrition, saturation, canAlwaysEat, effectType, effectDurationSeconds, effectAmplifier);
    }

    private static String required(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) throw new IllegalStateException("Missing '" + key + "' in food definition");
        return json.get(key).getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private static boolean booleanValue(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static String normalize(String id) {
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }
}
