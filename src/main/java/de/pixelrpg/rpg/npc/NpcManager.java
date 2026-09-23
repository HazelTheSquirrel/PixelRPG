package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.profession.Profession;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class NpcManager implements AutoCloseable {
    private final Plugin plugin;
    private final File file;
    private final Map<String, RPGNpc> npcsById = new ConcurrentHashMap<>();
    private final Map<String, UUID> spawnedEntityByNpcId = new ConcurrentHashMap<>();
    private final Map<UUID, String> entityToId = new ConcurrentHashMap<>();
    private final Map<NpcChunkKey, List<String>> npcChunkIndex = new ConcurrentHashMap<>();
    private final Map<String, StoredSkin> resolvedSkinsByNpcId = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ExecutorService persistenceExecutor;
    private CompletableFuture<Void> persistenceChain = CompletableFuture.completedFuture(null);
    private volatile boolean shuttingDown;

    public NpcManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
        this.persistenceExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-NpcIO");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void loadAll() {
        shuttingDown = false;
        despawnAllTracked();
        npcsById.clear();
        npcChunkIndex.clear();
        resolvedSkinsByNpcId.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextId.set(yaml.getInt("next-id", 1));
        ConfigurationSection root = yaml.getConfigurationSection("npcs");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;

            NpcType type = parseType(section.getString("type"));
            if (type == null) continue;
            String name = section.getString("name", "NPC");
            World world = section.getString("world") == null ? null : Bukkit.getWorld(section.getString("world"));
            if (world == null) continue;

            Location location = new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                    (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
            String skinSource = section.getString("skin-source", null);
            String skinValue = section.getString("skin-value", null);
            String skinSignature = section.getString("skin-signature", null);
            if (skinValue != null && !skinValue.isBlank()) {
                resolvedSkinsByNpcId.put(id, new StoredSkin(skinValue, skinSignature));
                plugin.getLogger().info("NPC skin loaded from disk: npc=" + id
                        + ", signature=" + (skinSignature == null || skinSignature.isBlank() ? "none" : "present"));
            } else if (skinSource != null && !skinSource.isBlank()) {
                plugin.getLogger().info("NPC skin has no persisted texture yet: npc=" + id
                        + ", source=" + skinSource);
            }
            Profession profession = parseProfession(section.getString("profession"));
            if (profession == null) profession = professionFor(type);

            RPGNpc npc = new RPGNpc(id, type, name, location, skinSource, profession);
            npcsById.put(id, npc);
            addToChunkIndex(npc);
            if (world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) spawnEntityFor(npc);
        }
    }

    public RPGNpc create(NpcType type, String name, Location location, String skinSource) {
        return create(type, name, location, skinSource, null);
    }

    public RPGNpc create(NpcType type, String name, Location location, String skinSource, Profession profession) {
        String id = String.valueOf(nextId.getAndIncrement());
        return createWithId(id, type, name, location, skinSource, profession);
    }

    /** Creates an NPC with a stable administrator-defined id for data-driven quest targets. */
    public synchronized RPGNpc createWithId(String id, NpcType type, String name, Location location, String skinSource, Profession profession) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("NPC id must not be blank");
        if (npcsById.containsKey(id)) throw new IllegalArgumentException("NPC id already exists: " + id);
        if (type == null) throw new IllegalArgumentException("NPC type must not be null");
        if (location == null || location.getWorld() == null) throw new IllegalArgumentException("NPC location must have a world");
        Profession effectiveProfession = profession == null ? professionFor(type) : profession;
        RPGNpc npc = new RPGNpc(id, type, name == null || name.isBlank() ? "NPC" : name, location.clone(), skinSource, effectiveProfession);
        npcsById.put(id, npc);
        addToChunkIndex(npc);
        spawnEntityFor(npc);
        saveAll();
        return npc;
    }

    public void spawnEntityFor(RPGNpc npc) {
        UUID trackedUuid = spawnedEntityByNpcId.get(npc.id());
        if (trackedUuid != null) {
            Entity trackedEntity = Bukkit.getEntity(trackedUuid);
            if (trackedEntity instanceof Mannequin mannequin && trackedEntity.isValid()) return;

            spawnedEntityByNpcId.remove(npc.id(), trackedUuid);
            entityToId.remove(trackedUuid, npc.id());
        }

        Mannequin mannequin = npc.location().getWorld().spawn(npc.location(), Mannequin.class, entity -> {
            entity.setAI(false);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.setRemoveWhenFarAway(false);
            entity.setCollidable(false);
            entity.customName(Component.text(npc.name(), npc.type().getColor()));
            entity.setCustomNameVisible(false);
            entity.getPersistentDataContainer().set(RPGKeys.Npc.npcType(), PersistentDataType.STRING, npc.type().name());
            entity.getPersistentDataContainer().set(RPGKeys.Npc.npcId(), PersistentDataType.STRING, npc.id());
            StoredSkin storedSkin = resolvedSkinsByNpcId.get(npc.id());
            if (storedSkin != null) {
                plugin.getLogger().info("Applying persisted NPC skin: npc=" + npc.id());
                MannequinSkinResolver.applyStoredTexture(entity, storedSkin.value(), storedSkin.signature(), plugin)
                        .whenComplete((ignored, exception) -> {
                            if (exception != null) {
                                plugin.getLogger().warning("Failed to apply persisted NPC skin: npc="
                                        + npc.id() + ": " + exception.getMessage());
                            } else {
                                plugin.getLogger().info("Persisted NPC skin applied: npc=" + npc.id());
                            }
                        });
            } else if (npc.hasCustomSkin()) {
                plugin.getLogger().info("Resolving NPC skin: npc=" + npc.id()
                        + ", source=" + npc.skinSource());
                MannequinSkinResolver.applyAndCapture(entity, npc.skinSource(), plugin.getLogger())
                        .thenAccept(texture -> {
                            plugin.getLogger().info("NPC skin resolved: npc=" + npc.id()
                                    + ", texture=" + abbreviateTexture(texture.getValue()));
                            rememberResolvedSkin(npc.id(), texture.getValue(), texture.getSignature());
                        })
                        .exceptionally(exception -> {
                            plugin.getLogger().warning("NPC skin resolution failed: npc=" + npc.id()
                                    + ": " + exception.getMessage());
                            return null;
                        });
            }
        });

        spawnedEntityByNpcId.put(npc.id(), mannequin.getUniqueId());
        entityToId.put(mannequin.getUniqueId(), npc.id());
    }

    /** Re-synchronizes NPC entities for a player after login without requiring a server restart. */
    public void resyncPlayer(Player player) {
        for (RPGNpc npc : npcsById.values()) {
            Location location = npc.location();
            if (location.getWorld() != player.getWorld()) continue;
            if (!location.getChunk().isLoaded()) continue;

            UUID entityUuid = spawnedEntityByNpcId.get(npc.id());
            Entity entity = entityUuid == null ? null : Bukkit.getEntity(entityUuid);

            if (!(entity instanceof Mannequin) || !entity.isValid()) {
                if (entityUuid != null) {
                    spawnedEntityByNpcId.remove(npc.id(), entityUuid);
                    entityToId.remove(entityUuid, npc.id());
                }
                spawnEntityFor(npc);
                entityUuid = spawnedEntityByNpcId.get(npc.id());
                entity = entityUuid == null ? null : Bukkit.getEntity(entityUuid);
            }

            if (entity != null && entity.isValid()) {
                player.hideEntity(plugin, entity);
                player.showEntity(plugin, entity);
            }
        }
    }

    public synchronized boolean updateSkin(String npcId, String newSkinSource) {
        RPGNpc existing = npcsById.get(npcId);
        if (existing == null) return false;
        RPGNpc updated = new RPGNpc(existing.id(), existing.type(), existing.name(), existing.location(), newSkinSource, existing.profession());
        npcsById.put(npcId, updated);
        resolvedSkinsByNpcId.remove(npcId);
        saveAll();
        UUID entityUuid = spawnedEntityByNpcId.get(npcId);
        if (entityUuid != null) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity instanceof Mannequin mannequin) {
                MannequinSkinResolver.applyAndCapture(mannequin, newSkinSource, plugin.getLogger())
                        .thenAccept(texture -> rememberResolvedSkin(
                                npcId, texture.getValue(), texture.getSignature()));
            }
        }
        return true;
    }

    public void handleChunkUnload(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            String id = entityToId.remove(entity.getUniqueId());
            if (id != null) spawnedEntityByNpcId.remove(id);
        }
    }

    public void handleChunkLoad(Chunk chunk) {
        NpcChunkKey key = new NpcChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        for (String npcId : npcChunkIndex.getOrDefault(key, List.of())) {
            RPGNpc npc = npcsById.get(npcId);
            if (npc != null) spawnEntityFor(npc);
        }
    }

    public synchronized boolean removeById(String id) {
        RPGNpc npc = npcsById.remove(id);
        if (npc == null) return false;
        removeFromChunkIndex(npc);
        removeSpawnedEntity(id);
        saveAll();
        return true;
    }

    public synchronized boolean rename(String id, String newName) {
        if (newName == null || newName.isBlank()) return false;
        RPGNpc existing = npcsById.get(id);
        if (existing == null) return false;
        RPGNpc updated = new RPGNpc(existing.id(), existing.type(), newName.trim(), existing.location(), existing.skinSource(), existing.profession());
        npcsById.put(id, updated);
        saveAll();
        UUID entityUuid = spawnedEntityByNpcId.get(id);
        if (entityUuid != null) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null) entity.customName(Component.text(newName.trim(), existing.type().getColor()));
        }
        return true;
    }

    public Optional<RPGNpc> getByEntity(UUID entityUuid) {
        String id = entityToId.get(entityUuid);
        return id != null ? Optional.ofNullable(npcsById.get(id)) : Optional.empty();
    }

    public Optional<RPGNpc> getById(String id) { return Optional.ofNullable(npcsById.get(id)); }
    public Collection<RPGNpc> getAll() { return List.copyOf(npcsById.values()); }
    public Collection<UUID> getSpawnedEntityUuids() { return List.copyOf(spawnedEntityByNpcId.values()); }

    @Override
    public synchronized void close() {
        shutdown();
    }

    public synchronized void shutdown() {
        if (shuttingDown) return;
        shuttingDown = true;
        saveAll();
        persistenceExecutor.shutdown();
        plugin.getLogger().info("NPC manager shutdown: flushing npc persistence.");
        try {
            if (!persistenceExecutor.awaitTermination(10, TimeUnit.SECONDS)) persistenceExecutor.shutdownNow();
        } catch (InterruptedException exception) {
            persistenceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        plugin.getLogger().info("NPC manager shutdown: persistence executor terminated.");
        despawnAllTracked();
        npcsById.clear();
        npcChunkIndex.clear();
    }

    /** Captures the complete NPC state on the server thread and serializes it off-thread. */
    public synchronized void saveAll() {
        if (persistenceExecutor.isShutdown()) return;
        Map<String, NpcSnapshot> snapshot = new java.util.HashMap<>();
        for (RPGNpc npc : npcsById.values()) {
            Location location = npc.location();
            snapshot.put(npc.id(), new NpcSnapshot(
                    npc.id(), npc.type().name(), npc.name(), location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(),
                    npc.skinSource(), npc.profession() == null ? null : npc.profession().name(),
                    Optional.ofNullable(resolvedSkinsByNpcId.get(npc.id())).map(StoredSkin::value).orElse(null),
                    Optional.ofNullable(resolvedSkinsByNpcId.get(npc.id())).map(StoredSkin::signature).orElse(null)));
        }
        int next = nextId.get();
        persistenceChain = persistenceChain.handle((ignored, throwable) -> null)
                .thenRunAsync(() -> writeSnapshot(next, snapshot), persistenceExecutor);
    }

    private void writeSnapshot(int next, Map<String, NpcSnapshot> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-id", next);
        for (NpcSnapshot npc : snapshot.values()) {
            String path = "npcs." + npc.id();
            yaml.set(path + ".type", npc.type());
            yaml.set(path + ".name", npc.name());
            yaml.set(path + ".world", npc.world());
            yaml.set(path + ".x", npc.x());
            yaml.set(path + ".y", npc.y());
            yaml.set(path + ".z", npc.z());
            yaml.set(path + ".yaw", (double) npc.yaw());
            yaml.set(path + ".pitch", (double) npc.pitch());
            if (npc.skinSource() != null && !npc.skinSource().isBlank()) yaml.set(path + ".skin-source", npc.skinSource());
            if (npc.skinValue() != null && !npc.skinValue().isBlank()) yaml.set(path + ".skin-value", npc.skinValue());
            if (npc.skinSignature() != null && !npc.skinSignature().isBlank()) yaml.set(path + ".skin-signature", npc.skinSignature());
            if (npc.profession() != null) yaml.set(path + ".profession", npc.profession());
        }

        Path target = file.toPath();
        Path temporary = target.resolveSibling(file.getName() + ".tmp");
        try {
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save npcs.yml", exception);
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private record NpcSnapshot(String id, String type, String name, String world, double x, double y, double z,
                               float yaw, float pitch, String skinSource, String profession, String skinValue,
                               String skinSignature) { }

    private void rememberResolvedSkin(String npcId, String value, String signature) {
        if (value == null || value.isBlank()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            synchronized (this) {
                if (shuttingDown || !npcsById.containsKey(npcId)) return;

                // Persist the exact texture property returned by the resolver.
                // This mirrors the Citizens2 approach: the resolved texture is
                // stored at acquisition time instead of being reconstructed
                // from mutable mannequin state after a restart.
                resolvedSkinsByNpcId.put(npcId, new StoredSkin(value, signature));
                plugin.getLogger().info("NPC skin persisted in memory: npc=" + npcId
                        + ", texture=" + abbreviateTexture(value));
                saveAll();
            }
        });
    }

    private record StoredSkin(String value, String signature) { }

    private static String abbreviateTexture(String value) {
        if (value == null || value.isBlank()) return "empty";
        return value.length() <= 16 ? value : value.substring(0, 16) + "...";
    }

    private void addToChunkIndex(RPGNpc npc) {
        Location location = npc.location();
        NpcChunkKey key = new NpcChunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        npcChunkIndex.compute(key, (ignored, current) -> {
            List<String> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
            if (!updated.contains(npc.id())) updated.add(npc.id());
            return List.copyOf(updated);
        });
    }

    private void removeFromChunkIndex(RPGNpc npc) {
        Location location = npc.location();
        NpcChunkKey key = new NpcChunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        npcChunkIndex.computeIfPresent(key, (ignored, current) -> {
            List<String> updated = current.stream().filter(id -> !id.equals(npc.id())).toList();
            return updated.isEmpty() ? null : updated;
        });
    }

    private void removeSpawnedEntity(String id) {
        UUID entityUuid = spawnedEntityByNpcId.remove(id);
        if (entityUuid == null) return;
        entityToId.remove(entityUuid);
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity != null) entity.remove();
    }

    private void despawnAllTracked() {
        for (UUID entityUuid : entityToId.keySet()) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null) entity.remove();
        }
        spawnedEntityByNpcId.clear();
        entityToId.clear();
    }

    private NpcType parseType(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toUpperCase();
        if (normalized.equals("BLACKSMITH") || normalized.equals("PROFESSION_BLACKSMITH")) return NpcType.PROFESSION_BLACKSMITH;
        if (normalized.equals("PROFESSION_PROVISIONER")) return NpcType.PROFESSION_COOK;
        if (normalized.equals("PROFESSION_COOK")) return NpcType.PROFESSION_COOK;
        try { return NpcType.valueOf(normalized); }
        catch (IllegalArgumentException e) { return null; }
    }

    private Profession parseProfession(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.trim().toUpperCase();
        if (normalized.equals("PROVISIONER")) return Profession.COOK;
        try { return Profession.valueOf(normalized); }
        catch (IllegalArgumentException e) { return null; }
    }

    private Profession professionFor(NpcType type) {
        return switch (type) {
            case PROFESSION_BLACKSMITH -> Profession.BLACKSMITH;
            case PROFESSION_SCHOLAR -> Profession.SCHOLAR;
            case PROFESSION_FARMER -> Profession.FARMER;
            case PROFESSION_COOK -> Profession.COOK;
            case PROFESSION_TAILOR -> Profession.TAILOR;
            case PROFESSION_ALCHEMIST -> Profession.ALCHEMIST;
            case PROFESSION_MASON -> Profession.MASON;
            case PROFESSION_FISHERMAN -> Profession.FISHERMAN;
            case PROFESSION_WOODCUTTER -> Profession.WOODCUTTER;
            default -> null;
        };
    }

    private record NpcChunkKey(String world, int x, int z) { }
}
