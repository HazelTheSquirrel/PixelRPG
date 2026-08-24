package de.pixelrpg.rpg.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/** Data-driven equipment-set bonus resolver. Empty data means no active sets. */
public final class EquipmentSetService {
    private final Map<String, JsonArray> sets = new HashMap<>();

    public EquipmentSetService(Plugin plugin) {
        JsonObject root = new JsonDataManager(plugin).load("equipment-sets.json");
        JsonArray definitions = root.has("sets") ? root.getAsJsonArray("sets") : new JsonArray();
        for (var element : definitions) {
            JsonObject definition = element.getAsJsonObject();
            if (!definition.has("id") || !definition.has("bonuses")) continue;
            sets.put(definition.get("id").getAsString().trim().toLowerCase(), definition.getAsJsonArray("bonuses"));
        }
    }

    public Map<String, Double> bonuses(Player player, int playerLevel) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : equipped(player)) {
            if (!usable(item, playerLevel) || !item.hasItemMeta()) continue;
            String setId = item.getItemMeta().getPersistentDataContainer()
                    .get(RPGKeys.Item.setId(), PersistentDataType.STRING);
            if (setId != null && !setId.isBlank()) counts.merge(setId.toLowerCase(), 1, Integer::sum);
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
                        result.merge(stat.getKey().toUpperCase(), Math.max(0.0D, stat.getValue().getAsDouble()), Double::sum);
                    }
                }
            }
        }
        return result;
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
}
