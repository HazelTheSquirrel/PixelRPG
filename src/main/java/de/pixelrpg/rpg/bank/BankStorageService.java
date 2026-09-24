package de.pixelrpg.rpg.bank;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns the persistent personal bank and separate trade-goods compartment. */
public final class BankStorageService implements AutoCloseable {
    public static final int PAGE_SIZE = 54;
    public static final int BANK_PAGE_COUNT = 2;
    public static final int TRADE_GOODS_PAGE = 2;

    private final JavaPlugin plugin;
    private final File bankFile;
    private final File tradeGoodsFile;
    private final YamlConfiguration bankData;
    private final YamlConfiguration tradeGoodsData;
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<UUID, ItemStack[]> bankCache = new HashMap<>();
    private final Map<UUID, ItemStack[]> tradeCache = new HashMap<>();
    private boolean closed;

    public BankStorageService(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        bankFile = new File(plugin.getDataFolder(), "bank-storage.yml");
        tradeGoodsFile = new File(plugin.getDataFolder(), "trade-goods-storage.yml");
        bankData = YamlConfiguration.loadConfiguration(bankFile);
        tradeGoodsData = YamlConfiguration.loadConfiguration(tradeGoodsFile);
        migrateLegacyTradeGoods();
    }

    public synchronized ItemStack[] loadBank(UUID playerId) {
        return copy(bankCache.computeIfAbsent(playerId, id -> loadFrom(bankData, id, PAGE_SIZE * BANK_PAGE_COUNT)));
    }

    public synchronized void saveBank(UUID playerId, ItemStack[] contents) {
        ItemStack[] snapshot = normalize(contents, PAGE_SIZE * BANK_PAGE_COUNT);
        bankCache.put(playerId, snapshot);
        bankData.set("players." + playerId, null);
        writeEntries(bankData, playerId, snapshot);
        submit(bankFile, bankData.saveToString());
    }

    public synchronized ItemStack[] loadTradeGoods(UUID playerId) {
        return copy(tradeCache.computeIfAbsent(playerId, id -> loadFrom(tradeGoodsData, id, PAGE_SIZE)));
    }

    public synchronized void saveTradeGoods(UUID playerId, ItemStack[] contents) {
        ItemStack[] snapshot = normalize(contents, PAGE_SIZE);
        tradeCache.put(playerId, snapshot);
        tradeGoodsData.set("players." + playerId, null);
        writeEntries(tradeGoodsData, playerId, snapshot);
        submit(tradeGoodsFile, tradeGoodsData.saveToString());
    }

    public synchronized boolean addTradeGoods(UUID playerId, ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        ItemStack[] contents = loadTradeGoods(playerId);
        int remaining = item.getAmount();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack current = contents[slot];
            if (current == null || current.isEmpty()) continue;
            if (!current.isSimilar(item) || current.getAmount() >= current.getMaxStackSize()) continue;
            int moved = Math.min(remaining, current.getMaxStackSize() - current.getAmount());
            current.setAmount(current.getAmount() + moved);
            remaining -= moved;
        }
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            if (contents[slot] != null && !contents[slot].isEmpty()) continue;
            ItemStack placed = item.clone();
            placed.setAmount(Math.min(remaining, placed.getMaxStackSize()));
            contents[slot] = placed;
            remaining -= placed.getAmount();
        }
        if (remaining > 0) return false;
        saveTradeGoods(playerId, contents);
        return true;
    }

    public synchronized boolean canFitTradeGoods(UUID playerId, ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        ItemStack[] contents = loadTradeGoods(playerId);
        int remaining = item.getAmount();
        for (ItemStack current : contents) {
            if (current == null || current.isEmpty()) {
                remaining -= item.getMaxStackSize();
            } else if (current.isSimilar(item)) {
                remaining -= Math.max(0, current.getMaxStackSize() - current.getAmount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    public synchronized void close() {
        if (closed) return;
        closed = true;
        ioExecutor.close();
        bankCache.clear();
        tradeCache.clear();
    }

    private ItemStack[] loadFrom(YamlConfiguration source, UUID playerId, int size) {
        ItemStack[] contents = new ItemStack[size];
        for (int slot = 0; slot < size; slot++) {
            String path = path(playerId, slot);
            ConfigurationSection section = source.getConfigurationSection(path);
            if (section == null) continue;
            try { contents[slot] = ItemStack.deserialize(section.getValues(false)); }
            catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid bank item " + path + ".");
            }
        }
        return contents;
    }

    private void migrateLegacyTradeGoods() {
        if (tradeGoodsFile.exists()) return;
        ConfigurationSection players = bankData.getConfigurationSection("players");
        if (players == null) {
            try { tradeGoodsFile.createNewFile(); } catch (IOException ignored) { }
            return;
        }
        boolean changed = false;
        for (String rawId : players.getKeys(false)) {
            UUID playerId;
            try { playerId = UUID.fromString(rawId); } catch (IllegalArgumentException ignored) { continue; }
            ItemStack[] legacy = loadFrom(bankData, playerId, PAGE_SIZE * 3);
            ItemStack[] bank = new ItemStack[PAGE_SIZE * BANK_PAGE_COUNT];
            System.arraycopy(legacy, 0, bank, 0, bank.length);
            ItemStack[] trade = new ItemStack[PAGE_SIZE];
            System.arraycopy(legacy, PAGE_SIZE * BANK_PAGE_COUNT, trade, 0, PAGE_SIZE);
            bankData.set("players." + playerId, null);
            writeEntries(bankData, playerId, bank);
            tradeGoodsData.set("players." + playerId, null);
            writeEntries(tradeGoodsData, playerId, trade);
            changed = true;
        }
        if (changed) writeSync(bankFile, bankData.saveToString());
        writeSync(tradeGoodsFile, tradeGoodsData.saveToString());
    }

    private void writeEntries(YamlConfiguration data, UUID playerId, ItemStack[] contents) {
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item != null && !item.isEmpty()) data.createSection(path(playerId, slot), item.serialize());
        }
    }

    private String path(UUID playerId, int slot) {
        return "players." + playerId + ".slots." + slot;
    }

    private ItemStack[] normalize(ItemStack[] source, int size) {
        ItemStack[] result = new ItemStack[size];
        if (source == null) return result;
        for (int slot = 0; slot < Math.min(source.length, size); slot++) {
            result[slot] = source[slot] == null ? null : source[slot].clone();
        }
        return result;
    }

    private ItemStack[] copy(ItemStack[] source) {
        return normalize(source, source.length);
    }

    private void submit(File target, String content) {
        if (closed) return;
        ioExecutor.execute(() -> writeSync(target.toPath(), content));
    }

    private void writeSync(File target, String content) {
        writeSync(target.toPath(), content);
    }

    private void writeSync(Path target, String content) {
        try {
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(temp, content);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save bank storage.", exception);
        }
    }
}
