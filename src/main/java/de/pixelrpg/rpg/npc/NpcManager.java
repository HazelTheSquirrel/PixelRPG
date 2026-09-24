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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns data-driven NPC definitions, mannequin runtime entities and persisted skin snapshots. */
public final class NpcManager implements AutoCloseable {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, RPGNpc> npcs = new ConcurrentHashMap<>();
    private final Map<String, UUID> entityByNpc = new ConcurrentHashMap<>();
    private final Map<UUID, String> npcByEntity = new ConcurrentHashMap<>();
    private final Map<NpcChunkKey, List<String>> chunkIndex = new ConcurrentHashMap<>();
    private final Map<String, StoredSkin> storedSkins = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private int nextId = 1;
    private volatile boolean closed;

    public NpcManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public synchronized void load() {
        despawnAll();
        npcs.clear();
        entityByNpc.clear();
        npcByEntity.clear();
        chunkIndex.clear();
        storedSkins.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextId = Math.max(1, yaml.getInt("next-id", 1));
        ConfigurationSection root = yaml.getConfigurationSection("npcs");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            try {
                NpcType type = NpcType.valueOf(section.getString("type", ""));
                World world = Bukkit.getWorld(section.getString("world", ""));
                if (world == null) continue;
                Location location = new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                        (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
                String name = section.getString("name", "NPC");
                String skinSource = section.getString("skin-source");
                String skinValue = section.getString("skin-value");
                String skinSignature = section.getString("skin-signature");
                if (skinValue != null && !skinValue.isBlank()) storedSkins.put(id, new StoredSkin(skinValue, skinSignature));
                Profession profession = parseProfession(section.getString("profession"));
                RPGNpc npc = new RPGNpc(id, type, name, location, skinSource, profession);
                npcs.put(id, npc);
                addIndex(npc);
                if (world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) spawn(npc);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Ignoring malformed NPC definition " + id + ".");
            }
        }
    }

    public synchronized RPGNpc create(NpcType type, String name, Location location, String skinSource, Profession profession) {
        String id = Integer.toString(nextId++);
        while (npcs.containsKey(id)) id = Integer.toString(nextId++);
        return createWithId(id, type, name, location, skinSource, profession);
    }

    public synchronized RPGNpc createWithId(String id, NpcType type, String name, Location location, String skinSource, Profession profession) {
        if (npcs.containsKey(id)) throw new IllegalArgumentException("NPC id already exists: " + id);
        Profession effective = profession != null ? profession : professionFor(type);
        RPGNpc npc = new RPGNpc(id, type, name, location, skinSource, effective);
        npcs.put(id, npc);
        addIndex(npc);
        spawn(npc);
        save();
        return npc;
    }

    public synchronized boolean rename(String id, String name) {
        RPGNpc existing = npcs.get(id);
        if (existing == null || name == null || name.isBlank()) return false;
        RPGNpc updated = new RPGNpc(id, existing.type(), name.trim(), existing.location(), existing.skinSource(), existing.profession());
        npcs.put(id, updated);
        UUID uuid = entityByNpc.get(id);
        Entity entity = uuid == null ? null : Bukkit.getEntity(uuid);
        if (entity != null) entity.customName(Component.text(updated.name(), updated.type().color()));
        save();
        return true;
    }

    public synchronized boolean updateSkin(String id, String skinSource) {
        RPGNpc existing = npcs.get(id);
        if (existing == null) return false;
        npcs.put(id, new RPGNpc(id, existing.type(), existing.name(), existing.location(), skinSource, existing.profession()));
        storedSkins.remove(id);
        UUID uuid = entityByNpc.get(id);
        Entity entity = uuid == null ? null : Bukkit.getEntity(uuid);
        if (entity instanceof Mannequin mannequin && skinSource != null && !skinSource.isBlank()) applySkin(npcs.get(id), mannequin);
        save();
        return true;
    }

    public synchronized boolean remove(String id) {
        RPGNpc npc = npcs.remove(id);
        if (npc == null) return false;
        removeIndex(npc);
        removeEntity(id);
        storedSkins.remove(id);
        save();
        return true;
    }

    public Optional<RPGNpc> getById(String id) { return Optional.ofNullable(npcs.get(id)); }
    public Optional<RPGNpc> getByEntity(UUID entityId) {
        String id = npcByEntity.get(entityId);
        return id == null ? Optional.empty() : Optional.ofNullable(npcs.get(id));
    }
    public Collection<RPGNpc> getAll() { return List.copyOf(npcs.values()); }

    public void handleChunkLoad(Chunk chunk) {
        NpcChunkKey key = new NpcChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        for (String id : chunkIndex.getOrDefault(key, List.of())) {
            RPGNpc npc = npcs.get(id);
            if (npc != null) spawn(npc);
        }
    }

