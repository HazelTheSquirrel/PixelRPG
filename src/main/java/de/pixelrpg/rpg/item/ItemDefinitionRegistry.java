package de.pixelrpg.rpg.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Loads and validates the complete concrete PixelRPG item catalogue from JSON. */
public final class ItemDefinitionRegistry {
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();

    public ItemDefinitionRegistry(Plugin plugin) {
        load(plugin);
    }

    public Optional<ItemDefinition> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(definitions.get(normalize(id)));
    }

    public Collection<ItemDefinition> all() {
        return java.util.List.copyOf(definitions.values());
    }

    private void load(Plugin plugin) {
        JsonObject root = new JsonDataManager(plugin).load("item-definitions.json");
        JsonArray items = root.getAsJsonArray("items");
        if (items == null) throw new IllegalStateException("item-definitions.json requires an 'items' array");

        for (var element : items) {
            ItemDefinition definition = parse(element.getAsJsonObject());
            if (definitions.put(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate PixelRPG item definition: " + definition.id());
            }
        }
    }

    private ItemDefinition parse(JsonObject json) {
        String id = required(json, "id");
        String name = required(json, "name");
        Material material = Material.matchMaterial(required(json, "material"));
        if (material == null) throw new IllegalStateException("Unknown item material for " + id);
        ItemRarity rarity = enumValue(ItemRarity.class, json, "rarity", id);
        ItemCategory category = enumValue(ItemCategory.class, json, "category", id);
        ItemCategory resolved = GearCategoryRegistry.resolve(material).orElse(null);
        if (resolved != category) throw new IllegalStateException("Category " + category + " does not match material " + material + " for " + id);

        int itemLevel = integer(json, "itemLevel", 1);
        int requiredLevel = integer(json, "requiredLevel", itemLevel);
        String ability = json.has("weaponAbility") ? json.get("weaponAbility").getAsString() : "";
        long cooldown = json.has("weaponAbilityCooldownMillis") ? json.get("weaponAbilityCooldownMillis").getAsLong() : 0L;
        boolean soulbound = booleanValue(json, "soulbound", false);
        boolean unique = booleanValue(json, "unique", rarity == ItemRarity.UNIQUE);
        boolean adminOnly = booleanValue(json, "adminOnly", unique);
        String resourcepackId = json.has("resourcepackId") ? json.get("resourcepackId").getAsString() : id;
        double modifier = json.has("gearscoreModifier") ? json.get("gearscoreModifier").getAsDouble() : 1.0D;
        String equipmentSlot = json.has("equipmentSlot") ? json.get("equipmentSlot").getAsString() : "";
        String setId = json.has("setId") ? json.get("setId").getAsString() : "";

        return new ItemDefinition(id, name, material, rarity, category, itemLevel, requiredLevel,
                ability, cooldown, soulbound, unique, adminOnly, resourcepackId, modifier, equipmentSlot, setId);
    }

    private static String required(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) throw new IllegalStateException("Missing '" + key + "' in item definition");
        return json.get(key).getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static boolean booleanValue(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, JsonObject json, String key, String id) {
        try {
            return Enum.valueOf(type, required(json, key).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid " + key + " for " + id, exception);
        }
    }

    private static String normalize(String id) {
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }
}
