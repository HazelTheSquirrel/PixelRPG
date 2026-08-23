package de.pixelrpg.rpg.companion;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persists equipment independently from companion progression. */
public final class CompanionEquipmentStore {
    private final File folder;
    private final Logger logger;

    public CompanionEquipmentStore(File pluginDataFolder, Logger logger) {
        this.folder = new File(pluginDataFolder, "companions/equipment");
        this.logger = logger;
        if (!folder.exists() && !folder.mkdirs()) logger.warning("Unable to create companion equipment storage folder: " + folder);
    }

    public boolean hasEntry(UUID playerId, String companionId) {
        return loadFile(playerId).contains("companions." + companionId + ".initialized");
    }

    public CompanionEquipment load(UUID playerId, String companionId) {
        YamlConfiguration yaml = loadFile(playerId);
        String path = "companions." + companionId;
        return new CompanionEquipment(
                get(yaml, path + ".helmet"),
                get(yaml, path + ".chestplate"),
                get(yaml, path + ".leggings"),
                get(yaml, path + ".boots"),
                get(yaml, path + ".main-hand"),
                get(yaml, path + ".off-hand")
        );
    }

    public void save(UUID playerId, String companionId, CompanionEquipment equipment) {
        YamlConfiguration yaml = loadFile(playerId);
        String path = "companions." + companionId;
        yaml.set(path + ".initialized", true);
        set(yaml, path + ".helmet", equipment.helmet());
        set(yaml, path + ".chestplate", equipment.chestplate());
        set(yaml, path + ".leggings", equipment.leggings());
        set(yaml, path + ".boots", equipment.boots());
        set(yaml, path + ".main-hand", equipment.mainHand());
        set(yaml, path + ".off-hand", equipment.offHand());
        try {
            yaml.save(file(playerId));
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Unable to save companion equipment for " + playerId, exception);
        }
    }

    private YamlConfiguration loadFile(UUID playerId) {
        File file = file(playerId);
        return file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    private File file(UUID playerId) {
        return new File(folder, playerId + ".yml");
    }

    private static ItemStack get(YamlConfiguration yaml, String path) {
        ItemStack item = yaml.getItemStack(path);
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    private static void set(YamlConfiguration yaml, String path, ItemStack item) {
        yaml.set(path, item == null || item.getType().isAir() ? null : item.clone());
    }
}
