package de.pixelrpg.rpg.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ItemDefinitionRegistry {
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();

    public ItemDefinitionRegistry(Plugin plugin) {
        load(plugin, "data/item-definitions.json");
        load(plugin, "data/boss-reward-items.json");
        load(plugin, "data/food-definitions.json");
    }

    public Optional<ItemDefinition> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        String key = normalize(id);
        return Optional.ofNullable(definitions.get(key));
    }

    public Collection<ItemDefinition> all() { return List.copyOf(definitions.values()); }

    public Optional<ItemDefinition> findByMaterial(Material material, ItemRarity rarity, int level) {
        return definitions.values().stream().filter(d -> d.material() == material && d.rarity() == rarity && d.itemLevel() == level).findFirst();
    }

    private void load(Plugin plugin, String resource) {
        try (var stream = plugin.getResource(resource)) {
            if (stream == null) throw new IllegalStateException("Missing item data resource: " + resource);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("items") || !root.get("items").isJsonArray()) throw new IllegalStateException(resource + " requires an 'items' array");
            for (var element : root.getAsJsonArray("items")) {
                JsonObject json = element.getAsJsonObject();
                String id = required(json, "id");
                Material material = Material.matchMaterial(required(json, "material"));
                if (material == null) throw new IllegalStateException("Unknown material for " + id);
                ItemCategory category = enumValue(ItemCategory.class, required(json, "category"), id);
                if (category != ItemCategory.FOOD && GearCategoryRegistry.resolve(material).filter(category::equals).isEmpty()) {
                    throw new IllegalStateException("Category " + category + " does not match material " + material + " for " + id);
                }
                ItemDefinition definition = new ItemDefinition(
                        id, required(json, "name"), material,
                        enumValue(ItemRarity.class, required(json, "rarity"), id), category,
                        integer(json, "itemLevel", 1), integer(json, "requiredLevel", 1),
                        json.has("weaponAbility") ? json.get("weaponAbility").getAsString() : "",
                        json.has("weaponAbilityCooldownMillis") ? json.get("weaponAbilityCooldownMillis").getAsLong() : 0L,
                        bool(json, "soulbound", false), bool(json, "unique", false), bool(json, "adminOnly", false),
                        json.has("resourcepackId") ? json.get("resourcepackId").getAsString() : id,
                        json.has("gearscoreModifier") ? json.get("gearscoreModifier").getAsDouble() : 1.0D,
                        json.has("equipmentSlot") ? json.get("equipmentSlot").getAsString() : "",
                        json.has("setId") ? json.get("setId").getAsString() : "");
                if (definitions.put(definition.id(), definition) != null) throw new IllegalStateException("Duplicate PixelRPG item definition: " + definition.id());
            }
        } catch (RuntimeException | java.io.IOException exception) {
            throw new IllegalStateException("Failed to load " + resource, exception);
        }
    }

    private static String required(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull() || json.get(key).getAsString().isBlank()) throw new IllegalStateException("Missing '" + key + "' in item definition");
        return json.get(key).getAsString();
    }
    private static int integer(JsonObject json, String key, int fallback) { return json.has(key) ? json.get(key).getAsInt() : fallback; }
    private static boolean bool(JsonObject json, String key, boolean fallback) { return json.has(key) ? json.get(key).getAsBoolean() : fallback; }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String id) {
        try { return Enum.valueOf(type, value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("Invalid value '" + value + "' for " + id, exception); }
    }
    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }
}
