package de.pixelrpg.rpg.guild;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Persists one shared 54-slot item storage per guild. */
public final class GuildBankStorageService {
    public static final int SIZE = 54;
    private final File file;
    private final YamlConfiguration data;

    public GuildBankStorageService(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "guild-bank.yml");
        data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized ItemStack[] load(UUID guildId) {
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

    public synchronized void save(UUID guildId, ItemStack[] contents) {
        data.set("guilds." + guildId, null);
        for (int slot = 0; slot < Math.min(SIZE, contents.length); slot++) {
            ItemStack item = contents[slot];
            if (item != null && !item.isEmpty()) data.createSection(path(guildId, slot), item.serialize());
        }
        try { data.save(file); }
        catch (IOException exception) { throw new IllegalStateException("Could not save guild bank", exception); }
    }

    private String path(UUID guildId, int slot) { return "guilds." + guildId + ".slots." + slot; }
}
