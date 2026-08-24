package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Maintains one native Minecraft locator-bar waypoint per active quest. */
public final class QuestNavigationService {
    private static final List<Color> QUEST_COLORS = List.of(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.FUCHSIA);

    private final Plugin plugin;
    private final QuestRepository questRepository;
    private final PlayerProfileManager profileManager;
    private final NpcManager npcManager;
    private final Map<UUID, Map<String, QuestMarker>> markersByPlayer = new HashMap<>();

    public QuestNavigationService(Plugin plugin, QuestRepository questRepository,
                                  PlayerProfileManager profileManager, NpcManager npcManager) {
        this.plugin = plugin;
        this.questRepository = questRepository;
        this.profileManager = profileManager;
        this.npcManager = npcManager;
    }

    /** Refreshes every active quest's locator-bar target for one player. */
    public void refresh(Player player) {
        cleanupOfflinePlayers();
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            clear(player);
            return;
        }

        Map<String, QuestMarker> currentMarkers = markersByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        List<QuestProgressEntry> entries = new ArrayList<>();
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            Quest quest = questRepository.getQuest(progress.getQuestId());
            if (quest != null) entries.add(new QuestProgressEntry(quest, progress));
        }
        entries.sort(java.util.Comparator.comparing(entry -> entry.quest().id()));

        Map<String, Integer> visibleQuestIndexes = new HashMap<>();
        for (int index = 0; index < entries.size() && index < 5; index++) visibleQuestIndexes.put(entries.get(index).quest().id(), index);

        for (int index = 0; index < entries.size() && index < 5; index++) {
            QuestProgressEntry entry = entries.get(index);
            Location target = resolveTarget(player.getLocation(), entry.quest(), entry.progress());
            if (target == null || target.getWorld() == null) {
                removeMarker(currentMarkers, entry.quest().id());
                continue;
            }

            QuestMarker marker = currentMarkers.get(entry.quest().id());
            if (marker == null || !marker.entity().isValid()) {
                marker = createMarker(player, index, target);
                if (marker == null) continue;
                currentMarkers.put(entry.quest().id(), marker);
            } else if (!sameLocation(marker.entity().getLocation(), target)) {
                marker.entity().teleport(target);
            }
            marker.entity().setWaypointColor(QUEST_COLORS.get(index));
            marker.entity().setWaypointStyle(Key.key("minecraft:default"));
        }

        currentMarkers.keySet().removeIf(id -> {
            if (visibleQuestIndexes.containsKey(id)) return false;
            QuestMarker marker = currentMarkers.get(id);
            if (marker != null) marker.entity().remove();
            return true;
        });
    }

    /** Removes marker state for players that are no longer online. */
    public void cleanupOfflinePlayers() {
        markersByPlayer.keySet().removeIf(uuid -> {
            if (Bukkit.getPlayer(uuid) != null) return false;
            Map<String, QuestMarker> markers = markersByPlayer.get(uuid);
            if (markers != null) markers.values().forEach(marker -> marker.entity().remove());
            return true;
        });
    }

    /** Removes all quest waypoints and any legacy PixelRPG quest compass from one player. */
    public void clear(Player player) {
        Map<String, QuestMarker> markers = markersByPlayer.remove(player.getUniqueId());
        if (markers != null) markers.values().forEach(marker -> marker.entity().remove());
        removeLegacyCompass(player);
    }

    public void clearAll() {
        for (Map<String, QuestMarker> markers : markersByPlayer.values()) markers.values().forEach(marker -> marker.entity().remove());
        markersByPlayer.clear();
    }

    private QuestMarker createMarker(Player player, int index, Location target) {
        ArmorStand marker = target.getWorld().spawn(target, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setPersistent(false);
            stand.setVisibleByDefault(false);
            stand.setWaypointColor(QUEST_COLORS.get(index));
            stand.setWaypointStyle(Key.key("minecraft:default"));
            stand.getPersistentDataContainer().set(RPGKeys.Quest.navigationCompass(), PersistentDataType.BYTE, (byte) 1);
        });
        player.showEntity(plugin, marker);
        return new QuestMarker(marker);
    }

    private Location resolveTarget(Location origin, Quest quest, QuestProgress progress) {
        if (progress.getCurrentAmount() >= quest.requiredAmount()) return resolveQuestGiver(quest);
        return switch (quest.type()) {
            case TALK_TO_NPC -> resolveNpc(quest.targetKey());
            case HUNT, COLLECT, REACH_LOCATION -> resolveWorldTarget(origin, quest);
            case GLOBAL_EVENT -> null;
        };
    }

    private Location resolveQuestGiver(Quest quest) {
        return quest.questGiverNpcId() == null || quest.questGiverNpcId().isBlank()
                ? null
                : npcManager.getById(quest.questGiverNpcId()).map(npc -> npc.location()).orElse(null);
    }

    private Location resolveNpc(String targetKey) {
        if (targetKey == null || targetKey.isBlank()) return null;
        return npcManager.getById(targetKey).map(npc -> npc.location()).orElse(null);
    }

    private Location resolveWorldTarget(Location origin, Quest quest) {
        if (quest.targetStructureKey() != null && !quest.targetStructureKey().isBlank()) {
            var registry = io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.STRUCTURE);
            var key = org.bukkit.NamespacedKey.fromString(quest.targetStructureKey());
            if (key != null) {
                var structure = registry.get(key);
                if (structure != null) {
                    var result = origin.getWorld().locateNearestStructure(origin, structure, quest.navigationRadius(), false);
                    if (result != null) return result.getLocation();
                }
            }
        }

        if (!quest.targetBiomeKeys().isEmpty()) {
            var registry = io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papmc.paper.registry.RegistryKey.BIOME);
            var biomes = quest.targetBiomeKeys().stream()
                    .map(org.bukkit.NamespacedKey::fromString)
                    .filter(java.util.Objects::nonNull)
                    .map(registry::get)
                    .filter(java.util.Objects::nonNull)
                    .toArray(org.bukkit.block.Biome[]::new);
            if (biomes.length > 0) {
                var result = origin.getWorld().locateNearestBiome(origin, quest.navigationRadius(), biomes);
                if (result != null) return result.getLocation();
            }
        }

        return quest.reachLocation();
    }

    private void removeMarker(Map<String, QuestMarker> markers, String questId) {
        QuestMarker marker = markers.remove(questId);
        if (marker != null) marker.entity().remove();
    }

    private boolean sameLocation(Location first, Location second) {
        return first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private void removeLegacyCompass(Player player) {
        for (var item : player.getInventory().getContents()) {
            if (item == null || item.getType() != org.bukkit.Material.RECOVERY_COMPASS) continue;
            var meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(RPGKeys.Quest.navigationCompass(), PersistentDataType.BYTE)) item.setAmount(0);
        }
    }

    private record QuestMarker(ArmorStand entity) { }
    private record QuestProgressEntry(Quest quest, QuestProgress progress) { }
}
