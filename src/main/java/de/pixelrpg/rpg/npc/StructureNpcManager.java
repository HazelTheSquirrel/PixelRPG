package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.dialogue.NpcKnowledgeStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class StructureNpcManager implements Listener {
    private final Plugin plugin;
    private final NpcManager npcManager;
    private final NpcProfileStore profileStore;
    private final NpcKnowledgeStore npcKnowledgeStore;
    private final NpcPresentationService presentationService;
    private final File file;
    private final Gson gson = new Gson();
    private final Set<String> initializedStructures = ConcurrentHashMap.newKeySet();
    private final Map<String, List<StructureNpcTemplate>> templates = new ConcurrentHashMap<>();

    public StructureNpcManager(Plugin plugin, NpcManager npcManager, NpcProfileStore profileStore, NpcKnowledgeStore npcKnowledgeStore, NpcPresentationService presentationService) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.profileStore = profileStore;
        this.npcKnowledgeStore = npcKnowledgeStore;
        this.presentationService = presentationService;
        this.file = new File(plugin.getDataFolder(), "structure-npcs.json");
        load();
    }

    private void load() {
        if (!file.exists()) copyDefault();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<String, List<StructureNpcTemplate>> loaded = gson.fromJson(reader,
                    new TypeToken<Map<String, List<StructureNpcTemplate>>>() { }.getType());
            if (loaded != null) templates.putAll(loaded);
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().warning("Failed to load structure-npcs.json: " + exception.getMessage());
        }
        File state = new File(plugin.getDataFolder(), "structure-npc-state.json");
        if (state.exists()) {
            try (FileReader reader = new FileReader(state, StandardCharsets.UTF_8)) {
                Set<String> loaded = gson.fromJson(reader, new TypeToken<Set<String>>() { }.getType());
                if (loaded != null) initializedStructures.addAll(loaded);
            } catch (IOException | RuntimeException exception) {
                plugin.getLogger().warning("Failed to load structure-npc-state.json: " + exception.getMessage());
            }
        }
    }

    /** Populates only structures intersecting a newly loaded chunk; no world-wide scan is performed. */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        boolean changed = false;
        for (GeneratedStructure generated : world.getStructures(event.getChunk().getX(), event.getChunk().getZ())) {
            String key = structureIdentity(world, generated);
            if (initializedStructures.contains(key)) continue;
            List<StructureNpcTemplate> selected = templates.getOrDefault(structureKey(generated.getStructure()), List.of());
            if (selected.isEmpty()) continue;
            if (spawnForStructure(world, generated, key, selected)) {
                initializedStructures.add(key);
                changed = true;
            }
        }
        if (changed) saveState();
    }

    private boolean spawnForStructure(World world, GeneratedStructure generated, String identity, List<StructureNpcTemplate> selected) {
        BoundingBox box = generated.getBoundingBox();
        Location base = findSpawnLocation(world, box);
        boolean complete = true;
        for (int index = 0; index < selected.size(); index++) {
            StructureNpcTemplate template = selected.get(index);
            String npcId = "structure." + identity + "." + index;
            RPGNpc existing = npcManager.getById(npcId).orElse(null);
            if (existing != null) continue;
            Location location = base.clone().add(index * 1.5D, 0.0D, 0.0D);
            try {
                RPGNpc npc = npcManager.createWithId(npcId, NpcType.FILLER,
                        template.name(), location, null, template.profession());
                NpcProfile profile = new NpcProfile(
                        npc.id(), template.title(), template.category(), template.role(), template.profession(),
                        template.faction(), template.origin(), template.personality(), Set.copyOf(template.traits()),
                        Set.copyOf(template.knowledge()), Set.of(), Set.of(), template.behavior(), template.schedule(),
                        template.dialogueTreeId(), template.storyRelevant(), template.questRelevant(), template.loreRelevant(), template.skinSource());
                profileStore.put(profile);
                presentationService.refresh(npc);
                for (String knowledge : template.knowledge()) npcKnowledgeStore.learn(npc.id(), knowledge);
            } catch (RuntimeException exception) {
                complete = false;
                plugin.getLogger().warning("Failed to populate structure " + identity + " NPC " + npcId + ": " + exception.getMessage());
            }
        }
        return complete;
    }

    private Location findSpawnLocation(World world, BoundingBox box) {
        int centerX = (int) Math.floor((box.getMinX() + box.getMaxX()) / 2.0D);
        int centerZ = (int) Math.floor((box.getMinZ() + box.getMaxZ()) / 2.0D);
        int minY = (int) Math.floor(box.getMinY());
        int maxY = (int) Math.ceil(box.getMaxY());
        for (int y = minY; y <= maxY; y++) {
            Block feet = world.getBlockAt(centerX, y, centerZ);
            Block head = world.getBlockAt(centerX, y + 1, centerZ);
            Block below = world.getBlockAt(centerX, y - 1, centerZ);
            if (feet.isPassable() && head.isPassable() && !below.isPassable()) {
                return new Location(world, centerX + 0.5D, y, centerZ + 0.5D);
            }
        }
        return new Location(world, centerX + 0.5D, Math.max(minY, 1) + 1.0D, centerZ + 0.5D);
    }

    private String structureKey(Structure structure) {
        NamespacedKey key = RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.STRUCTURE).getKey(structure);
        return key == null ? "" : key.toString();
    }

    private String structureIdentity(World world, GeneratedStructure structure) {
        BoundingBox box = structure.getBoundingBox();
        String raw = world.getKey() + ":" + structureKey(structure.getStructure()) + ":" +
                box.getMinX() + ":" + box.getMinY() + ":" + box.getMinZ() + ":" +
                box.getMaxX() + ":" + box.getMaxY() + ":" + box.getMaxZ();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) builder.append(String.format("%02x", value));
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void saveState() {
        File state = new File(plugin.getDataFolder(), "structure-npc-state.json");
        try (FileWriter writer = new FileWriter(state, StandardCharsets.UTF_8)) { gson.toJson(initializedStructures, writer); }
        catch (IOException exception) { plugin.getLogger().warning("Failed to save structure NPC state: " + exception.getMessage()); }
    }

    private void copyDefault() {
        try { plugin.saveResource("data/structure-npcs.json", false); }
        catch (IllegalArgumentException ignored) { }
    }

    public void shutdown() { saveState(); }

    public record StructureNpcTemplate(
            String name, String title, NpcCategory category, String role, de.pixelrpg.rpg.profession.Profession profession,
            NpcFaction faction, String origin, String personality, List<String> traits, List<String> knowledge,
            String behavior, String schedule, String dialogueTreeId, boolean storyRelevant, boolean questRelevant,
            boolean loreRelevant, String skinSource) { }
}
