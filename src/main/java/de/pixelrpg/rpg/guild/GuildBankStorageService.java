package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.core.AsyncFileWriter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GuildBankStorageService implements AutoCloseable {
    public static final int SIZE = 54;
    private final AsyncFileWriter writer;
    private final java.nio.file.Path path;
    private final YamlConfiguration data;
    private final Map<UUID, ItemStack[]> cache = new HashMap<>();

    public GuildBankStorageService(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "guild-bank.yml");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        path = file.toPath();
        data = YamlConfiguration.loadConfiguration(file);
        writer = new AsyncFileWriter(plugin, "PixelRPG-GuildBank");
    }

    public synchronized ItemStack[] load(UUID guildId) {
        ItemStack[] cached = cache.computeIfAbsent(guildId, this::loadFromData);
        ItemStack[] copy = new ItemStack[SIZE];
        for (int i = 0; i < SIZE; i++) copy[i] = cached[i] == null ? null : cached[i].clone();
        return copy;
    }

    public synchronized void save(UUID guildId, ItemStack[] contents) {
        ItemStack[] snapshot = new ItemStack[SIZE];
        data.set("guilds." + guildId, null);
        for (int i = 0; i < SIZE; i++) {
            ItemStack item = i < contents.length ? contents[i] : null;
            snapshot[i] = item == null ? null : item.clone();
            if (item != null && !item.isEmpty()) data.createSection("guilds." + guildId + ".slots." + i, item.serialize());
        }
        cache.put(guildId, snapshot);
        writer.submit(path, data.saveToString());
    }

    private ItemStack[] loadFromData(UUID guildId) {
        ItemStack[] result = new ItemStack[SIZE];
        for (int i = 0; i < SIZE; i++) {
            var section = data.getConfigurationSection("guilds." + guildId + ".slots." + i);
            if (section == null) continue;
            try { result[i] = ItemStack.deserialize(section.getValues(false)); } catch (IllegalArgumentException ignored) { }
        }
        return result;
    }

    @Override public void close() { writer.shutdown(); }
}
