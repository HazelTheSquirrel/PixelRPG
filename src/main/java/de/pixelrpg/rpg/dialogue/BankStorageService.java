package de.pixelrpg.rpg.dialogue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Persists the personal two-page bank storage independently from the vanilla Ender Chest. */
public final class BankStorageService {
    public static final int PAGE_SIZE = 54;
    public static final int PAGE_COUNT = 2;

    private final File file;
    private final YamlConfiguration data;

    public BankStorageService(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for bank storage.");
        }

        this.file = new File(plugin.getDataFolder(), "bank-storage.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public ItemStack[] load(UUID playerId) {
        ItemStack[] contents = new ItemStack[PAGE_SIZE * PAGE_COUNT];
        for (int slot = 0; slot < contents.length; slot++) {
            String path = path(playerId, slot);
            if (!data.contains(path)) continue;

            Map<String, Object> serialized = data.getConfigurationSection(path) == null
                    ? null
                    : data.getConfigurationSection(path).getValues(false);
            if (serialized == null || serialized.isEmpty()) continue;

            try {
                contents[slot] = ItemStack.deserialize(serialized);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy data instead of preventing the bank from opening.
            }
        }
        return contents;
    }

    public void save(UUID playerId, ItemStack[] contents) {
        String prefix = "players." + playerId;
        data.getConfigurationSection(prefix);
        data.set(prefix, null);

        for (int slot = 0; slot < Math.min(contents.length, PAGE_SIZE * PAGE_COUNT); slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.isEmpty()) continue;
            data.createSection(path(playerId, slot), item.serialize());
        }

        try {
            data.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save bank storage", exception);
        }
    }

    private String path(UUID playerId, int slot) {
        return "players." + playerId + ".slots." + slot;
    }
}
