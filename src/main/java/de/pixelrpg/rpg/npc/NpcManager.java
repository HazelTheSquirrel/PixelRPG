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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class NpcManager {
    private final Plugin plugin;
    private final File file;
    private final Map<String, RPGNpc> npcsById = new ConcurrentHashMap<>();
    private final Map<String, UUID> spawnedEntityByNpcId = new ConcurrentHashMap<>();
    private final Map<UUID, String> entityToId = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public NpcManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public void loadAll() {
        despawnAllTracked();
        npcsById.clear();
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
            Profession profession = parseProfession(section.getString("profession"));
            if (type == NpcType.PROFESSION_TRAINER && profession == null) {
                plugin.getLogger().warning("Skipping profession trainer NPC " + id + ": missing profession.");
                continue;
            }

            RPGNpc npc = new RPGNpc(id, type, name, location, skinSource, profession);
            npcsById.put(id, npc);
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
    public RPGNpc createWithId(String id, NpcType type, String name, Location location, String skinSource, Profession profession) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("NPC id must not be blank");
        if (npcsById.containsKey(id)) throw new IllegalArgumentException("NPC id already exists: " + id);
        RPGNpc npc = new RPGNpc(id, type, name, location.clone(), skinSource, profession);
        npcsById.put(id, npc);
        spawnEntityFor(npc);
        saveAll();
        return npc;
    }

    public void spawnEntityFor(RPGNpc npc) {
        if (spawnedEntityByNpcId.containsKey(npc.id())) return;

        Mannequin mannequin = npc.location().getWorld().spawn(npc.location(), Mannequin.class, entity -> {
            entity.setAI(false);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.setRemoveWhenFarAway(false);
            entity.setCollidable(false);
            entity.customName(Component.text(npc.name(), npc.type().getColor()));
            entity.setCustomNameVisible(true);
            entity.getPersistentDataContainer().set(RPGKeys.Npc.npcType(), PersistentDataType.STRING, npc.type().name());
            entity.getPersistentDataContainer().set(RPGKeys.Npc.npcId(), PersistentDataType.STRING, npc.id());
            if (npc.hasCustomSkin()) MannequinSkinResolver.apply(entity, npc.skinSource(), plugin.getLogger());
        });

        spawnedEntityByNpcId.put(npc.id(), mannequin.getUniqueId());
        entityToId.put(mannequin.getUniqueId(), npc.id());
    }

    public boolean updateSkin(String npcId, String newSkinSource) {
        RPGNpc existing = npcsById.get(npcId);
        if (existing == null) return false;
        RPGNpc updated = new RPGNpc(existing.id(), existing.type(), existing.name(), existing.location(), newSkinSource, existing.profession());
        npcsById.put(npcId, updated);
        saveAll();
        UUID entityUuid = spawnedEntityByNpcId.get(npcId);
        if (entityUuid != null) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity instanceof Mannequin mannequin) MannequinSkinResolver.apply(mannequin, newSkinSource, plugin.getLogger());
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
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        String worldName = chunk.getWorld().getName();
        for (RPGNpc npc : npcsById.values()) {
            if (!npc.location().getWorld().getName().equals(worldName)) continue;
            if ((npc.location().getBlockX() >> 4) == chunkX && (npc.location().getBlockZ() >> 4) == chunkZ) spawnEntityFor(npc);
        }
    }

    public boolean removeById(String id) {
        RPGNpc npc = npcsById.remove(id);
        if (npc == null) return false;
        removeSpawnedEntity(id);
        saveAll();
        return true;
    }

    public boolean rename(String id, String newName) {
        RPGNpc existing = npcsById.get(id);
        if (existing == null) return false;
        RPGNpc updated = new RPGNpc(existing.id(), existing.type(), newName, existing.location(), existing.skinSource(), existing.profession());
        npcsById.put(id, updated);
        saveAll();
        UUID entityUuid = spawnedEntityByNpcId.get(id);
        if (entityUuid != null) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null) entity.customName(Component.text(newName, existing.type().getColor()));
        }
        return true;
    }

    public Optional<RPGNpc> getByEntity(UUID entityUuid) {
        String id = entityToId.get(entityUuid);
        return id != null ? Optional.ofNullable(npcsById.get(id)) : Optional.empty();
    }

    public Optional<RPGNpc> getById(String id) { return Optional.ofNullable(npcsById.get(id)); }
    public Collection<RPGNpc> getAll() { return npcsById.values(); }
    public Collection<UUID> getSpawnedEntityUuids() { return spawnedEntityByNpcId.values(); }

    public void shutdown() {
        despawnAllTracked();
        npcsById.clear();
    }

    public void saveAll() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-id", nextId.get());
        for (RPGNpc npc : npcsById.values()) {
            String path = "npcs." + npc.id();
            yaml.set(path + ".type", npc.type().name());
            yaml.set(path + ".name", npc.name());
            yaml.set(path + ".world", npc.location().getWorld().getName());
            yaml.set(path + ".x", npc.location().getX());
            yaml.set(path + ".y", npc.location().getY());
            yaml.set(path + ".z", npc.location().getZ());
            yaml.set(path + ".yaw", (double) npc.location().getYaw());
            yaml.set(path + ".pitch", (double) npc.location().getPitch());
            if (npc.hasCustomSkin()) yaml.set(path + ".skin-source", npc.skinSource());
            if (npc.profession() != null) yaml.set(path + ".profession", npc.profession().name());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save npcs.yml", e);
        }
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
        try { return NpcType.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private Profession parseProfession(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Profession.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
