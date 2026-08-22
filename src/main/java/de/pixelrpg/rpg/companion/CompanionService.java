package de.pixelrpg.rpg.companion;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns persistent companion definitions, progression and active entity state. */
public final class CompanionService {
    private static final String TEST_WOLF_ID = "test-wolf";
    private static final String TEST_WOLF_NAME = "PixelRPG Wolf";
    private static final int MAX_LEVEL = 99;

    private final Plugin plugin;
    private final File storageFolder;
    private final Map<UUID, List<Companion>> companions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activeEntities = new ConcurrentHashMap<>();

    public CompanionService(Plugin plugin) {
        this.plugin = plugin;
        this.storageFolder = new File(plugin.getDataFolder(), "companions");
        if (!storageFolder.exists()) storageFolder.mkdirs();
    }

    public List<Companion> getCompanions(UUID playerId) {
        load(playerId);
        return List.copyOf(companions.getOrDefault(playerId, List.of()));
    }

    /** Adds the temporary wolf used for the companion-system test. */
    public void ensureTestWolf(UUID playerId) {
        load(playerId);
        companions.compute(playerId, (ignored, current) -> {
            List<Companion> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
            if (updated.stream().noneMatch(existing -> existing.id().equals(TEST_WOLF_ID))) {
                updated.add(new Companion(TEST_WOLF_ID, TEST_WOLF_NAME, 1, 0L,
                        CompanionRarity.UNCOMMON, org.bukkit.entity.EntityType.WOLF, false));
                save(playerId, updated);
            }
            return updated;
        });
    }

    public void unlock(UUID playerId, Companion companion) {
        load(playerId);
        companions.compute(playerId, (ignored, current) -> {
            List<Companion> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
            if (updated.stream().noneMatch(existing -> existing.id().equals(companion.id()))) {
                updated.add(companion.withActive(false));
                save(playerId, updated);
            }
            return updated;
        });
    }

    /** Unlocks a fixed-name Unique companion. Only administrative code should call this method. */
    public void unlockUnique(UUID playerId, String id, String fixedName, org.bukkit.entity.EntityType entityType) {
        unlock(playerId, new Companion(id, fixedName, 1, 0L, CompanionRarity.UNIQUE, entityType, false));
    }

    public boolean setActive(Player player, String companionId) {
        UUID playerId = player.getUniqueId();
        load(playerId);
        List<Companion> current = companions.get(playerId);
        if (current == null || current.stream().noneMatch(companion -> companion.id().equals(companionId))) return false;

        clearActiveEntity(player);
        List<Companion> updated = current.stream()
                .map(companion -> companion.withActive(companion.id().equals(companionId)))
                .toList();
        companions.put(playerId, updated);
        save(playerId, updated);

        Companion selected = updated.stream().filter(companion -> companion.id().equals(companionId)).findFirst().orElseThrow();
        if (selected.entityType() == org.bukkit.entity.EntityType.WOLF) {
            Location spawnLocation = player.getLocation().clone().add(1.0, 0.0, 1.0);
            Wolf wolf = player.getWorld().spawn(spawnLocation, Wolf.class, spawned -> {
                spawned.setTamed(true);
                spawned.setOwner(player);
                spawned.setAdult();
                spawned.setCustomNameVisible(true);
                spawned.setCustomName(selected.name());
                spawned.setAngry(false);
                spawned.setTarget(null);
            });
            activeEntities.put(playerId, wolf.getUniqueId());
        }
        return true;
    }

    public boolean setActive(UUID playerId, String companionId) {
        Player player = plugin.getServer().getPlayer(playerId);
        return player != null && setActive(player, companionId);
    }

    public void clearActive(Player player) {
        clearActiveEntity(player);
        UUID playerId = player.getUniqueId();
        load(playerId);
        List<Companion> current = companions.get(playerId);
        if (current == null) return;
        List<Companion> updated = current.stream().map(companion -> companion.withActive(false)).toList();
        companions.put(playerId, updated);
        save(playerId, updated);
    }

