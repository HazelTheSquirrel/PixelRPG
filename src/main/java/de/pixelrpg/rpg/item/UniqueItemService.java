package de.pixelrpg.rpg.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import de.pixelrpg.rpg.core.RPGKeys;

import java.util.HashSet;
import java.util.Set;

public final class UniqueItemService {
    private final Plugin plugin;
    private final Set<String> claimedDefinitions = new HashSet<>();

    public UniqueItemService(Plugin plugin) {
        this.plugin = plugin;
        load();
    }

    public synchronized boolean claim(ItemDefinition definition) {
        if (!definition.unique()) return false;
        if (!claimedDefinitions.add(definition.id())) return false;
        save();
        return true;
    }

    public synchronized void release(ItemDefinition definition) {
        if (!definition.unique()) return;
        if (claimedDefinitions.remove(definition.id())) save();
    }

    public synchronized boolean isClaimed(ItemDefinition definition) {
        return claimedDefinitions.contains(definition.id());
    }

    public boolean isUnique(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.itemId(), PersistentDataType.STRING);
        return id != null && claimedDefinitions.contains(id);
    }

    private void load() {
        JsonObject root = new JsonDataManager(plugin).load("unique-items.json");
        JsonArray claimed = root.getAsJsonArray("claimed");
        if (claimed == null) return;
        for (var element : claimed) claimedDefinitions.add(element.getAsString());
    }

    private void save() {
        JsonObject root = new JsonObject();
        JsonArray claimed = new JsonArray();
        claimedDefinitions.stream().sorted().forEach(claimed::add);
        root.add("claimed", claimed);
        new JsonDataManager(plugin).save("unique-items.json", root);
    }
}
