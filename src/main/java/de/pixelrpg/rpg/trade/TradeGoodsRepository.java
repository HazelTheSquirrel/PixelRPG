package de.pixelrpg.rpg.trade;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public final class TradeGoodsRepository implements AutoCloseable {
    public static final int PAGE_SIZE = 54;
    private final Plugin plugin;
    private final Path file;
    private final ExecutorService io;
    private Map<UUID, ItemStack[]> state = new LinkedHashMap<>();
    private CompletableFuture<Void> writeChain = CompletableFuture.completedFuture(null);

    public TradeGoodsRepository(Plugin plugin, Path file, ExecutorService io) {
        this.plugin = plugin;
        this.file = file;
        this.io = io;
    }

    public CompletableFuture<Map<UUID, ItemStack[]>> loadAsync() {
        return CompletableFuture.supplyAsync(this::loadSnapshot, io).thenApply(snapshot -> {
            synchronized (this) {
                state = copyMap(snapshot);
                return copyMap(state);
            }
        });
    }

    public synchronized ItemStack[] get(UUID playerId) {
        return copy(state.getOrDefault(playerId, new ItemStack[PAGE_SIZE]));
    }

    public synchronized void set(UUID playerId, ItemStack[] contents) {
        state.put(playerId, copy(contents));
        Map<UUID, ItemStack[]> snapshot = copyMap(state);
        writeChain = writeChain.handle((ignored, failure) -> null)
                .thenRunAsync(() -> writeSnapshot(snapshot), io);
    }

    private Map<UUID, ItemStack[]> loadSnapshot() {
        if (!Files.exists(file)) return Map.of();
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
            ConfigurationSection players = yaml.getConfigurationSection("players");
            if (players == null) return Map.of();
            Map<UUID, ItemStack[]> result = new LinkedHashMap<>();
            for (String rawId : players.getKeys(false)) {
                UUID id;
                try { id = UUID.fromString(rawId); } catch (IllegalArgumentException ignored) { continue; }
                ItemStack[] contents = new ItemStack[PAGE_SIZE];
                ConfigurationSection slots = players.getConfigurationSection(rawId + ".slots");
                if (slots != null) {
                    for (String rawSlot : slots.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(rawSlot);
                            String encoded = slots.getString(rawSlot);
                            if (slot >= 0 && slot < PAGE_SIZE && encoded != null) {
                                contents[slot] = ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
                            }
                        } catch (RuntimeException ignored) { }
                    }
                }
                result.put(id, contents);
            }
            return result;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load trade-goods-storage.yml.", exception);
            throw exception;
        }
    }

    private void writeSnapshot(Map<UUID, ItemStack[]> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        snapshot.forEach((playerId, contents) -> {
            for (int slot = 0; slot < Math.min(PAGE_SIZE, contents.length); slot++) {
                ItemStack item = contents[slot];
                if (item == null || item.isEmpty()) continue;
                yaml.set("players." + playerId + ".slots." + slot,
                        Base64.getEncoder().encodeToString(item.serializeAsBytes()));
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
            throw new IllegalStateException("Failed to save trade goods.", exception);
        }
    }

    private static ItemStack[] copy(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[PAGE_SIZE];
        for (int i = 0; i < Math.min(PAGE_SIZE, contents.length); i++) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }
        return copy;
    }

    private static Map<UUID, ItemStack[]> copyMap(Map<UUID, ItemStack[]> source) {
        Map<UUID, ItemStack[]> copy = new LinkedHashMap<>();
        source.forEach((id, contents) -> copy.put(id, copy(contents)));
        return copy;
    }

    @Override
    public synchronized void close() {
        try { writeChain.get(); }
        catch (Exception exception) { plugin.getLogger().log(Level.SEVERE, "Trade goods persistence did not flush cleanly.", exception); }
        io.shutdown();
    }
}
