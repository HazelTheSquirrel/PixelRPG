package de.pixelrpg.rpg.dialogue;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Persists the personal bank and the dedicated Handelsfach in separate storage files. */
public final class BankStorageService {
    public static final int PAGE_SIZE = 54;
    public static final int BANK_PAGE_COUNT = 2;
    public static final int TRADE_GOODS_PAGE = 2;
    public static final int DISPLAY_PAGE_COUNT = 3;

    private final File bankFile;
    private final File tradeGoodsFile;
    private final YamlConfiguration bankData;
    private final YamlConfiguration tradeGoodsData;

    public BankStorageService(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for bank storage.");
        }

        this.bankFile = new File(plugin.getDataFolder(), "bank-storage.yml");
        this.tradeGoodsFile = new File(plugin.getDataFolder(), "trade-goods-storage.yml");
        this.bankData = YamlConfiguration.loadConfiguration(bankFile);
        this.tradeGoodsData = YamlConfiguration.loadConfiguration(tradeGoodsFile);
        migrateLegacyTradeGoods(plugin);
    }

    public synchronized ItemStack[] load(UUID playerId) {
        return loadFrom(bankData, playerId, PAGE_SIZE * BANK_PAGE_COUNT);
    }

    public synchronized void save(UUID playerId, ItemStack[] contents) {
        saveTo(bankData, bankFile, playerId, contents, PAGE_SIZE * BANK_PAGE_COUNT);
    }

    public synchronized ItemStack[] loadTradeGoods(UUID playerId) {
        return loadFrom(tradeGoodsData, playerId, PAGE_SIZE);
    }

    public synchronized void saveTradeGoods(UUID playerId, ItemStack[] contents) {
        saveTo(tradeGoodsData, tradeGoodsFile, playerId, contents, PAGE_SIZE);
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

    private ItemStack[] loadFrom(YamlConfiguration source, UUID playerId, int size) {
        ItemStack[] contents = new ItemStack[size];
        for (int slot = 0; slot < contents.length; slot++) {
            String path = path(playerId, slot);
            if (!source.contains(path)) continue;

            Map<String, Object> serialized = source.getConfigurationSection(path) == null
                    ? null
                    : source.getConfigurationSection(path).getValues(false);
            if (serialized == null || serialized.isEmpty()) continue;

            try {
                contents[slot] = ItemStack.deserialize(serialized);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy data instead of preventing the bank from opening.
            }
        }
        return contents;
    }

    private void saveTo(YamlConfiguration target, File file, UUID playerId, ItemStack[] contents, int size) {
        String prefix = "players." + playerId;
        target.set(prefix, null);

        for (int slot = 0; slot < Math.min(contents.length, size); slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.isEmpty()) continue;
            target.createSection(path(playerId, slot), item.serialize());
        }

        write(target, file);
    }

    private void migrateLegacyTradeGoods(JavaPlugin plugin) {
        if (tradeGoodsFile.exists()) return;

        ConfigurationSection players = bankData.getConfigurationSection("players");
        if (players == null) return;

        boolean changed = false;
        for (String rawPlayerId : players.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(rawPlayerId);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ItemStack[] legacy = loadFrom(bankData, playerId, PAGE_SIZE * DISPLAY_PAGE_COUNT);
            ItemStack[] bank = new ItemStack[PAGE_SIZE * BANK_PAGE_COUNT];
            System.arraycopy(legacy, 0, bank, 0, bank.length);

            ItemStack[] trade = new ItemStack[PAGE_SIZE];
            System.arraycopy(legacy, PAGE_SIZE * BANK_PAGE_COUNT, trade, 0, PAGE_SIZE);
            saveTo(tradeGoodsData, tradeGoodsFile, playerId, trade, PAGE_SIZE);

            bankData.set("players." + playerId, null);
            for (int slot = 0; slot < bank.length; slot++) {
                ItemStack item = bank[slot];
                if (item != null && !item.isEmpty()) bankData.createSection(path(playerId, slot), item.serialize());
            }
            changed = true;
        }

        if (changed) write(bankData, bankFile);
        else {
            try {
                tradeGoodsFile.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not initialize trade goods storage: " + exception.getMessage());
            }
        }
    }

    private void write(YamlConfiguration target, File file) {
        try {
            target.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save bank storage", exception);
        }
    }

    private String path(UUID playerId, int slot) {
        return "players." + playerId + ".slots." + slot;
    }
}
