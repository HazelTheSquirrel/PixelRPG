package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

/** Facade/orchestrator for companion ownership, persistence, progression and runtime lifecycle. */
public final class CompanionService {
    private final Plugin plugin;
    private final File storageFolder;
    private final CompanionRegistry registry;
    private final CompanionProgression progression;
    private final Map<UUID, List<Companion>> companions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activeEntities = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, CompanionEquipment>> equipment = new ConcurrentHashMap<>();
    private final CompanionEquipmentStore equipmentStore;
    private final CompanionEquipmentListener equipmentListener;
    private final BukkitTask followTask;

    public CompanionService(Plugin plugin) {
        this.plugin = plugin;
        this.storageFolder = new File(plugin.getDataFolder(), "companions");
        if (!storageFolder.exists() && !storageFolder.mkdirs()) plugin.getLogger().warning("Unable to create companion storage folder.");
        this.registry = CompanionRegistry.load(plugin);
        this.progression = new CompanionProgression(registry);
        this.equipmentStore = new CompanionEquipmentStore(plugin.getDataFolder(), plugin.getLogger());
        this.equipmentListener = new CompanionEquipmentListener(plugin, this);
        this.followTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new CompanionFollowTask(plugin, activeEntities, this), 1L, 2L);
        plugin.getServer().getPluginManager().registerEvents(equipmentListener, plugin);
    }

    public List<String> definitionIds() { return registry.definitions().keySet().stream().sorted().toList(); }
    public List<Companion> getCompanions(UUID playerId) { load(playerId); return List.copyOf(companions.getOrDefault(playerId, List.of())); }
    public CompanionDefinition definition(String id) { return registry.require(id); }
    public void ensureTestWolf(UUID playerId) { unlockDefinition(playerId, "test-wolf"); }

    public boolean unlockDefinition(UUID playerId, String id) {
        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null || definition.adminOnly() || definition.rarity().isUnique()) return false;
        unlock(playerId, toCompanion(definition));
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

    /** Grants a Unique companion only when the immutable registry definition itself permits it. */
    public boolean unlockUnique(UUID playerId, String id, String ignoredName, org.bukkit.entity.EntityType ignoredEntityType) {
        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null || !definition.rarity().isUnique() || !definition.adminOnly()) return false;
        unlock(playerId, toCompanion(definition));
        return true;
    }

    public boolean setActive(Player player, String companionId) {
        UUID playerId = player.getUniqueId();
        load(playerId);
        List<Companion> current = companions.get(playerId);
        if (current == null || current.stream().noneMatch(companion -> companion.id().equals(companionId))) return false;
        clearActiveEntity(player);
        List<Companion> updated = current.stream().map(companion -> companion.withActive(companion.id().equals(companionId))).toList();
        companions.put(playerId, updated);
        save(playerId, updated);
        Companion selected = updated.stream().filter(companion -> companion.id().equals(companionId)).findFirst().orElseThrow();
        spawnPassiveCompanion(player, selected);
        return true;
    }

    public boolean setActive(UUID playerId, String companionId) { Player player = plugin.getServer().getPlayer(playerId); return player != null && setActive(player, companionId); }

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
        if (player != null) { clearActive(player); return; }
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

    public Companion getActive(UUID playerId) { return getCompanions(playerId).stream().filter(Companion::active).findFirst().orElse(null); }
    public UUID getActiveEntity(UUID playerId) { return activeEntities.get(playerId); }

    public UUID getOwnerOfEntity(UUID entityId) {
        for (Map.Entry<UUID, UUID> entry : activeEntities.entrySet()) if (entry.getValue().equals(entityId)) return entry.getKey();
        return null;
    }

    public void openEquipment(Player player, Companion companion) { equipmentListener.open(player, companion); }

    public CompanionEquipment getEquipment(UUID playerId, String companionId) {
        load(playerId);
        Map<String, CompanionEquipment> playerEquipment = equipment.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
        CompanionEquipment cached = playerEquipment.get(companionId);
        if (cached != null) return cached.copy();
        CompanionEquipment loaded = equipmentStore.hasEntry(playerId, companionId) ? equipmentStore.load(playerId, companionId) : defaultEquipment(companionId);
        if (!equipmentStore.hasEntry(playerId, companionId)) equipmentStore.save(playerId, companionId, loaded);
        playerEquipment.put(companionId, loaded.copy());
        return loaded.copy();
    }

    public void setEquipment(UUID playerId, String companionId, CompanionEquipment value) {
        CompanionEquipment copy = value == null ? CompanionEquipment.empty() : value.copy();
        equipment.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(companionId, copy.copy());
        equipmentStore.save(playerId, companionId, copy);
    }

    public void applyEquipmentToEntity(UUID playerId, String companionId, LivingEntity entity) {
        CompanionEquipment value = getEquipment(playerId, companionId);
        var target = entity.getEquipment();
        if (target == null) return;
        target.setHelmet(value.helmet(), true);
        target.setChestplate(value.chestplate(), true);
        target.setLeggings(value.leggings(), true);
        target.setBoots(value.boots(), true);
        target.setItemInMainHand(value.mainHand(), true);
        target.setItemInOffHand(value.offHand(), true);
    }

    public boolean rename(UUID playerId, String companionId, String newName) {
        load(playerId);
        String cleaned = newName == null ? "" : newName.strip();
        if (cleaned.isEmpty() || cleaned.length() > registry.maxNameLength()) return false;
        List<Companion> current = companions.get(playerId);
        if (current == null) return false;
        Companion selected = current.stream().filter(companion -> companion.id().equals(companionId)).findFirst().orElse(null);
        CompanionDefinition definition = registry.find(companionId).orElse(null);
        if (selected == null || definition == null || !definition.renameable()) return false;
        List<Companion> updated = current.stream().map(companion -> companion.id().equals(companionId) ? companion.withName(cleaned) : companion).toList();
        companions.put(playerId, updated);
        save(playerId, updated);
        UUID entityId = activeEntities.get(playerId);
        if (entityId != null) {
            Entity entity = plugin.getServer().getEntity(entityId);
            if (entity instanceof LivingEntity living) living.customName(Component.text(cleaned));
        }
        return true;
    }

    /** Awards XP through the centralized progression engine to the active companion only. */
    public boolean awardExperience(UUID playerId, long baseExperience) {
        if (baseExperience <= 0L) return false;
        load(playerId);
        List<Companion> current = companions.get(playerId);
        if (current == null) return false;
        Companion active = current.stream().filter(Companion::active).findFirst().orElse(null);
        if (active == null) return false;
        CompanionDefinition definition = registry.find(active.id()).orElse(null);
        if (definition == null) return false;
        CompanionInstance instance = new CompanionInstance(playerId, active.id(), active.level(), active.experience(), true, true, getEquipment(playerId, active.id()));
        CompanionInstance progressed = progression.addExperience(definition, instance, baseExperience);
        if (progressed.experience() == active.experience()) return false;
        List<Companion> updated = current.stream().map(companion -> companion.id().equals(active.id()) ? companion.withProgress(progressed.level(), progressed.experience()) : companion).toList();
        companions.put(playerId, updated);
        save(playerId, updated);
        refreshActiveMetadata(playerId, updated.stream().filter(Companion::active).findFirst().orElse(null));
        return progressed.level() != active.level();
    }

    public int calculateLevel(long experience) {
        CompanionDefinition definition = registry.definitions().values().stream().findFirst().orElse(null);
        return definition == null ? 1 : progression.levelForExperience(definition, experience);
    }

    public long experienceToNextLevel(int level) {
        CompanionDefinition definition = registry.definitions().values().stream().findFirst().orElse(null);
        return definition == null ? Long.MAX_VALUE : progression.experienceToNextLevel(definition, level);
    }

    public long experienceWithinLevel(Companion companion) {
        CompanionDefinition definition = registry.find(companion.id()).orElse(null);
        if (definition == null) return 0L;
        CompanionInstance instance = new CompanionInstance(UUID.randomUUID(), companion.id(), companion.level(), companion.experience(), true, companion.active(), CompanionEquipment.empty());
        return progression.experienceWithinLevel(definition, instance);
    }

    public long experienceNeededForCurrentLevel(Companion companion) {
        CompanionDefinition definition = registry.find(companion.id()).orElse(null);
        return definition == null ? Long.MAX_VALUE : progression.experienceToNextLevel(definition, companion.level());
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
        equipment.clear();
    }

    private Companion toCompanion(CompanionDefinition definition) { return new Companion(definition.id(), definition.displayName(), 1, 0L, definition.rarity(), definition.visual().entityType(), false); }

    private void spawnPassiveCompanion(Player player, Companion selected) {
        try {
            Location spawnLocation = player.getLocation().clone().add(1.0D, 0.0D, 1.0D);
            Entity entity = player.getWorld().spawnEntity(spawnLocation, selected.entityType());
            if (!(entity instanceof LivingEntity living)) { entity.remove(); return; }
            living.getPersistentDataContainer().set(RPGKeys.Companion.id(), PersistentDataType.STRING, selected.id());
            living.getPersistentDataContainer().set(RPGKeys.Companion.level(), PersistentDataType.INTEGER, selected.level());
            living.getPersistentDataContainer().set(RPGKeys.Companion.rarity(), PersistentDataType.STRING, selected.rarity().name());
            living.customName(Component.text(selected.name()));
            living.setCustomNameVisible(true);
            if (living instanceof Mob mob) { mob.setAware(true); mob.setTarget(null); }
            applyEquipmentToEntity(player.getUniqueId(), selected.id(), living);
            activeEntities.put(player.getUniqueId(), entity.getUniqueId());
        } catch (RuntimeException exception) { plugin.getLogger().warning("Unable to spawn companion '" + selected.id() + "': " + exception.getMessage()); }
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

    private CompanionEquipment defaultEquipment(String id) {
        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null || !definition.equipment().enabled()) return CompanionEquipment.empty();
        CompanionDefinition.CompanionEquipmentDefaults configured = definition.equipment();
        return new CompanionEquipment(item(configured.helmet()), item(configured.chestplate()), item(configured.leggings()), item(configured.boots()), item(configured.mainHand()), item(configured.offHand()));
    }

    private static ItemStack item(String material) {
        if (material == null || material.isBlank()) return null;
        Material matched = Material.matchMaterial(material);
        return matched == null || matched.isAir() ? null : new ItemStack(matched);
    }

    private void clearActiveEntity(Player player) { UUID entityId = activeEntities.remove(player.getUniqueId()); if (entityId != null) removeEntity(entityId); }
    private void removeEntity(UUID entityId) { Entity entity = plugin.getServer().getEntity(entityId); if (entity != null) entity.remove(); }

    private void load(UUID playerId) {
        if (companions.containsKey(playerId)) return;
        File file = new File(storageFolder, playerId + ".yml");
        if (!file.exists()) { companions.put(playerId, new ArrayList<>()); return; }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Companion> loaded = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("companions");
        if (section != null) for (String id : section.getKeys(false)) {
            String path = "companions." + id;
            try {
                CompanionDefinition definition = registry.require(id);
                int level = Math.max(1, Math.min(registry.maxLevel(), yaml.getInt(path + ".level", 1)));
                long experience = Math.max(0L, yaml.getLong(path + ".experience", 0L));
                String name = yaml.getString(path + ".name", definition.displayName());
                loaded.add(new Companion(id, name, level, experience, definition.rarity(), definition.visual().entityType(), false));
            } catch (IllegalArgumentException exception) { plugin.getLogger().warning("Ignoring invalid companion '" + id + "' for " + playerId + "."); }
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
        }
        try { yaml.save(file); }
        catch (IOException exception) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to save companions for " + playerId, exception); }
    }
}
