package de.pixelrpg.rpg.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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

    private final Plugin plugin;
    private final File storageFolder;
    private final Map<UUID, List<Companion>> companions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activeEntities = new ConcurrentHashMap<>();
    private final BukkitTask followTask;

    private int maxLevel = 99;
    private long experienceBase = 100L;
    private long experiencePerLevel = 50L;
    private int maxNameLength = 24;
    private boolean activeXpOnly = true;

    public CompanionService(Plugin plugin) {
        this.plugin = plugin;
        this.storageFolder = new File(plugin.getDataFolder(), "companions");
        if (!storageFolder.exists()) storageFolder.mkdirs();
        loadJsonConfiguration();
        this.followTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                new CompanionFollowTask(plugin, activeEntities), 1L, 5L);
    }

    /** Loads user-editable companion progression and definitions from data/companions.json. */
    private void loadJsonConfiguration() {
        try {
            JsonObject root = new JsonDataManager(plugin).load("companions.json");
            JsonObject progression = object(root, "progression");
            if (progression != null) {
                maxLevel = Math.max(1, Math.min(99, number(progression, "maxLevel", maxLevel)));
                JsonObject xp = object(progression, "xp");
                if (xp != null) {
                    experienceBase = Math.max(1L, numberLong(xp, "base", experienceBase));
                    experiencePerLevel = Math.max(0L, numberLong(xp, "perLevel", experiencePerLevel));
                }
            }
            JsonObject defaults = object(root, "defaults");
            if (defaults != null) {
                activeXpOnly = bool(defaults, "activeXpOnly", activeXpOnly);
                maxNameLength = Math.max(1, number(defaults, "maxNameLength", maxNameLength));
            }
            validateDefinitions(root);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load companions.json; using safe companion defaults: " + exception.getMessage());
        }
    }

    public List<Companion> getCompanions(UUID playerId) {
        load(playerId);
        return List.copyOf(companions.getOrDefault(playerId, List.of()));
    }

    /** Adds the configured test wolf used for the companion-system test. */
    public void ensureTestWolf(UUID playerId) {
        unlockDefinition(playerId, TEST_WOLF_ID);
    }

    /** Unlocks a player-accessible companion definition. Admin-only and Unique definitions are excluded. */
    public boolean unlockDefinition(UUID playerId, String id) {
        JsonObject definition = readDefinitionJson(id);
        if (definition == null || bool(definition, "adminOnly", false)) return false;
        if ("UNIQUE".equalsIgnoreCase(string(definition, "rarity", ""))) return false;
        Companion companion = toCompanion(id, definition);
        if (companion == null) return false;
        unlock(playerId, companion);
        return true;
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
    public boolean unlockUnique(UUID playerId, String id, String fixedName, org.bukkit.entity.EntityType entityType) {
        if (id == null || id.isBlank() || fixedName == null || fixedName.isBlank() || fixedName.length() > maxNameLength
                || entityType == null || !entityType.isSpawnable()) return false;
        JsonObject configured = readDefinitionJson(id);
        if (configured != null) {
            String rarity = string(configured, "rarity", "").toUpperCase();
            if (!"UNIQUE".equals(rarity) || !bool(configured, "adminOnly", false) || bool(configured, "renameable", true)) return false;
        }
        unlock(playerId, new Companion(id, fixedName.strip(), 1, 0L, CompanionRarity.UNIQUE, entityType, false));
        return true;
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
        spawnPassiveCompanion(player, selected);
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
        if (cleaned.isEmpty() || cleaned.length() > maxNameLength) return false;
        List<Companion> current = companions.get(playerId);
        if (current == null) return false;
        Companion selected = current.stream().filter(companion -> companion.id().equals(companionId)).findFirst().orElse(null);
        if (selected == null || selected.rarity().isUnique() || !isRenameable(companionId)) return false;

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
        if (active == null || active.level() >= maxLevel) return false;

        long gained = Math.max(1L, Math.round(baseExperience));
        long experience = active.experience() + gained;
        int level = Math.min(maxLevel, calculateLevel(experience));
        List<Companion> updated = current.stream()
                .map(companion -> companion.id().equals(active.id()) ? companion.withProgress(level, experience) : companion)
                .toList();
        companions.put(playerId, updated);
        save(playerId, updated);
        refreshActiveMetadata(playerId, updated.stream().filter(Companion::active).findFirst().orElse(null));
        return level != active.level();
    }

    public int calculateLevel(long experience) {
        long remaining = Math.max(0L, experience);
        int level = 1;
        while (level < maxLevel) {
            long required = experienceToNextLevel(level);
            if (remaining < required) break;
            remaining -= required;
            level++;
        }
        return level;
    }

    public long experienceToNextLevel(int level) {
        if (level >= maxLevel) return Long.MAX_VALUE;
        return experienceBase + (long) level * experiencePerLevel;
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
        followTask.cancel();
        for (UUID entityId : activeEntities.values()) removeEntity(entityId);
        activeEntities.clear();
        for (UUID playerId : companions.keySet()) {
            List<Companion> current = companions.get(playerId);
            if (current != null) save(playerId, current.stream().map(companion -> companion.withActive(false)).toList());
        }
        companions.clear();
    }

    private void spawnPassiveCompanion(Player player, Companion selected) {
        try {
            Location spawnLocation = player.getLocation().clone().add(1.0, 0.0, 1.0);
            Entity entity = player.getWorld().spawnEntity(spawnLocation, selected.entityType());
            if (!(entity instanceof LivingEntity living)) {
                entity.remove();
                return;
            }

            living.getPersistentDataContainer().set(RPGKeys.Companion.id(), PersistentDataType.STRING, selected.id());
            living.getPersistentDataContainer().set(RPGKeys.Companion.level(), PersistentDataType.INTEGER, selected.level());
            living.getPersistentDataContainer().set(RPGKeys.Companion.rarity(), PersistentDataType.STRING, selected.rarity().name());
            living.setCustomName(selected.name());
            living.setCustomNameVisible(true);

            // Phase 1 companions are visual/passive only; combat AI is intentionally not enabled yet.
            if (living instanceof Mob mob) {
                mob.setAware(false);
                mob.setTarget(null);
            }

            activeEntities.put(player.getUniqueId(), entity.getUniqueId());
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to spawn companion '" + selected.id() + "' as "
                    + selected.entityType() + ": " + exception.getMessage());
        }
    }

    private void refreshActiveMetadata(UUID playerId, Companion active) {
        if (active == null) return;
        UUID entityId = activeEntities.get(playerId);
        if (entityId == null) return;
        Entity entity = plugin.getServer().getEntity(entityId);
        if (entity == null) return;
        entity.getPersistentDataContainer().set(RPGKeys.Companion.level(), PersistentDataType.INTEGER, active.level());
        entity.getPersistentDataContainer().set(RPGKeys.Companion.rarity(), PersistentDataType.STRING, active.rarity().name());
    }

    private JsonObject readDefinitionJson(String id) {
        try {
            JsonObject root = new JsonDataManager(plugin).load("companions.json");
            JsonArray definitions = root.getAsJsonArray("definitions");
            if (definitions == null) return null;
            for (var element : definitions) {
                if (!element.isJsonObject()) continue;
                JsonObject json = element.getAsJsonObject();
                if (id.equals(string(json, "id", ""))) return json;
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to read companion definition '" + id + "': " + exception.getMessage());
        }
        return null;
    }

    private Companion toCompanion(String id, JsonObject json) {
        try {
            CompanionRarity rarity = CompanionRarity.valueOf(string(json, "rarity", "COMMON").toUpperCase());
            org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(string(json, "entityType", "WOLF").toUpperCase());
            if (!entityType.isSpawnable()) throw new IllegalArgumentException("EntityType is not spawnable");
            return new Companion(id, string(json, "name", id), 1, 0L, rarity, entityType, false);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Ignoring invalid companion definition '" + id + "'.");
            return null;
        }
    }

    private boolean isRenameable(String id) {
        JsonObject json = readDefinitionJson(id);
        return json != null && bool(json, "renameable", true);
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
                            Math.max(1, Math.min(maxLevel, yaml.getInt(path + ".level", 1))),
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
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to save companions for " + playerId, exception);
        }
    }

    private void validateDefinitions(JsonObject root) {
        JsonArray definitions = root.getAsJsonArray("definitions");
        if (definitions == null) {
            plugin.getLogger().warning("companions.json contains no definitions array.");
            return;
        }
        for (var element : definitions) {
            if (!element.isJsonObject()) {
                plugin.getLogger().warning("Ignoring non-object companion definition.");
                continue;
            }
            JsonObject json = element.getAsJsonObject();
            String id = string(json, "id", "").strip();
            String entityName = string(json, "entityType", "").toUpperCase();
            String rarityName = string(json, "rarity", "").toUpperCase();
            if (id.isBlank()) {
                plugin.getLogger().warning("Ignoring companion definition without an id.");
                continue;
            }
            try {
                CompanionRarity rarity = CompanionRarity.valueOf(rarityName);
                org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(entityName);
                if (!entityType.isAlive() || !entityType.isSpawnable()) throw new IllegalArgumentException("entity type is not a spawnable living entity");
                if (rarity.isUnique() && (!bool(json, "adminOnly", false) || bool(json, "renameable", true))) {
                    plugin.getLogger().warning("Invalid UNIQUE companion '" + id + "': it must be adminOnly and not renameable.");
                }
                double scale = json.has("scale") && json.get("scale").isNumber() ? json.get("scale").getAsDouble() : 1.0D;
                if (!Double.isFinite(scale) || scale <= 0.0D) plugin.getLogger().warning("Invalid scale for companion '" + id + "'.");
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid companion definition '" + id + "': " + exception.getMessage());
            }
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null;
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }

    private static int number(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) && object.get(key).isNumber() ? object.get(key).getAsInt() : fallback;
    }

    private static long numberLong(JsonObject object, String key, long fallback) {
        return object != null && object.has(key) && object.get(key).isNumber() ? object.get(key).getAsLong() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object != null && object.has(key) && object.get(key).isBoolean() ? object.get(key).getAsBoolean() : fallback;
    }
}
