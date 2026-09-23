package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.profession.Profession;
import org.bukkit.Chunk;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class NpcRuntimeManager implements AutoCloseable {
    private final Plugin plugin;
    private final RPGKeys keys;
    private final NpcRepository repository;
    private final Map<String, RPGNpc> npcs = new ConcurrentHashMap<>();
    private final Map<String, UUID> spawnedByNpc = new ConcurrentHashMap<>();
    private final Map<UUID, String> npcByEntity = new ConcurrentHashMap<>();
    private final Map<NpcChunkKey, List<String>> chunkIndex = new ConcurrentHashMap<>();
    private final Map<String, SkinProperty> storedSkins = new ConcurrentHashMap<>();
    private volatile boolean shuttingDown;
    private CompletableFuture<Void> persistenceChain = CompletableFuture.completedFuture(null);
    private int nextId = 1;

    public NpcRuntimeManager(Plugin plugin, RPGKeys keys, NpcRepository repository) {
        this.plugin = plugin;
        this.keys = keys;
        this.repository = repository;
    }

    public void loadAsync() {
        repository.load().thenAccept(records -> plugin.getServer().getScheduler().runTask(plugin, () -> applyLoaded(records)))
                .exceptionally(exception -> {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load NPC persistence.", exception);
                    return null;
                });
        if (repository instanceof YamlNpcRepository yaml) {
            yaml.loadNextId().thenAccept(id -> plugin.getServer().getScheduler().runTask(plugin, () -> nextId = Math.max(1, id)));
        }
    }

    private synchronized void applyLoaded(List<NpcRepository.NpcRecord> records) {
        if (shuttingDown) return;
        npcs.clear();
        chunkIndex.clear();
        storedSkins.clear();
        for (NpcRepository.NpcRecord record : records) {
            NpcType type;
            World world = record.world() == null ? null : plugin.getServer().getWorld(record.world());
            try { type = NpcType.valueOf(record.type()); } catch (RuntimeException ignored) { continue; }
            if (world == null) continue;
            Profession profession = parseProfession(record.profession());
            if (profession == null) profession = professionFor(type);
            RPGNpc npc = new RPGNpc(record.id(), type, record.name(), new Location(world, record.x(), record.y(), record.z(), record.yaw(), record.pitch()), record.skinSource(), profession);
            npcs.put(npc.id(), npc);
            index(npc);
            if (record.skinValue() != null && !record.skinValue().isBlank()) {
                storedSkins.put(npc.id(), new SkinProperty(record.skinValue(), record.skinSignature()));
            }
            if (world.isChunkLoaded(npc.location().getBlockX() >> 4, npc.location().getBlockZ() >> 4)) spawn(npc);
        }
    }

    public synchronized RPGNpc create(NpcType type, String name, Location location, String skinSource, Profession profession) {
        ensureRunning();
        String id = String.valueOf(nextId++);
        RPGNpc npc = createWithId(id, type, name, location, skinSource, profession);
        persist();
        return npc;
    }

    public synchronized RPGNpc createWithId(String id, NpcType type, String name, Location location, String skinSource, Profession profession) {
        ensureRunning();
        if (npcs.containsKey(id)) throw new IllegalArgumentException("NPC id already exists: " + id);
        RPGNpc npc = new RPGNpc(id, type, name, location, skinSource, profession == null ? professionFor(type) : profession);
        npcs.put(id, npc);
        index(npc);
        spawn(npc);
        return npc;
    }

    public synchronized boolean removeById(String id) {
        RPGNpc npc = npcs.remove(id);
        if (npc == null) return false;
        unindex(npc);
        storedSkins.remove(id);
        removeSpawned(id);
        persist();
        return true;
    }

    public synchronized boolean updateSkin(String id, String source) {
        RPGNpc npc = npcs.get(id);
        if (npc == null) return false;
        RPGNpc updated = new RPGNpc(npc.id(), npc.type(), npc.name(), npc.location(), source, npc.profession());
        npcs.put(id, updated);
        storedSkins.remove(id);
        UUID uuid = spawnedByNpc.get(id);
        if (uuid != null) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity instanceof Mannequin mannequin && mannequin.isValid()) resolveSkin(updated, mannequin);
        }
        persist();
        return true;
    }

    public synchronized boolean rename(String id, String name) {
        if (name == null || name.isBlank()) return false;
        RPGNpc npc = npcs.get(id);
        if (npc == null) return false;
        RPGNpc updated = new RPGNpc(id, npc.type(), name.trim(), npc.location(), npc.skinSource(), npc.profession());
        npcs.put(id, updated);
        UUID uuid = spawnedByNpc.get(id);
        if (uuid != null) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity != null) entity.customName(net.kyori.adventure.text.Component.text(updated.name(), updated.type().getColor()));
        }
        persist();
        return true;
    }

    public void handleChunkLoad(Chunk chunk) {
        for (String id : chunkIndex.getOrDefault(new NpcChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()), List.of())) {
            RPGNpc npc = npcs.get(id);
            if (npc != null) spawn(npc);
        }
    }

    public void handleChunkUnload(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            String id = npcByEntity.remove(entity.getUniqueId());
            if (id != null) spawnedByNpc.remove(id, entity.getUniqueId());
        }
    }

    public void resyncPlayer(Player player) {
        for (UUID uuid : spawnedByNpc.values()) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity != null && entity.isValid() && entity.getWorld() == player.getWorld()) {
                player.hideEntity(plugin, entity);
                player.showEntity(plugin, entity);
            }
        }
    }

    public Optional<RPGNpc> getByEntity(UUID uuid) { return Optional.ofNullable(npcByEntity.get(uuid)).map(npcs::get); }
    public Optional<RPGNpc> getById(String id) { return Optional.ofNullable(npcs.get(id)); }
    public Collection<RPGNpc> getAll() { return List.copyOf(npcs.values()); }
    public Collection<UUID> getSpawnedEntityUuids() { return List.copyOf(spawnedByNpc.values()); }

    private void spawn(RPGNpc npc) {
        UUID existing = spawnedByNpc.get(npc.id());
        if (existing != null) {
            Entity entity = plugin.getServer().getEntity(existing);
            if (entity instanceof Mannequin mannequin && mannequin.isValid()) return;
            spawnedByNpc.remove(npc.id(), existing);
            npcByEntity.remove(existing, npc.id());
        }
        Mannequin mannequin = npc.location().getWorld().spawn(npc.location(), Mannequin.class, entity -> {
            entity.setAI(false);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.setRemoveWhenFarAway(false);
            entity.setCollidable(false);
            entity.customName(net.kyori.adventure.text.Component.text(npc.name(), npc.type().getColor()));
            entity.setCustomNameVisible(false);
            entity.getPersistentDataContainer().set(keys.npcType(), PersistentDataType.STRING, npc.type().name());
            entity.getPersistentDataContainer().set(keys.npcId(), PersistentDataType.STRING, npc.id());
        });
        spawnedByNpc.put(npc.id(), mannequin.getUniqueId());
        npcByEntity.put(mannequin.getUniqueId(), npc.id());
        resolveSkin(npc, mannequin);
    }

    private void resolveSkin(RPGNpc npc, Mannequin mannequin) {
        SkinProperty stored = storedSkins.get(npc.id());
        if (stored != null) {
            MannequinSkinResolver.applyStoredTexture(mannequin, stored.value(), stored.signature(), plugin)
                    .exceptionally(exception -> { plugin.getLogger().warning("Failed to apply persisted NPC skin for " + npc.id() + ": " + exception.getMessage()); return null; });
            return;
        }
        if (!npc.hasCustomSkin()) return;
        MannequinSkinResolver.applyAndCapture(mannequin, npc.skinSource(), plugin.getLogger())
                .thenAccept(property -> rememberSkin(npc.id(), property.getValue(), property.getSignature()))
                .exceptionally(exception -> { plugin.getLogger().warning("Failed to resolve NPC skin for " + npc.id() + ": " + exception.getMessage()); return null; });
    }

    private void rememberSkin(String id, String value, String signature) {
        if (value == null || value.isBlank() || shuttingDown) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (shuttingDown || !npcs.containsKey(id)) return;
            storedSkins.put(id, new SkinProperty(value, signature));
            persist();
        });
    }

    private void persist() {
        if (shuttingDown) return;
        List<NpcRepository.NpcRecord> records = npcs.values().stream().map(npc -> {
            Location l = npc.location();
            SkinProperty skin = storedSkins.get(npc.id());
            return new NpcRepository.NpcRecord(npc.id(), npc.type().name(), npc.name(), l.getWorld().getName(),
                    l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), npc.skinSource(),
                    skin == null ? null : skin.value(), skin == null ? null : skin.signature(),
                    npc.profession() == null ? null : npc.profession().name());
        }).toList();
        persistenceChain = persistenceChain.handle((ignored, previousFailure) -> null)
                .thenCompose(ignored -> repository.save(nextId, records))
                .exceptionally(exception -> {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to persist NPC state.", exception);
            return null;
        });
    }

    private void index(RPGNpc npc) {
        Location l = npc.location();
        NpcChunkKey key = new NpcChunkKey(l.getWorld().getName(), l.getBlockX() >> 4, l.getBlockZ() >> 4);
        chunkIndex.compute(key, (ignored, ids) -> {
            java.util.ArrayList<String> copy = ids == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(ids);
            if (!copy.contains(npc.id())) copy.add(npc.id());
            return List.copyOf(copy);
        });
    }
    private void unindex(RPGNpc npc) {
        Location l = npc.location();
        NpcChunkKey key = new NpcChunkKey(l.getWorld().getName(), l.getBlockX() >> 4, l.getBlockZ() >> 4);
        chunkIndex.computeIfPresent(key, (ignored, ids) -> ids.stream().filter(id -> !id.equals(npc.id())).toList());
    }
    private void removeSpawned(String id) {
        UUID uuid = spawnedByNpc.remove(id);
        if (uuid == null) return;
        npcByEntity.remove(uuid);
        Entity entity = plugin.getServer().getEntity(uuid);
        if (entity != null) entity.remove();
    }
    private void ensureRunning() { if (shuttingDown) throw new IllegalStateException("NPC runtime is shutting down"); }
    private Profession parseProfession(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Profession.valueOf(value); } catch (IllegalArgumentException ignored) { return null; }
    }
    private Profession professionFor(NpcType type) {
        String name = type.name();
        if (!name.startsWith("PROFESSION_")) return null;
        try { return Profession.valueOf(name.substring("PROFESSION_").toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
    }
    @Override public synchronized void close() {
        if (shuttingDown) return;
        persist();
        shuttingDown = true;
        try {
            persistenceChain.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "NPC persistence did not flush cleanly on shutdown.", exception);
        }
        for (UUID uuid : List.copyOf(spawnedByNpc.values())) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity != null) entity.remove();
        }
        spawnedByNpc.clear(); npcByEntity.clear(); npcs.clear(); chunkIndex.clear(); storedSkins.clear();
        repository.close();
    }
    private record SkinProperty(String value, String signature) {}
    private record NpcChunkKey(String world, int x, int z) {}
}
