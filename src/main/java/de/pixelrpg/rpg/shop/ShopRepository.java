package de.pixelrpg.rpg.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public final class ShopRepository implements AutoCloseable {
    private final Plugin plugin;
    private final Path file;
    private final ExecutorService io;
    private CompletableFuture<Void> writeChain = CompletableFuture.completedFuture(null);

    public ShopRepository(Plugin plugin, Path file, ExecutorService io) {
        this.plugin = plugin;
        this.file = file;
        this.io = io;
    }

    public CompletableFuture<Map<String, List<ShopEntry>>> loadAsync() {
        return CompletableFuture.supplyAsync(this::loadSnapshot, io);
    }

    public synchronized CompletableFuture<Void> saveAsync(Map<String, List<ShopEntry>> shops) {
        Map<String, List<ShopEntry>> snapshot = new LinkedHashMap<>();
        shops.forEach((id, entries) -> snapshot.put(id, entries.stream().map(entry -> new ShopEntry(entry.item(), entry.buyPrice(), entry.sellPrice())).toList()));
        writeChain = writeChain.handle((ignored, failure) -> null)
                .thenRunAsync(() -> writeSnapshot(snapshot), io);
        return writeChain;
    }

    private Map<String, List<ShopEntry>> loadSnapshot() {
        if (!Files.exists(file)) return Map.of();
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
            ConfigurationSection root = yaml.getConfigurationSection("shops");
            if (root == null) return Map.of();
            Map<String, List<ShopEntry>> result = new LinkedHashMap<>();
            for (String npcId : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(npcId);
                if (section == null) continue;
                ConfigurationSection entriesSection = section.getConfigurationSection("entries");
                List<ShopEntry> entries = new ArrayList<>();
                if (entriesSection != null) {
                    for (String key : entriesSection.getKeys(false)) {
                        ItemStack item = readItem(entriesSection, key);
                        if (item == null || item.isEmpty()) continue;
                        double buy = entriesSection.contains(key + ".buy-price")
                                ? entriesSection.getDouble(key + ".buy-price", 0.0D)
                                : entriesSection.getDouble(key + ".price", 0.0D);
                        double sell = entriesSection.contains(key + ".sell-price")
                                ? entriesSection.getDouble(key + ".sell-price", buy * 0.50D)
                                : buy * 0.50D;
                        try {
                            entries.add(new ShopEntry(item, buy, sell));
                        } catch (IllegalArgumentException exception) {
                            plugin.getLogger().warning("Ignoring invalid shop entry " + npcId + "." + key);
                        }
                    }
                }
                result.put(npcId, List.copyOf(entries));
            }
            return Map.copyOf(result);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load shops.yml.", exception);
            throw exception;
        }
    }

    private ItemStack readItem(ConfigurationSection entries, String key) {
        String encoded = entries.getString(key + ".item-data");
        if (encoded != null && !encoded.isBlank()) {
            try {
                return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to decode shop item " + key, exception);
                return null;
            }
        }
        try {
            return entries.getItemStack(key + ".item");
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void writeSnapshot(Map<String, List<ShopEntry>> shops) {
        YamlConfiguration yaml = new YamlConfiguration();
        shops.forEach((npcId, entries) -> {
            String base = "shops." + npcId + ".entries";
            for (int index = 0; index < entries.size(); index++) {
                ShopEntry entry = entries.get(index);
                String path = base + "." + index;
                yaml.set(path + ".item-data", Base64.getEncoder().encodeToString(entry.item().serializeAsBytes()));
                yaml.set(path + ".buy-price", entry.buyPrice());
                yaml.set(path + ".sell-price", entry.sellPrice());
            }
        });
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            yaml.save(temp.toFile());
            try {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save shops.yml", exception);
        }
    }

    @Override
    public synchronized void close() {
        try {
            writeChain.get();
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Shop persistence did not flush cleanly.", exception);
        }
        io.shutdown();
    }
}
