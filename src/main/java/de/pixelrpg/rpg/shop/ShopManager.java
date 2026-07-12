// src/main/java/de/pixelrpg/rpg/shop/ShopManager.java
package de.pixelrpg.rpg.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ShopManager {

    private final Plugin plugin;
    private final File file;
    private final Map<String, List<ShopEntry>> shopsByNpcId = new ConcurrentHashMap<>();

    public ShopManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shops.yml");
    }

    public void load() {
        shopsByNpcId.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("shops");
        if (root == null) {
            return;
        }

        for (String npcId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(npcId);
            if (section == null) {
                continue;
            }

            List<ShopEntry> entries = new ArrayList<>();
            ConfigurationSection entriesSection = section.getConfigurationSection("entries");
            if (entriesSection != null) {
                for (String key : entriesSection.getKeys(false)) {
                    ItemStack item = entriesSection.getItemStack(key + ".item");
                    double price = entriesSection.getDouble(key + ".price", 0.0);
                    if (item != null) {
                        entries.add(new ShopEntry(item, price));
                    }
                }
            }
            shopsByNpcId.put(npcId, entries);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, List<ShopEntry>> mapEntry : shopsByNpcId.entrySet()) {
            String basePath = "shops." + mapEntry.getKey() + ".entries";
            int index = 0;
            for (ShopEntry entry : mapEntry.getValue()) {
                yaml.set(basePath + "." + index + ".item", entry.item());
                yaml.set(basePath + "." + index + ".price", entry.price());
                index++;
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save shops.yml", e);
        }
    }

    public List<ShopEntry> getEntries(String npcId) {
        return shopsByNpcId.getOrDefault(npcId, List.of());
    }

    public void addEntry(String npcId, ShopEntry entry) {
        shopsByNpcId.computeIfAbsent(npcId, k -> new ArrayList<>()).add(entry);
        save();
    }

    public boolean removeEntry(String npcId, int index) {
        List<ShopEntry> entries = shopsByNpcId.get(npcId);
        if (entries == null || index < 0 || index >= entries.size()) {
            return false;
        }
        entries.remove(index);
        save();
        return true;
    }

    public void replaceEntries(String npcId, List<ShopEntry> entries) {
        shopsByNpcId.put(npcId, entries);
        save();
    }
}