    public void clearActive(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            clearActive(player);
            return;
        }
        UUID entityId = activeEntities.remove(playerId);
        if (entityId != null) removeEntity(entityId);
        load(playerId);
        List<Companion> current = companions.get(playerId);
        if (current != null) {
            List<Companion> updated = current.stream().map(companion -> companion.withActive(false)).toList();
            companions.put(playerId, updated);
            save(playerId, updated);
        }
    }

    public Companion getActive(UUID playerId) {
        return getCompanions(playerId).stream().filter(Companion::active).findFirst().orElse(null);
    }

    public boolean rename(UUID playerId, String companionId, String newName) {
        load(playerId);
        String cleaned = newName == null ? "" : newName.strip();
        if (cleaned.isEmpty() || cleaned.length() > 24) return false;
        List<Companion> current = companions.get(playerId);
        if (current == null) return false;
        Companion selected = current.stream().filter(companion -> companion.id().equals(companionId)).findFirst().orElse(null);
        if (selected == null || selected.rarity().isUnique()) return false;

        List<Companion> updated = current.stream()
                .map(companion -> companion.id().equals(companionId) ? companion.withName(cleaned) : companion)
                .toList();
        companions.put(playerId, updated);
        save(playerId, updated);

        UUID entityId = activeEntities.get(playerId);
        if (entityId != null) {
            Entity entity = plugin.getServer().getEntity(entityId);
            if (entity instanceof LivingEntity living) living.setCustomName(cleaned);
        }
        return true;
    }

    /** Awards companion XP only to the currently active companion. */
    public boolean awardExperience(UUID playerId, long baseExperience) {
        if (baseExperience <= 0L) return false;
        load(playerId);
        List<Companion> current = companions.get(playerId);
        if (current == null) return false;
        Companion active = current.stream().filter(Companion::active).findFirst().orElse(null);
        if (active == null || active.level() >= MAX_LEVEL) return false;

        long gained = Math.max(1L, Math.round(baseExperience));
        long experience = active.experience() + gained;
        int level = calculateLevel(experience);
        level = Math.min(MAX_LEVEL, level);
        List<Companion> updated = current.stream()
                .map(companion -> companion.id().equals(active.id()) ? companion.withProgress(level, experience) : companion)
                .toList();
        companions.put(playerId, updated);
        save(playerId, updated);
        return level != active.level();
    }

    public int calculateLevel(long experience) {
        long remaining = Math.max(0L, experience);
        int level = 1;
        while (level < MAX_LEVEL) {
            long required = experienceToNextLevel(level);
            if (remaining < required) break;
            remaining -= required;
            level++;
        }
        return level;
    }

    public long experienceToNextLevel(int level) {
        if (level >= MAX_LEVEL) return Long.MAX_VALUE;
        return 100L + (long) level * 50L;
    }

    public long experienceWithinLevel(Companion companion) {
        long remaining = companion.experience();
        for (int level = 1; level < companion.level(); level++) remaining -= experienceToNextLevel(level);
        return Math.max(0L, remaining);
    }

    public long experienceNeededForCurrentLevel(Companion companion) {
        return experienceToNextLevel(companion.level());
    }

    public void shutdown() {
        for (UUID entityId : activeEntities.values()) removeEntity(entityId);
        activeEntities.clear();
        for (UUID playerId : companions.keySet()) {
            List<Companion> current = companions.get(playerId);
            if (current != null) save(playerId, current.stream().map(companion -> companion.withActive(false)).toList());
        }
        companions.clear();
    }

    private void clearActiveEntity(Player player) {
        UUID entityId = activeEntities.remove(player.getUniqueId());
        if (entityId != null) removeEntity(entityId);
    }

    private void removeEntity(UUID entityId) {
        Entity entity = plugin.getServer().getEntity(entityId);
        if (entity != null) entity.remove();
    }

    private void load(UUID playerId) {
        if (companions.containsKey(playerId)) return;
        File file = new File(storageFolder, playerId + ".yml");
        if (!file.exists()) {
            companions.put(playerId, new ArrayList<>());
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Companion> loaded = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("companions");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "companions." + id;
                try {
                    CompanionRarity rarity = CompanionRarity.valueOf(yaml.getString(path + ".rarity", "COMMON").toUpperCase());
                    org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(yaml.getString(path + ".entity-type", "WOLF").toUpperCase());
                    loaded.add(new Companion(id,
                            yaml.getString(path + ".name", id),
                            Math.max(1, Math.min(MAX_LEVEL, yaml.getInt(path + ".level", 1))),
                            Math.max(0L, yaml.getLong(path + ".experience", 0L)),
                            rarity,
                            entityType,
                            false));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring invalid companion definition '" + id + "' for " + playerId + ".");
                }
            }
        }
        companions.put(playerId, loaded);
    }

    private void save(UUID playerId, List<Companion> values) {
        File file = new File(storageFolder, playerId + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        for (Companion companion : values) {
            String path = "companions." + companion.id();
            yaml.set(path + ".name", companion.name());
            yaml.set(path + ".level", companion.level());
            yaml.set(path + ".experience", companion.experience());
            yaml.set(path + ".rarity", companion.rarity().name());
            yaml.set(path + ".entity-type", companion.entityType().name());
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save companions for " + playerId + ".", exception);
        }
    }
}
