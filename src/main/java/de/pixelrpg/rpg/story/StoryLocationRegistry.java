package de.pixelrpg.rpg.story;

import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestRepository;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestType;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import java.util.Collection;
import java.util.Optional;

/**
 * Resolves already-generated structures from loaded chunks and creates persistent story NPCs.
 *
 * No locateNearestStructure call is used here. Structure detection is limited to chunks
 * the player is already entering and only runs while a matching story quest is active.
 */
public final class StoryLocationRegistry {
    private final Plugin plugin;
    private final StoryManager storyManager;
    private final PlayerProfileManager profileManager;
    private final QuestRepository questRepository;
    private final QuestManager questManager;
    private final NpcManager npcManager;

    public StoryLocationRegistry(Plugin plugin, StoryManager storyManager,
                                 PlayerProfileManager profileManager, QuestRepository questRepository, QuestManager questManager,
                                 NpcManager npcManager) {
        this.plugin = plugin;
        this.storyManager = storyManager;
        this.profileManager = profileManager;
        this.questRepository = questRepository;
        this.questManager = questManager;
        this.npcManager = npcManager;
    }

    public void checkPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        Chunk chunk = player.getLocation().getChunk();
        checkChunk(player, chunk);
    }

    public void checkChunk(Player player, Chunk chunk) {
        if (player == null || chunk == null || !chunk.isLoaded()) return;

        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;

        Collection<GeneratedStructure> structures = chunk.getStructures();
        if (structures.isEmpty()) return;

        for (StoryChapter chapter : storyManager.getAllChapters()) {
            if (!chapter.hasStructureTrigger() || profile.getLevel() < chapter.requiredLevel()) continue;
            if (!hasActiveStoryQuest(profile, chapter)) continue;

            for (GeneratedStructure structure : structures) {
                if (!chapter.structureTrigger().equals(structureKey(structure))) continue;
                RPGNpc npc = ensureStoryNpc(chapter, structure, chunk.getWorld());
                if (npc != null) {
                    markStructureQuestsReached(player, chapter);
                    plugin.getLogger().fine("Story NPC active: " + npc.id() + " at " + npc.location());
                }
            }
        }
    }

    private void markStructureQuestsReached(Player player, StoryChapter chapter) {
        for (String questId : chapter.questIds()) {
            Quest quest = questRepository.getQuest(questId);
            if (quest == null || quest.type() != QuestType.REACH_LOCATION) continue;
            questManager.markReachLocationReached(player, quest.id(), chapter.structureTrigger());
        }
    }

    private boolean hasActiveStoryQuest(PlayerProfile profile, StoryChapter chapter) {
        for (String questId : chapter.questIds()) {
            Quest quest = questRepository.getQuest(questId);
            if (quest != null && profile.hasActiveQuest(quest.id())) return true;
        }
        return false;
    }

    private RPGNpc ensureStoryNpc(StoryChapter chapter, GeneratedStructure structure, World world) {
        Location location = findSpawnLocation(structure, world);
        if (location == null) return null;

        String id = stableNpcId(chapter, location);
        Optional<RPGNpc> existing = npcManager.getById(id);
        if (existing.isPresent()) return existing.get();

        return npcManager.createWithId(id, NpcType.STORY, displayName(chapter), location, null, null);
    }

    private Location findSpawnLocation(GeneratedStructure structure, World world) {

        double centerX = structure.getBoundingBox().getCenterX();
        double centerY = structure.getBoundingBox().getCenterY();
        double centerZ = structure.getBoundingBox().getCenterZ();

        int baseX = (int) Math.floor(centerX);
        int baseY = (int) Math.floor(centerY);
        int baseZ = (int) Math.floor(centerZ);

        for (int radius = 0; radius <= 4; radius++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        int x = baseX + dx;
                        int y = baseY + dy;
                        int z = baseZ + dz;
                        Material feet = world.getBlockAt(x, y, z).getType();
                        Material head = world.getBlockAt(x, y + 1, z).getType();
                        Material floor = world.getBlockAt(x, y - 1, z).getType();
                        if (feet.isAir() && head.isAir() && !floor.isAir() && floor.isSolid()) {
                            return new Location(world, x + 0.5D, y, z + 0.5D);
                        }
                    }
                }
            }
        }
        return null;
    }

    private String structureKey(GeneratedStructure structure) {
        var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
        var key = registry.getKey(structure.getStructure());
        return key == null ? "" : key.toString().toLowerCase();
    }

    private String stableNpcId(StoryChapter chapter, Location location) {
        return "story_" + chapter.npcId().toLowerCase().replaceAll("[^a-z0-9_-]", "_")
                + "_" + location.getWorld().getUID()
                + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
    }

    private String displayName(StoryChapter chapter) {
        return switch (chapter.npcId().toLowerCase()) {
            case "eryn" -> "Eryn";
            case "the_archivist" -> "Der Archivar";
            case "vael" -> "Vael";
            case "mara" -> "Mara";
            case "oren" -> "Oren";
            case "silex" -> "Silex";
            case "kael" -> "Kael";
            case "lyra" -> "Lyra";
            default -> chapter.title();
        };
    }
}
