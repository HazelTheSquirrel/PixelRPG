package de.pixelrpg.rpg.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.RPGKeys;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Data-driven equipment-set bonuses and visual armor-trim mappings. */
public final class EquipmentSetService {
    private final Map<String, JsonArray> sets = new HashMap<>();
    private final Map<String, TrimDefinition> trimMappings = new HashMap<>();

    public EquipmentSetService(Plugin plugin) {
        JsonObject root = new JsonDataManager(plugin).load("equipment-sets.json");
        JsonArray definitions = root.has("sets") ? root.getAsJsonArray("sets") : new JsonArray();
        for (var element : definitions) {
            JsonObject definition = element.getAsJsonObject();
            if (!definition.has("id") || !definition.has("bonuses")) continue;
            sets.put(definition.get("id").getAsString().trim().toLowerCase(Locale.ROOT), definition.getAsJsonArray("bonuses"));
        }

        if (root.has("trim_mappings") && root.get("trim_mappings").isJsonObject()) {
            JsonObject mappings = root.getAsJsonObject("trim_mappings");
            for (var entry : mappings.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject definition = entry.getValue().getAsJsonObject();
                String pattern = stringValue(definition, "pattern");
                String material = stringValue(definition, "material");
                if (pattern != null && material != null) {
                    trimMappings.put(entry.getKey().trim().toUpperCase(Locale.ROOT), new TrimDefinition(pattern, material));
                }
            }
        }
    }

    public Map<String, Double> bonuses(Player player, int playerLevel) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : equipped(player)) {
            if (!usable(item, playerLevel) || !item.hasItemMeta()) continue;
            String setId = item.getItemMeta().getPersistentDataContainer()
                    .get(RPGKeys.Item.setId(), PersistentDataType.STRING);
            if (setId != null && !setId.isBlank()) counts.merge(setId.toLowerCase(Locale.ROOT), 1, (current, added) -> current + added);
        }

        Map<String, Double> result = new HashMap<>();
        for (var entry : counts.entrySet()) {
            JsonArray bonuses = sets.get(entry.getKey());
            if (bonuses == null) continue;
            for (var element : bonuses) {
                JsonObject bonus = element.getAsJsonObject();
                int pieces = bonus.has("pieces") ? bonus.get("pieces").getAsInt() : Integer.MAX_VALUE;
                if (entry.getValue() < pieces || !bonus.has("stats")) continue;
                JsonObject stats = bonus.getAsJsonObject("stats");
                for (var stat : stats.entrySet()) {
                    if (stat.getValue().isJsonPrimitive() && stat.getValue().getAsJsonPrimitive().isNumber()) {
                        result.merge(stat.getKey().toUpperCase(Locale.ROOT), Math.max(0.0D, stat.getValue().getAsDouble()), (current, added) -> current + added);
                    }
                }
            }
        }
        return result;
    }

    /** Applies the configured stat-to-trim mapping to all active set pieces. */
    public void applyArmorTrims(Player player, int playerLevel) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : equipped(player)) {
            if (!usable(item, playerLevel) || !item.hasItemMeta()) continue;
            String setId = item.getItemMeta().getPersistentDataContainer()
                    .get(RPGKeys.Item.setId(), PersistentDataType.STRING);
            if (setId != null && !setId.isBlank()) counts.merge(setId.toLowerCase(Locale.ROOT), 1, Integer::sum);
        }

        for (ItemStack item : equipped(player)) {
            if (item == null || item.isEmpty() || !item.hasItemMeta()) continue;
            String setId = item.getItemMeta().getPersistentDataContainer()
                    .get(RPGKeys.Item.setId(), PersistentDataType.STRING);
            if (setId == null || setId.isBlank()) continue;

            TrimDefinition activeTrim = activeTrimForSet(setId, counts.getOrDefault(setId.toLowerCase(Locale.ROOT), 0));
            applyTrim(item, activeTrim);
        }
    }

    private TrimDefinition activeTrimForSet(String setId, int pieceCount) {
        JsonArray bonuses = sets.get(setId.trim().toLowerCase(Locale.ROOT));
        if (bonuses == null) return null;

        TrimDefinition selected = null;
        int selectedPieces = -1;
        for (var element : bonuses) {
            JsonObject bonus = element.getAsJsonObject();
            int pieces = bonus.has("pieces") ? bonus.get("pieces").getAsInt() : Integer.MAX_VALUE;
            if (pieceCount < pieces || pieces < selectedPieces) continue;

            if (bonus.has("trim") && bonus.get("trim").isJsonObject()) {
                JsonObject trim = bonus.getAsJsonObject("trim");
                String pattern = stringValue(trim, "pattern");
                String material = stringValue(trim, "material");
                if (pattern != null && material != null) {
                    selected = new TrimDefinition(pattern, material);
                    selectedPieces = pieces;
                    continue;
                }
            }

            if (bonus.has("stats") && bonus.get("stats").isJsonObject()) {
                for (String stat : bonus.getAsJsonObject("stats").keySet()) {
                    TrimDefinition mapped = trimMappings.get(stat.trim().toUpperCase(Locale.ROOT));
                    if (mapped != null) {
                        selected = mapped;
                        selectedPieces = pieces;
                        break;
                    }
                }
            }
        }
        return selected;
    }

    private void applyTrim(ItemStack item, TrimDefinition definition) {
        if (!(item.getItemMeta() instanceof ArmorMeta armorMeta)) return;
        ArmorTrim trim = definition == null ? null : resolveTrim(definition);
        armorMeta.setTrim(trim);
        item.setItemMeta(armorMeta);
    }

    private ArmorTrim resolveTrim(TrimDefinition definition) {
        Registry<TrimMaterial> materials = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL);
        Registry<TrimPattern> patterns = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN);
        TrimMaterial material = materials.get(parseMinecraftKey(definition.material()));
        TrimPattern pattern = patterns.get(parseMinecraftKey(definition.pattern()));
        if (material == null || pattern == null) return null;
        return new ArmorTrim(material, pattern);
    }

    private NamespacedKey parseMinecraftKey(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? NamespacedKey.fromString(normalized) : NamespacedKey.minecraft(normalized);
    }

    private String stringValue(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) return null;
        String value = object.get(key).getAsString().trim();
        return value.isBlank() ? null : value;
    }

    private boolean usable(ItemStack item, int playerLevel) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return false;
        Integer required = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        return required == null || playerLevel >= required;
    }

    private ItemStack[] equipped(Player player) {
        var inventory = player.getInventory();
        ItemStack[] armor = inventory.getArmorContents();
        ItemStack[] result = new ItemStack[armor.length + 2];
        System.arraycopy(armor, 0, result, 0, armor.length);
        result[armor.length] = inventory.getItemInMainHand();
        result[armor.length + 1] = inventory.getItemInOffHand();
        return result;
    }

    private record TrimDefinition(String pattern, String material) { }
}
