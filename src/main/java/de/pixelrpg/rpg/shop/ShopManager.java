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

/** Owns shop definitions and persists independent buy/sell prices per offer. */
public final class ShopManager {
    private final Plugin plugin;
    private final File file;
    private final Map<String, List<ShopEntry>> shopsByNpcId = new ConcurrentHashMap<>();

    public ShopManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shops.yml");
    }

    // Lädt Shop-Items und unterstützt sowohl das alte einzelne "price"-Feld als auch die neuen Kauf-/Verkaufspreise.
    public void load() {
        shopsByNpcId.clear();

        YamlConfiguration yaml = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        ConfigurationSection root = yaml.getConfigurationSection("shops");

        boolean migratedAny = false;
        if (root != null) for (String npcId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(npcId);
            if (section == null) continue;

            List<ShopEntry> entries = new ArrayList<>();
            ConfigurationSection entriesSection = section.getConfigurationSection("entries");
            if (entriesSection != null) {
                for (String key : entriesSection.getKeys(false)) {
                    boolean hasBuyPrice = entriesSection.contains(key + ".buy-price");
                    double buyPrice = hasBuyPrice
                            ? entriesSection.getDouble(key + ".buy-price", 0.0D)
                            : entriesSection.getDouble(key + ".price", 0.0D);
                    double sellPrice = entriesSection.contains(key + ".sell-price")
                            ? entriesSection.getDouble(key + ".sell-price", 0.0D)
                            : buyPrice * 0.50D;
                    ItemStack item = readItem(entriesSection, key);
                    if (item != null && item.getType() != Material.AIR) {
                        try {
                            entries.add(new ShopEntry(item, buyPrice, sellPrice));
                        } catch (IllegalArgumentException ignored) {
                            plugin.getLogger().warning("Ignoring invalid shop price at " + npcId + ".entries." + key);
                        }
                        if (!hasBuyPrice || !entriesSection.contains(key + ".sell-price") || !entriesSection.contains(key + ".item-data")) {
                            migratedAny = true;
                        }
                    }
                }
            }
            shopsByNpcId.put(npcId, List.copyOf(entries));
        }

        boolean defaultAdded = ensureDefaultVanillaBuildingShop();
        if (migratedAny || defaultAdded) save();
    }

    private boolean ensureDefaultVanillaBuildingShop() {
        String shopId = "vanilla_building_blocks";
        if (!getEntries(shopId).isEmpty()) return false;

        List<ShopEntry> entries = new ArrayList<>();
        addDefaultEntry(entries, Material.STONE, 1.00D);
        addDefaultEntry(entries, Material.COBBLESTONE, 0.50D);
        addDefaultEntry(entries, Material.DEEPSLATE, 1.25D);
        addDefaultEntry(entries, Material.COBBLED_DEEPSLATE, 0.75D);
        addDefaultEntry(entries, Material.GRANITE, 0.75D);
        addDefaultEntry(entries, Material.DIORITE, 0.75D);
        addDefaultEntry(entries, Material.ANDESITE, 0.75D);
        addDefaultEntry(entries, Material.TUFF, 0.75D);
        addDefaultEntry(entries, Material.CALCITE, 1.25D);
        addDefaultEntry(entries, Material.BLACKSTONE, 1.25D);
        addDefaultEntry(entries, Material.BASALT, 1.00D);
        addDefaultEntry(entries, Material.SAND, 0.75D);
        addDefaultEntry(entries, Material.RED_SAND, 1.00D);
        addDefaultEntry(entries, Material.GRAVEL, 0.50D);
        addDefaultEntry(entries, Material.SANDSTONE, 1.25D);
        addDefaultEntry(entries, Material.RED_SANDSTONE, 1.50D);
        addDefaultEntry(entries, Material.BRICKS, 2.50D);
        addDefaultEntry(entries, Material.STONE_BRICKS, 1.25D);
        addDefaultEntry(entries, Material.DEEPSLATE_BRICKS, 1.50D);
        addDefaultEntry(entries, Material.DEEPSLATE_TILES, 1.50D);
        addDefaultEntry(entries, Material.MUD_BRICKS, 2.00D);
        addDefaultEntry(entries, Material.OAK_PLANKS, 0.75D);
        addDefaultEntry(entries, Material.SPRUCE_PLANKS, 0.75D);
        addDefaultEntry(entries, Material.BIRCH_PLANKS, 0.75D);
        addDefaultEntry(entries, Material.JUNGLE_PLANKS, 0.75D);
        addDefaultEntry(entries, Material.ACACIA_PLANKS, 0.75D);
        addDefaultEntry(entries, Material.DARK_OAK_PLANKS, 0.75D);
        addDefaultEntry(entries, Material.MANGROVE_PLANKS, 0.75D);
        addDefaultEntry(entries, Material.CHERRY_PLANKS, 0.75D);
        addDefaultEntry(entries, Material.PALE_OAK_PLANKS, 0.75D);
        addDefaultEntry(entries, Material.GLASS, 2.00D);
        addDefaultEntry(entries, Material.TERRACOTTA, 2.00D);
        addDefaultEntry(entries, Material.QUARTZ_BLOCK, 5.00D);
        addDefaultEntry(entries, Material.PRISMARINE, 3.50D);
        addDefaultEntry(entries, Material.OBSIDIAN, 10.00D);
        addDefaultEntry(entries, Material.WHITE_CONCRETE, 3.00D);
        addDefaultEntry(entries, Material.GRAY_CONCRETE, 3.00D);
        addDefaultEntry(entries, Material.BLACK_CONCRETE, 3.00D);
        shopsByNpcId.put(shopId, List.copyOf(entries));
        plugin.getLogger().info("Created default vanilla building-block shop '" + shopId + "'.");
        return true;
    }

    private static void addDefaultEntry(List<ShopEntry> entries, Material material, double buyPrice) {
        entries.add(new ShopEntry(ItemStack.of(material, 64), buyPrice, buyPrice * 0.50D));
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

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, List<ShopEntry>> mapEntry : shopsByNpcId.entrySet()) {
            String basePath = "shops." + mapEntry.getKey() + ".entries";
            int index = 0;
            for (ShopEntry entry : mapEntry.getValue()) {
                String itemBase64 = Base64.getEncoder().encodeToString(entry.item().serializeAsBytes());
                yaml.set(basePath + "." + index + ".item-data", itemBase64);
                yaml.set(basePath + "." + index + ".buy-price", entry.buyPrice());
                yaml.set(basePath + "." + index + ".sell-price", entry.sellPrice());
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

    public List<ShopEntry> getEntries(String npcId) {
        return shopsByNpcId.getOrDefault(npcId, List.of());
    }

    public synchronized void addEntry(String npcId, ShopEntry entry) {
        List<ShopEntry> entries = new ArrayList<>(getEntries(npcId));
        entries.add(entry);
        shopsByNpcId.put(npcId, List.copyOf(entries));
        save();
    }

    public synchronized boolean removeEntry(String npcId, int index) {
        List<ShopEntry> existing = shopsByNpcId.get(npcId);
        if (existing == null || index < 0 || index >= existing.size()) return false;
        List<ShopEntry> entries = new ArrayList<>(existing);
        entries.remove(index);
        shopsByNpcId.put(npcId, List.copyOf(entries));
        save();
        return true;
    }

    public synchronized void replaceEntries(String npcId, List<ShopEntry> entries) {
        shopsByNpcId.put(npcId, List.copyOf(entries));
        save();
    }
}
