package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.core.AsyncFileWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persists the personal bank and the dedicated Handelsfach with coalesced asynchronous disk writes. */
public final class BankStorageService {
    public static final int PAGE_SIZE = 54;
    public static final int BANK_PAGE_COUNT = 2;
    public static final int TRADE_GOODS_PAGE = 2;
    public static final int DISPLAY_PAGE_COUNT = 3;

    private final File bankFile;
    private final File tradeGoodsFile;
    private final YamlConfiguration bankData;
    private final YamlConfiguration tradeGoodsData;
    private final AsyncFileWriter fileWriter;
    private final Map<UUID, ItemStack[]> bankCache = new HashMap<>();
    private final Map<UUID, ItemStack[]> tradeGoodsCache = new HashMap<>();

    public BankStorageService(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) plugin.getLogger().warning("Could not create plugin data folder for bank storage.");
        this.bankFile = new File(plugin.getDataFolder(), "bank-storage.yml");
        this.tradeGoodsFile = new File(plugin.getDataFolder(), "trade-goods-storage.yml");
        this.bankData = YamlConfiguration.loadConfiguration(bankFile);
        this.tradeGoodsData = YamlConfiguration.loadConfiguration(tradeGoodsFile);
        this.fileWriter = new AsyncFileWriter(plugin, "PixelRPG-BankIO");
        migrateLegacyTradeGoods(plugin);
    }

    public synchronized ItemStack[] load(UUID playerId) {
        ItemStack[] cached = bankCache.get(playerId);
        if (cached == null) {
            cached = loadFrom(bankData, playerId, PAGE_SIZE * BANK_PAGE_COUNT);
            bankCache.put(playerId, cached);
        }
        return copyContents(cached);
    }

    public synchronized void save(UUID playerId, ItemStack[] contents) {
        saveTo(bankData, bankFile, bankCache, playerId, contents, PAGE_SIZE * BANK_PAGE_COUNT);
    }

    public synchronized ItemStack[] loadTradeGoods(UUID playerId) {
        ItemStack[] cached = tradeGoodsCache.get(playerId);
        if (cached == null) {
            cached = loadFrom(tradeGoodsData, playerId, PAGE_SIZE);
            tradeGoodsCache.put(playerId, cached);
        }
        return copyContents(cached);
    }

    public synchronized void saveTradeGoods(UUID playerId, ItemStack[] contents) {
        saveTo(tradeGoodsData, tradeGoodsFile, tradeGoodsCache, playerId, contents, PAGE_SIZE);
    }

    /** Places an expired listing into the dedicated Handelsfach without touching the personal bank. */
    public synchronized boolean addTradeGoods(UUID playerId, ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        ItemStack[] contents = loadTradeGoods(playerId);
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            ItemStack current = contents[slot];
            if (current == null || current.isEmpty()) {
                contents[slot] = item.clone();
                saveTradeGoods(playerId, contents);
                return true;
            }
            if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                int space = current.getMaxStackSize() - current.getAmount();
                int moved = Math.min(space, item.getAmount());
                current.setAmount(current.getAmount() + moved);
                item = item.clone();
                item.setAmount(item.getAmount() - moved);
                if (item.isEmpty()) {
                    saveTradeGoods(playerId, contents);
                    return true;
                }
            }
        }
        return false;
    }

    /** Flushes pending personal-bank and trade-goods writes during plugin shutdown. */
    public void shutdown() {
        fileWriter.shutdown();
    }

    private ItemStack[] loadFrom(YamlConfiguration source, UUID playerId, int size) {
        ItemStack[] contents = new ItemStack[size];
        for (int slot = 0; slot < contents.length; slot++) {
            String path = path(playerId, slot);
            if (!source.contains(path)) continue;
            Map<String, Object> serialized = source.getConfigurationSection(path) == null ? null : source.getConfigurationSection(path).getValues(false);
            if (serialized == null || serialized.isEmpty()) continue;
            try { contents[slot] = ItemStack.deserialize(serialized); }
            catch (IllegalArgumentException ignored) { }
        }
        return contents;
    }

    private void saveTo(YamlConfiguration target, File file, Map<UUID, ItemStack[]> cache, UUID playerId, ItemStack[] contents, int size) {
        ItemStack[] snapshot = copyContents(contents);
        cache.put(playerId, snapshot);
        String prefix = "players." + playerId;
        target.set(prefix, null);
        for (int slot = 0; slot < Math.min(snapshot.length, size); slot++) {
            ItemStack item = snapshot[slot];
            if (item == null || item.isEmpty()) continue;
            target.createSection(path(playerId, slot), item.serialize());
        }
        fileWriter.submit(file.toPath(), target.saveToString());
    }

    private ItemStack[] copyContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            copy[slot] = item == null ? null : item.clone();
        }
        return copy;
    }

    private void migrateLegacyTradeGoods(JavaPlugin plugin) {
        if (tradeGoodsFile.exists()) return;
        ConfigurationSection players = bankData.getConfigurationSection("players");
        if (players == null) return;
        boolean changed = false;
        for (String rawPlayerId : players.getKeys(false)) {
            UUID playerId;
            try { playerId = UUID.fromString(rawPlayerId); }
            catch (IllegalArgumentException ignored) { continue; }
            ItemStack[] legacy = loadFrom(bankData, playerId, PAGE_SIZE * DISPLAY_PAGE_COUNT);
            ItemStack[] bank = new ItemStack[PAGE_SIZE * BANK_PAGE_COUNT];
            System.arraycopy(legacy, 0, bank, 0, bank.length);
            ItemStack[] trade = new ItemStack[PAGE_SIZE];
            System.arraycopy(legacy, PAGE_SIZE * BANK_PAGE_COUNT, trade, 0, PAGE_SIZE);
            writeSync(tradeGoodsData, tradeGoodsFile, playerId, trade, PAGE_SIZE);
            bankData.set("players." + playerId, null);
            for (int slot = 0; slot < bank.length; slot++) {
                ItemStack item = bank[slot];
                if (item != null && !item.isEmpty()) bankData.createSection(path(playerId, slot), item.serialize());
            }
            changed = true;
        }
        if (changed) writeSync(bankData, bankFile);
        else {
            try { tradeGoodsFile.createNewFile(); }
            catch (IOException exception) { plugin.getLogger().warning("Could not initialize trade goods storage: " + exception.getMessage()); }
        }
    }

    private void writeSync(YamlConfiguration target, File file, UUID playerId, ItemStack[] contents, int size) {
        String prefix = "players." + playerId;
        target.set(prefix, null);
        for (int slot = 0; slot < Math.min(contents.length, size); slot++) {
            ItemStack item = contents[slot];
            if (item != null && !item.isEmpty()) target.createSection(path(playerId, slot), item.serialize());
        }
        writeSync(target, file);
    }

    private void writeSync(YamlConfiguration target, File file) {
        try { target.save(file); }
        catch (IOException exception) { throw new IllegalStateException("Could not save bank storage", exception); }
    }

    private String path(UUID playerId, int slot) { return "players." + playerId + ".slots." + slot; }
}
