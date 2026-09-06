package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.core.AsyncFileWriter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persists one shared 54-slot item storage per guild. */
public final class GuildBankStorageService {
    public static final int SIZE = 54;
    private final AsyncFileWriter fileWriter;
    private final java.nio.file.Path filePath;
    private final YamlConfiguration data;
    private final Map<UUID, ItemStack[]> cache = new HashMap<>();

    public GuildBankStorageService(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        File file = new File(plugin.getDataFolder(), "guild-bank.yml");
        filePath = file.toPath();
        data = YamlConfiguration.loadConfiguration(file);
        fileWriter = new AsyncFileWriter(plugin, "PixelRPG-GuildBank");
    }

    /** Returns a defensive snapshot while reusing the parsed guild-bank state. */
    public synchronized ItemStack[] load(UUID guildId) {
        ItemStack[] cached = cache.get(guildId);
        if (cached == null) {
            cached = loadFromData(guildId);
            cache.put(guildId, cached);
        }
        return copyContents(cached);
    }

    /** Updates the in-memory guild-bank state and queues the latest complete storage snapshot for persistence. */
    public synchronized void save(UUID guildId, ItemStack[] contents) {
        ItemStack[] snapshot = copyContents(contents);
        cache.put(guildId, snapshot);
        data.set("guilds." + guildId, null);
        for (int slot = 0; slot < snapshot.length; slot++) {
            ItemStack item = snapshot[slot];
            if (item != null && !item.isEmpty()) data.createSection(path(guildId, slot), item.serialize());
        }
        fileWriter.submit(filePath, data.saveToString());
    }

    /** Flushes pending guild-bank snapshots and releases the asynchronous writer. */
    public void shutdown() {
        fileWriter.shutdown();
    }

    private ItemStack[] loadFromData(UUID guildId) {
        ItemStack[] contents = new ItemStack[SIZE];
        for (int slot = 0; slot < SIZE; slot++) {
            String path = path(guildId, slot);
            if (!data.contains(path)) continue;
            var section = data.getConfigurationSection(path);
            if (section == null) continue;
            Map<String, Object> serialized = section.getValues(false);
            try { contents[slot] = ItemStack.deserialize(serialized); }
            catch (IllegalArgumentException ignored) { }
        }
        return contents;
    }

    private ItemStack[] copyContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[SIZE];
        for (int slot = 0; slot < Math.min(SIZE, contents.length); slot++) {
            ItemStack item = contents[slot];
            copy[slot] = item == null ? null : item.clone();
        }
        return copy;
    }

    private String path(UUID guildId, int slot) { return "guilds." + guildId + ".slots." + slot; }
}
