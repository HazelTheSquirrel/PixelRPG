package de.pixelrpg.rpg.shop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/** Owns shop definitions and persists independent buy/sell prices per NPC. */
public final class ShopManager implements AutoCloseable {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, List<ShopEntry>> shopsByNpcId = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean closed;

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shops.yml");
    }

    public void load() {
        shopsByNpcId.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("shops");
        if (root == null) return;

        boolean migrated = false;
        for (String npcId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(npcId);
            if (section == null) continue;
            ConfigurationSection entriesSection = section.getConfigurationSection("entries");
            if (entriesSection == null) {
                shopsByNpcId.put(npcId, List.of());
                continue;
            }

            List<ShopEntry> entries = new ArrayList<>();
            for (String key : entriesSection.getKeys(false)) {
                boolean hasBuy = entriesSection.contains(key + ".buy-price");
                double buy = hasBuy ? entriesSection.getDouble(key + ".buy-price", 0.0D)
                        : entriesSection.getDouble(key + ".price", 0.0D);
                double sell = entriesSection.contains(key + ".sell-price")
                        ? entriesSection.getDouble(key + ".sell-price", 0.0D)
                        : buy * 0.50D;
                ItemStack item = readItem(entriesSection, key);
                if (item == null || item.getType() == Material.AIR) continue;
                try {
                    entries.add(new ShopEntry(item, buy, sell));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring invalid shop entry " + npcId + "." + key);
                }
                if (!hasBuy || !entriesSection.contains(key + ".sell-price") || !entriesSection.contains(key + ".item-data")) {
                    migrated = true;
                }
            }
            shopsByNpcId.put(npcId, List.copyOf(entries));
        }

        if (migrated) save();
    }

    private ItemStack readItem(ConfigurationSection section, String key) {
        String encoded = section.getString(key + ".item-data");
        if (encoded != null && !encoded.isBlank()) {
            try {
                return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to deserialize shop item " + key, exception);
                return null;
            }
        }
        try {
            return section.getItemStack(key + ".item");
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize legacy shop item " + key, exception);
            return null;
        }
    }

    public synchronized void save() {
        if (closed || ioExecutor.isShutdown()) return;
        Map<String, List<ShopEntry>> snapshot = Map.copyOf(shopsByNpcId);
        ioExecutor.execute(() -> write(snapshot));
    }

    private void write(Map<String, List<ShopEntry>> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, List<ShopEntry>> shop : snapshot.entrySet()) {
            String base = "shops." + shop.getKey() + ".entries";
            for (int index = 0; index < shop.getValue().size(); index++) {
                ShopEntry entry = shop.getValue().get(index);
                String path = base + "." + index;
                yaml.set(path + ".item-data", Base64.getEncoder().encodeToString(entry.item().serializeAsBytes()));
                yaml.set(path + ".buy-price", entry.buyPrice());
                yaml.set(path + ".sell-price", entry.sellPrice());
            }
        }

        Path target = file.toPath();
        Path temp = target.resolveSibling(file.getName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            yaml.save(temp.toFile());
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save shops.yml", exception);
        }
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

    @Override
    public void close() {
        if (closed) return;
        save();
        closed = true;
        ioExecutor.close();
    }

    public void shutdown() {
        close();
    }
}