    public void handleChunkUnload(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            String id = npcByEntity.remove(entity.getUniqueId());
            if (id != null) entityByNpc.remove(id, entity.getUniqueId());
        }
    }

    public void spawn(RPGNpc npc) {
        if (closed) return;
        UUID tracked = entityByNpc.get(npc.id());
        if (tracked != null) {
            Entity entity = Bukkit.getEntity(tracked);
            if (entity instanceof Mannequin mannequin && mannequin.isValid()) return;
            removeEntity(npc.id());
        }

        Mannequin mannequin = npc.location().getWorld().spawn(npc.location(), Mannequin.class, entity -> {
            entity.setAI(false);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.setRemoveWhenFarAway(false);
            entity.setCollidable(false);
            entity.customName(Component.text(npc.name(), npc.type().color()));
            entity.setCustomNameVisible(false);
            entity.getPersistentDataContainer().set(RPGKeys.Npc.npcType(), PersistentDataType.STRING, npc.type().name());
            entity.getPersistentDataContainer().set(RPGKeys.Npc.npcId(), PersistentDataType.STRING, npc.id());
        });
        entityByNpc.put(npc.id(), mannequin.getUniqueId());
        npcByEntity.put(mannequin.getUniqueId(), npc.id());
        plugin.getServer().getScheduler().runTask(plugin, () -> applySkin(npc, mannequin));
    }

    public void resyncPlayer(org.bukkit.entity.Player player) {
        for (RPGNpc npc : npcs.values()) {
            if (!npc.location().getWorld().equals(player.getWorld())) continue;
            if (!npc.location().getChunk().isLoaded()) continue;
            UUID id = entityByNpc.get(npc.id());
            Entity entity = id == null ? null : Bukkit.getEntity(id);
            if (!(entity instanceof Mannequin mannequin) || !mannequin.isValid()) {
                spawn(npc);
                id = entityByNpc.get(npc.id());
                entity = id == null ? null : Bukkit.getEntity(id);
            }
            if (entity != null && entity.isValid()) {
                player.hideEntity(plugin, entity);
                player.showEntity(plugin, entity);
            }
        }
    }

    private void applySkin(RPGNpc npc, Mannequin mannequin) {
        StoredSkin stored = storedSkins.get(npc.id());
        if (stored != null) {
            MannequinSkinResolver.applyStoredTexture(mannequin, stored.value(), stored.signature(), plugin)
                    .exceptionally(exception -> { plugin.getLogger().warning("Failed to apply stored NPC skin: " + exception.getMessage()); return null; });
            return;
        }
        if (!npc.hasCustomSkin()) return;
        MannequinSkinResolver.applyPlayerName(mannequin, npc.skinSource(), plugin)
                .thenAccept(property -> rememberSkin(npc.id(), property.getValue(), property.getSignature()))
                .exceptionally(exception -> { plugin.getLogger().warning("Failed to resolve NPC skin '" + npc.skinSource() + "': " + unwrap(exception).getMessage()); return null; });
    }

    private synchronized void rememberSkin(String id, String value, String signature) {
        storedSkins.put(id, new StoredSkin(value, signature));
        save();
    }

    private void addIndex(RPGNpc npc) {
        NpcChunkKey key = key(npc.location());
        chunkIndex.compute(key, (ignored, ids) -> {
            java.util.ArrayList<String> result = new java.util.ArrayList<>(ids == null ? List.of() : ids);
            if (!result.contains(npc.id())) result.add(npc.id());
            return List.copyOf(result);
        });
    }

    private void removeIndex(RPGNpc npc) {
        NpcChunkKey key = key(npc.location());
        chunkIndex.computeIfPresent(key, (ignored, ids) -> ids.stream().filter(id -> !id.equals(npc.id())).toList());
    }

    private NpcChunkKey key(Location location) {
        return new NpcChunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private void removeEntity(String id) {
        UUID uuid = entityByNpc.remove(id);
        if (uuid == null) return;
        npcByEntity.remove(uuid, id);
        Entity entity = Bukkit.getEntity(uuid);
        if (entity != null) entity.remove();
    }

    private void despawnAll() {
        entityByNpc.keySet().forEach(this::removeEntity);
    }

    private synchronized void save() {
        if (closed || ioExecutor.isShutdown()) return;
        Map<String, NpcSnapshot> snapshot = new java.util.HashMap<>();
        for (RPGNpc npc : npcs.values()) {
            Location location = npc.location();
            StoredSkin skin = storedSkins.get(npc.id());
            snapshot.put(npc.id(), new NpcSnapshot(npc.id(), npc.type().name(), npc.name(),
                    location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch(), npc.skinSource(),
                    npc.profession() == null ? null : npc.profession().name(),
                    skin == null ? null : skin.value(), skin == null ? null : skin.signature()));
        }
        ioExecutor.execute(() -> write(snapshot, nextId));
    }

    private void write(Map<String, NpcSnapshot> snapshot, int next) {
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
        Path temp = target.resolveSibling(file.getName() + ".tmp");
        try {
            yaml.save(temp.toFile());
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save npcs.yml.", exception);
        }
    }

    private Profession parseProfession(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Profession.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
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

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @Override public void close() { shutdown(); }

    public void shutdown() {
        if (closed) return;
        save();
        closed = true;
        ioExecutor.close();
        despawnAll();
        npcs.clear();
        chunkIndex.clear();
        storedSkins.clear();
    }

    private record NpcChunkKey(String world, int x, int z) {}
    private record StoredSkin(String value, String signature) {}
    private record NpcSnapshot(String id, String type, String name, String world, double x, double y, double z,
                               float yaw, float pitch, String skinSource, String profession, String skinValue, String skinSignature) {}
}
