package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.npc.MannequinSkinResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Facade/orchestrator for companion ownership, persistence, progression and reactive runtime lifecycle. */
public final class CompanionService {
    private final Plugin plugin;
    private final File storageFolder;
    private final CompanionRegistry registry;
    private final CompanionProgression progression;
    private final Map<UUID, List<Companion>> companions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activeEntities = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, CompanionEquipment>> equipment = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, StoredSkin>> storedSkins = new ConcurrentHashMap<>();
    private final CompanionEquipmentStore equipmentStore;
    private final CompanionEquipmentListener equipmentListener;
    private final CompanionMountController mountController = new CompanionMountController();
    private final CompanionFollowTask runtimeTask;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PixelRPG-CompanionIO");
        thread.setDaemon(true);
        return thread;
    });

    public CompanionService(Plugin plugin) {
        this.plugin = plugin;
        this.storageFolder = new File(plugin.getDataFolder(), "companions");
        if (!storageFolder.exists() && !storageFolder.mkdirs()) plugin.getLogger().warning("Unable to create companion storage folder.");
        this.registry = CompanionRegistry.load(plugin);
        this.progression = new CompanionProgression(registry);
        this.equipmentStore = new CompanionEquipmentStore(plugin.getDataFolder(), plugin.getLogger());
        this.equipmentListener = new CompanionEquipmentListener(plugin, this);
        this.runtimeTask = new CompanionFollowTask(plugin, activeEntities, this);
        plugin.getServer().getPluginManager().registerEvents(runtimeTask, plugin);
        plugin.getServer().getPluginManager().registerEvents(equipmentListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new CompanionBossRewardListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CompanionMountListener(this, registry, mountController), plugin);
    }

    public List<String> definitionIds() { return registry.definitions().keySet().stream().sorted().toList(); }
    public List<Companion> getCompanions(UUID playerId) { load(playerId); return List.copyOf(companions.getOrDefault(playerId, List.of())); }
    public CompanionDefinition definition(String id) { return registry.require(id); }
    public void ensureTestWolf(UUID playerId) { load(playerId); }

    /** Returns the shared mount controller used by the reactive companion runtime. */
    public CompanionMountController mountController() { return mountController; }

    /** Wakes one active companion after an external state change. */
    public void wakeRuntime(UUID playerId) { runtimeTask.wakeOwner(playerId); }

    /** Unlocks the companion configured for a defeated boss. */
    public boolean unlockFromBoss(UUID playerId, String bossId) {
        if (bossId == null || bossId.isBlank()) return false;
        return registry.definitions().values().stream().filter(definition -> "BOSS".equalsIgnoreCase(definition.unlock().type()))
                .filter(definition -> definition.unlock().data().has("bossId") && bossId.equalsIgnoreCase(definition.unlock().data().get("bossId").getAsString()))
                .findFirst().map(definition -> grantDefinition(playerId, definition)).orElse(false);
    }

    /** Grants a companion directly through an administrative command. */
    public boolean adminGrant(UUID playerId, String id) {
        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null) return false;
        grantDefinition(playerId, definition);
        return true;
    }

    public boolean unlockDefinition(UUID playerId, String id) {
        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null || definition.adminOnly() || definition.rarity().isUnique()) return false;
        grantDefinition(playerId, definition);
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

    private boolean grantDefinition(UUID playerId, CompanionDefinition definition) {
        load(playerId);
        List<Companion> current = companions.getOrDefault(playerId, List.of());
        if (current.stream().anyMatch(existing -> existing.id().equals(definition.id()))) return false;
        List<Companion> updated = new ArrayList<>(current);
        updated.add(toCompanion(definition));
        companions.put(playerId, updated);
        save(playerId, updated);
        return true;
    }

    /** Grants a Unique companion only when the immutable registry definition itself permits it. */
    public boolean unlockUnique(UUID playerId, String id, String ignoredName, org.bukkit.entity.EntityType ignoredEntityType) {
        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null || !definition.rarity().isUnique() || !definition.adminOnly()) return false;
        grantDefinition(playerId, definition);
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

    public void restoreActive(Player player) {
        UUID playerId = player.getUniqueId();
        load(playerId);
        if (activeEntities.containsKey(playerId)) return;
        Companion active = companions.getOrDefault(playerId, List.of()).stream().filter(Companion::active).findFirst().orElse(null);
        if (active != null) spawnPassiveCompanion(player, active);
    }

    public void despawn(UUID playerId) { UUID entityId = activeEntities.remove(playerId); if (entityId != null) removeEntity(entityId); }

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

    /** Returns the active companion directly from the cached player state without creating a defensive list snapshot. */
    public Companion getActive(UUID playerId) {
        load(playerId);
        for (Companion companion : companions.getOrDefault(playerId, List.of())) {
            if (companion.active()) return companion;
        }
        return null;
    }

    public UUID getActiveEntity(UUID playerId) { return activeEntities.get(playerId); }
    public UUID getOwnerOfEntity(UUID entityId) { for (Map.Entry<UUID, UUID> entry : activeEntities.entrySet()) if (entry.getValue().equals(entityId)) return entry.getKey(); return null; }
    public void openEquipment(Player player, Companion companion) { equipmentListener.open(player, companion); }

    /** Returns cached companion equipment; disk access is restricted to player state initialization. */
    public CompanionEquipment getEquipment(UUID playerId, String companionId) {
        load(playerId);
        Map<String, CompanionEquipment> playerEquipment = equipment.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
        CompanionEquipment cached = playerEquipment.get(companionId);
        if (cached != null) return cached.copy();
        CompanionEquipment loaded = defaultEquipment(companionId);
        equipmentStore.save(playerId, companionId, loaded);
        playerEquipment.put(companionId, loaded.copy());
        return loaded.copy();
    }

    public void setEquipment(UUID playerId, String companionId, CompanionEquipment value) {
        CompanionEquipment copy = value == null ? CompanionEquipment.empty() : value.copy();
        equipment.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(companionId, copy.copy());
        equipmentStore.save(playerId, companionId, copy);
        wakeRuntime(playerId);
    }

    /** Returns the resolved mannequin skin texture persisted for one companion, if available. */
    public StoredSkin getStoredSkin(UUID playerId, String companionId) {
        load(playerId);
        return storedSkins.getOrDefault(playerId, Map.of()).get(companionId);
    }

    /** Persists only the already-resolved mannequin texture; skin acquisition remains unchanged. */
    public void storeSkin(UUID playerId, String companionId, String value, String signature) {
        if (value == null || value.isBlank() || companionId == null || companionId.isBlank()) return;
        storedSkins.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(companionId, new StoredSkin(value, signature));
        plugin.getLogger().info("Companion skin persisted in memory: player=" + playerId
                + ", companion=" + companionId);
        save(playerId, companions.getOrDefault(playerId, List.of()));
    }

    public void applyEquipmentToEntity(UUID playerId, String companionId, LivingEntity entity) {
        CompanionEquipment value = getEquipment(playerId, companionId);
        var target = entity.getEquipment(); if (target == null) return;
        target.setHelmet(value.helmet(), true); target.setChestplate(value.chestplate(), true); target.setLeggings(value.leggings(), true); target.setBoots(value.boots(), true);
        target.setItemInMainHand(value.mainHand(), true); target.setItemInOffHand(value.offHand(), true);
    }

    public boolean rename(UUID playerId, String companionId, String newName) {
        load(playerId); String cleaned = newName == null ? "" : newName.strip();
        if (cleaned.isEmpty() || cleaned.length() > registry.maxNameLength()) return false;
        List<Companion> current = companions.get(playerId); if (current == null) return false;
        Companion selected = current.stream().filter(companion -> companion.id().equals(companionId)).findFirst().orElse(null);
        CompanionDefinition definition = registry.find(companionId).orElse(null);
        if (selected == null || definition == null || !definition.renameable()) return false;
        List<Companion> updated = current.stream().map(companion -> companion.id().equals(companionId) ? companion.withName(cleaned) : companion).toList();
        companions.put(playerId, updated); save(playerId, updated);
        UUID entityId = activeEntities.get(playerId); if (entityId != null) { Entity entity = plugin.getServer().getEntity(entityId); if (entity instanceof LivingEntity living) living.customName(Component.text(cleaned)); }
        wakeRuntime(playerId);
        return true;
    }

    /** Awards XP through the centralized progression engine to the active companion only. */
    public boolean awardExperience(UUID playerId, long baseExperience) {
        if (baseExperience <= 0L) return false; load(playerId); List<Companion> current = companions.get(playerId); if (current == null) return false;
        Companion active = current.stream().filter(Companion::active).findFirst().orElse(null); if (active == null) return false;
        CompanionDefinition definition = registry.find(active.id()).orElse(null); if (definition == null) return false;
        CompanionInstance instance = new CompanionInstance(playerId, active.id(), active.level(), active.experience(), true, true, getEquipment(playerId, active.id()));
        CompanionInstance progressed = progression.addExperience(definition, instance, baseExperience); if (progressed.experience() == active.experience()) return false;
        List<Companion> updated = current.stream().map(companion -> companion.id().equals(active.id()) ? companion.withProgress(progressed.level(), progressed.experience()) : companion).toList();
        companions.put(playerId, updated); save(playerId, updated); refreshActiveMetadata(playerId, updated.stream().filter(Companion::active).findFirst().orElse(null));
        wakeRuntime(playerId);
        return progressed.level() != active.level();
    }

    public int calculateLevel(long experience) { CompanionDefinition definition = registry.definitions().values().stream().findFirst().orElse(null); return definition == null ? 1 : progression.levelForExperience(definition, experience); }
    public long experienceToNextLevel(int level) { CompanionDefinition definition = registry.definitions().values().stream().findFirst().orElse(null); return definition == null ? Long.MAX_VALUE : progression.experienceToNextLevel(definition, level); }
    public long experienceWithinLevel(Companion companion) { CompanionDefinition definition = registry.find(companion.id()).orElse(null); if (definition == null) return 0L; CompanionInstance instance = new CompanionInstance(UUID.randomUUID(), companion.id(), companion.level(), companion.experience(), true, companion.active(), CompanionEquipment.empty()); return progression.experienceWithinLevel(definition, instance); }
    public long experienceNeededForCurrentLevel(Companion companion) { CompanionDefinition definition = registry.find(companion.id()).orElse(null); return definition == null ? Long.MAX_VALUE : progression.experienceToNextLevel(definition, companion.level()); }

    public void shutdown() {
        runtimeTask.shutdown();
        for (UUID entityId : activeEntities.values()) removeEntity(entityId);
        activeEntities.clear();
        for (Map.Entry<UUID, List<Companion>> entry : companions.entrySet()) save(entry.getKey(), entry.getValue());
        companions.clear();
        equipment.clear();
        storedSkins.clear();
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) { plugin.getLogger().warning("Companion I/O did not finish within 10 seconds; forcing shutdown."); ioExecutor.shutdownNow(); }
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); ioExecutor.shutdownNow(); }
        equipmentStore.close();
    }

    private Companion toCompanion(CompanionDefinition definition) { return new Companion(definition.id(), definition.displayName(), 1, 0L, definition.rarity(), definition.visual().entityType(), false); }

    private void spawnPassiveCompanion(Player player, Companion selected) {
        try {
            Location spawnLocation = player.getLocation().clone().add(1.0D, 0.0D, 1.0D); Entity entity = player.getWorld().spawnEntity(spawnLocation, selected.entityType());
            if (!(entity instanceof LivingEntity living)) { entity.remove(); return; }
            living.getPersistentDataContainer().set(RPGKeys.Companion.id(), PersistentDataType.STRING, selected.id()); living.getPersistentDataContainer().set(RPGKeys.Companion.level(), PersistentDataType.INTEGER, selected.level()); living.getPersistentDataContainer().set(RPGKeys.Companion.rarity(), PersistentDataType.STRING, selected.rarity().name());
            living.customName(Component.text(selected.name())); living.setCustomNameVisible(true);

            // Unique mannequin companions may define a fixed player name or external skin URL.
            // Keep this path independent from the generic NPC manager.
            CompanionDefinition definition = registry.require(selected.id());
            if (living instanceof Mannequin mannequin) {
                CompanionService.StoredSkin storedSkin = getStoredSkin(player.getUniqueId(), selected.id());
                if (storedSkin != null) {
                    plugin.getLogger().info("Applying persisted companion skin: player=" + player.getUniqueId()
                            + ", companion=" + selected.id());
                    // A persisted resolved texture is authoritative after restart/reload.
                    MannequinSkinResolver.applyStoredTexture(
                            mannequin,
                            storedSkin.value(),
                            storedSkin.signature(),
                            plugin
                    );
                } else {
                    String skinSource = definition.visual().skinSource();
                    if (skinSource != null && !skinSource.isBlank()) {
                        plugin.getLogger().info("Resolving companion skin: player=" + player.getUniqueId()
                                + ", companion=" + selected.id() + ", source=" + skinSource);
                        // Capture the resolved texture property immediately so the
                        // exact skin can be restored without resolving it again after restart.
                        MannequinSkinResolver.applyAndCapture(mannequin, skinSource, plugin.getLogger())
                                .thenAccept(texture -> storeSkin(
                                        player.getUniqueId(),
                                        selected.id(),
                                        texture.getValue(),
                                        texture.getSignature()));
                    }
                }
            }

            if (living instanceof Mob mob) { mob.setAware(true); mob.setTarget(null); }
            applyEquipmentToEntity(player.getUniqueId(), selected.id(), living);
            mountController.prepare(living, definition.mount(), player);
            activeEntities.put(player.getUniqueId(), entity.getUniqueId());
            wakeRuntime(player.getUniqueId());
        } catch (RuntimeException exception) { plugin.getLogger().warning("Unable to spawn companion '" + selected.id() + "': " + exception.getMessage()); }
    }

    private void refreshActiveMetadata(UUID playerId, Companion active) { if (active == null) return; UUID entityId = activeEntities.get(playerId); if (entityId == null) return; Entity entity = plugin.getServer().getEntity(entityId); if (entity == null) return; entity.getPersistentDataContainer().set(RPGKeys.Companion.level(), PersistentDataType.INTEGER, active.level()); entity.getPersistentDataContainer().set(RPGKeys.Companion.rarity(), PersistentDataType.STRING, active.rarity().name()); }
    private CompanionEquipment defaultEquipment(String id) { CompanionDefinition definition = registry.find(id).orElse(null); if (definition == null || !definition.equipment().enabled()) return CompanionEquipment.empty(); CompanionDefinition.CompanionEquipmentDefaults configured = definition.equipment(); return new CompanionEquipment(item(configured.helmet()), item(configured.chestplate()), item(configured.leggings()), item(configured.boots()), item(configured.mainHand()), item(configured.offHand())); }
    private static ItemStack item(String material) { if (material == null || material.isBlank()) return null; Material matched = Material.matchMaterial(material); return matched == null || matched.isAir() ? null : new ItemStack(matched); }
    private void clearActiveEntity(Player player) { UUID entityId = activeEntities.remove(player.getUniqueId()); if (entityId != null) removeEntity(entityId); }
    private void removeEntity(UUID entityId) { Entity entity = plugin.getServer().getEntity(entityId); if (entity != null) entity.remove(); }

    private void load(UUID playerId) {
        if (companions.containsKey(playerId)) return;
        File file = new File(storageFolder, playerId + ".yml");
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        List<Companion> loaded = new ArrayList<>();
        if (file.exists()) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section == null) continue;
                CompanionDefinition definition = registry.find(section.getString("id", key)).orElse(null);
                if (definition == null) continue;
                loaded.add(new Companion(
                        definition.id(),
                        section.getString("name", definition.displayName()),
                        Math.max(1, section.getInt("level", 1)),
                        Math.max(0L, section.getLong("experience", 0L)),
                        definition.rarity(),
                        definition.visual().entityType(),
                        section.getBoolean("active", false)
                ));
            }
        }
        companions.put(playerId, loaded);
        equipment.put(playerId, new ConcurrentHashMap<>(equipmentStore.loadPlayer(playerId)));
        Map<String, StoredSkin> loadedSkins = new ConcurrentHashMap<>();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            String value = section.getString("skin.value", "");
            if (value.isBlank()) continue;
            String signature = section.getString("skin.signature");
            loadedSkins.put(key, new StoredSkin(value, signature));
        }
        storedSkins.put(playerId, loadedSkins);
    }

    private void save(UUID playerId, List<Companion> values) {
        File file = new File(storageFolder, playerId + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Companion companion : values) {
            ConfigurationSection section = config.createSection(companion.id());
            section.set("id", companion.id());
            section.set("name", companion.name());
            section.set("level", companion.level());
            section.set("experience", companion.experience());
            section.set("active", companion.active());
            StoredSkin skin = storedSkins.getOrDefault(playerId, Map.of()).get(companion.id());
            if (skin != null) {
                section.set("skin.value", skin.value());
                if (skin.signature() != null && !skin.signature().isBlank()) section.set("skin.signature", skin.signature());
            }
        }
        ioExecutor.execute(() -> {
            try {
                config.save(file);
            } catch (IOException exception) {
                plugin.getLogger().warning("Unable to save companions for " + playerId + ": " + exception.getMessage());
            }
        });
    }
    /** Cached resolved mannequin texture persisted independently of the configured skin source. */
    public record StoredSkin(String value, String signature) {
    }
}
