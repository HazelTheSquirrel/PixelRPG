package de.pixelrpg.rpg.shop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
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

    // Lädt Shop-Items primär aus dem neuen Base64-NBT-Format ("item-data").
    // Fallback auf den alten YAML-nativen Schlüssel ("item") für Bestandsschutz.
    public void load() {
        shopsByNpcId.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("shops");
        if (root == null) return;

        boolean migratedAny = false;
        for (String npcId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(npcId);
            if (section == null) continue;

            List<ShopEntry> entries = new ArrayList<>();
            ConfigurationSection entriesSection = section.getConfigurationSection("entries");
            if (entriesSection != null) {
                for (String key : entriesSection.getKeys(false)) {
                    double price = entriesSection.getDouble(key + ".price", 0.0);
                    ItemStack item = readItem(entriesSection, key);
                    if (item != null && item.getType() != Material.AIR) {
                        entries.add(new ShopEntry(item, price));
                        if (!entriesSection.contains(key + ".item-data")) migratedAny = true;
                    }
                }
            }
            shopsByNpcId.put(npcId, entries);
        }

        if (migratedAny) save();
    }

    private ItemStack readItem(ConfigurationSection entriesSection, String key) {
        String itemBase64 = entriesSection.getString(key + ".item-data");
        if (itemBase64 != null && !itemBase64.isBlank()) {
            try {
                return ItemStack.deserializeBytes(Base64.getDecoder().decode(itemBase64));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to deserialize shop item-data at " + key, e);
                return null;
            }
        }

        try {
            return entriesSection.getItemStack(key + ".item");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize legacy shop item at " + key, e);
            return null;
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, List<ShopEntry>> mapEntry : shopsByNpcId.entrySet()) {
            String basePath = "shops." + mapEntry.getKey() + ".entries";
            int index = 0;
            for (ShopEntry entry : mapEntry.getValue()) {
                String itemBase64 = Base64.getEncoder().encodeToString(entry.item().serializeAsBytes());
                yaml.set(basePath + "." + index + ".item-data", itemBase64);
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

    public void shutdown() {
        save();
    }

    public List<ShopEntry> getEntries(String npcId) { return shopsByNpcId.getOrDefault(npcId, List.of()); }

    public void addEntry(String npcId, ShopEntry entry) {
        shopsByNpcId.computeIfAbsent(npcId, k -> new ArrayList<>()).add(entry);
        save();
    }

    public boolean removeEntry(String npcId, int index) {
        List<ShopEntry> entries = shopsByNpcId.get(npcId);
        if (entries == null || index < 0 || index >= entries.size()) return false;
        entries.remove(index);
        save();
        return true;
    }

    public void replaceEntries(String npcId, List<ShopEntry> entries) {
        shopsByNpcId.put(npcId, entries);
        save();
    }
}
