package de.pixelrpg.rpg.npc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class RegionalNpcPopulationManager implements Listener {
    private final Plugin plugin;
    private final NpcManager npcManager;
    private final NpcProfileStore profileStore;
    private final NpcIdentityService identityService;
    private final File file;
    private final Gson gson = new Gson();
    private final Set<String> populatedRegions = new HashSet<>();

    public RegionalNpcPopulationManager(Plugin plugin, NpcManager npcManager, NpcProfileStore profileStore, NpcIdentityService identityService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.npcManager = Objects.requireNonNull(npcManager, "npcManager");
        this.profileStore = Objects.requireNonNull(profileStore, "profileStore");
        this.identityService = Objects.requireNonNull(identityService, "identityService");
        this.file = new File(plugin.getDataFolder(), "regional-npc-population.json");
    }

    public void load() {
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Set<String> loaded = gson.fromJson(reader, new TypeToken<Set<String>>() { }.getType());
            if (loaded != null) populatedRegions.addAll(loaded);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to load regional-npc-population.json: " + exception.getMessage());
        }
    }

    /** Populates an eligible overworld region when a generated chunk becomes available. */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        int regionX = Math.floorDiv(chunk.getX(), 32);
        int regionZ = Math.floorDiv(chunk.getZ(), 32);
        String regionId = world.getUID() + ":" + regionX + ":" + regionZ;
        if (populatedRegions.contains(regionId)) return;

        if (!isEligibleBiome(chunk)) {
            populatedRegions.add(regionId);
            save();
            return;
        }

        Location location = findSafeLocation(chunk);
        if (location == null) return;

        String npcId = "regional:" + regionId;
        RPGNpc existing = npcManager.getById(npcId).orElse(null);
        if (existing == null) {
            try {
                existing = npcManager.createWithId(npcId, NpcType.TRAVEL, "Wanderer", location, null, null);
            } catch (IllegalArgumentException ignored) {
                existing = npcManager.getById(npcId).orElse(null);
            }
        }
        if (existing != null) {
            identityService.assignIdentity(existing);
            profileStore.getOrCreate(existing);
            populatedRegions.add(regionId);
            save();
        }
    }

    private boolean isEligibleBiome(Chunk chunk) {
        Location sample = chunk.getBlock(8, Math.max(chunk.getWorld().getMinHeight(), 64), 8).getLocation();
        String biome = chunk.getWorld().getComputedBiome(sample.getBlockX(), sample.getBlockY(), sample.getBlockZ())
                .key().value();
        return biome.equals("plains")
                || biome.equals("forest")
                || biome.equals("birch_forest")
                || biome.equals("taiga")
                || biome.equals("savanna")
                || biome.equals("desert")
                || biome.equals("meadow");
    }

    private Location findSafeLocation(Chunk chunk) {
        World world = chunk.getWorld();
        int centerX = chunk.getX() * 16 + 8;
        int centerZ = chunk.getZ() * 16 + 8;
        for (int radius = 0; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    int y = world.getHighestBlockYAt(x, z);
                    Material ground = world.getBlockAt(x, y - 1, z).getType();
                    if (!ground.isSolid()) continue;
                    if (!world.getBlockAt(x, y, z).isPassable()) continue;
                    if (!world.getBlockAt(x, y + 1, z).isPassable()) continue;
                    return new Location(world, x + 0.5D, y, z + 0.5D);
                }
            }
        }
        return null;
    }

    public void save() {
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                gson.toJson(populatedRegions, writer);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to save regional-npc-population.json: " + exception.getMessage());
        }
    }
}
