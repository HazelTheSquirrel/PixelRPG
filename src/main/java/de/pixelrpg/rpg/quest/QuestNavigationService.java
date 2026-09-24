package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.story.StoryManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.GameRules;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Maintains one native Minecraft locator-bar waypoint per active quest. */
public final class QuestNavigationService {
    private static final List<Color> QUEST_COLORS = List.of(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.FUCHSIA);
    private static final Color STORY_QUEST_COLOR = Color.FUCHSIA;
    private static final NamespacedKey DEFAULT_WAYPOINT_STYLE = NamespacedKey.minecraft("default");

    private final Plugin plugin;
    private final QuestRepository questRepository;
    private final PlayerProfileManager profileManager;
    private final NpcManager npcManager;
    private final Set<String> storyQuestIds;
    private final Map<UUID, Map<String, QuestMarker>> markersByPlayer = new HashMap<>();
    private final Map<UUID, RefreshState> refreshStateByPlayer = new HashMap<>();

    public QuestNavigationService(Plugin plugin, QuestRepository questRepository,
                                  PlayerProfileManager profileManager, NpcManager npcManager, StoryManager storyManager) {
        this.plugin = plugin;
        this.questRepository = questRepository;
        this.profileManager = profileManager;
        this.npcManager = npcManager;
        this.storyQuestIds = storyManager.getAllChapters().stream()
                .flatMap(chapter -> chapter.questIds().stream())
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** Refreshes every active quest's locator-bar target for one player. */
    public synchronized void refresh(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            clear(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRules.LOCATOR_BAR))) {
            player.getWorld().setGameRule(GameRules.LOCATOR_BAR, true);
        }
        Map<String, QuestMarker> currentMarkers = markersByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>());
        Location playerLocation = player.getLocation();
        UUID worldId = playerLocation.getWorld() == null ? null : playerLocation.getWorld().getUID();
        RefreshState previousState = refreshStateByPlayer.get(playerId);
        if (worldId != null && previousState != null
                && previousState.worldId().equals(worldId)
                && previousState.chunkX() == (playerLocation.getBlockX() >> 4)
                && previousState.chunkZ() == (playerLocation.getBlockZ() >> 4)
                && previousState.mutationRevision() == profile.getMutationRevision()
                && currentMarkers.values().stream().allMatch(marker -> marker.entity().isValid())) {
            return;
        }

        List<QuestProgressEntry> entries = new ArrayList<>();
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            Quest quest = questRepository.getQuest(progress.getQuestId());
            if (quest != null && isNavigationQuest(quest)) entries.add(new QuestProgressEntry(quest, progress));
        }
        entries.sort(java.util.Comparator
                .comparing((QuestProgressEntry entry) -> !storyQuestIds.contains(entry.quest().id()))
                .thenComparing(entry -> entry.quest().id()));

        Map<String, Integer> visibleQuestIndexes = new HashMap<>();
        for (int index = 0; index < entries.size() && index < 5; index++) visibleQuestIndexes.put(entries.get(index).quest().id(), index);

        for (int index = 0; index < entries.size() && index < 5; index++) {
            QuestProgressEntry entry = entries.get(index);
            Location target = resolveTarget(playerLocation, profile, entry.quest(), entry.progress());
            if (target == null || target.getWorld() == null) {
                removeMarker(currentMarkers, entry.quest().id());
                continue;
            }

            QuestMarker marker = currentMarkers.get(entry.quest().id());
            if (marker == null || !marker.entity().isValid()) {
                marker = createMarker(player, index, target, storyQuestIds.contains(entry.quest().id()));
                if (marker == null) continue;
                currentMarkers.put(entry.quest().id(), marker);
            } else if (!sameLocation(marker.entity().getLocation(), target)) {
                marker.entity().teleport(target);
            }
            marker.entity().setWaypointColor(storyQuestIds.contains(entry.quest().id())
                    ? STORY_QUEST_COLOR
                    : QUEST_COLORS.get(index));
            marker.entity().setWaypointStyle(DEFAULT_WAYPOINT_STYLE);
        }

        currentMarkers.keySet().removeIf(id -> {
            if (visibleQuestIndexes.containsKey(id)) return false;
            QuestMarker marker = currentMarkers.get(id);
            if (marker != null) marker.entity().remove();
            return true;
        });

        if (worldId != null) {
            refreshStateByPlayer.put(playerId, new RefreshState(worldId, playerLocation.getBlockX() >> 4,
                    playerLocation.getBlockZ() >> 4, profile.getMutationRevision()));
        }
    }

    /** Removes marker state for players that are no longer online. */
    public synchronized void cleanupOfflinePlayers() {
        markersByPlayer.keySet().removeIf(uuid -> {
            if (Bukkit.getPlayer(uuid) != null) return false;
            Map<String, QuestMarker> markers = markersByPlayer.get(uuid);
            if (markers != null) markers.values().forEach(marker -> marker.entity().remove());
            refreshStateByPlayer.remove(uuid);
            return true;
        });
    }

    /** Removes all quest markers and cached navigation targets for one player. */
    public synchronized void clear(Player player) {
        Map<String, QuestMarker> markers = markersByPlayer.remove(player.getUniqueId());
        if (markers != null) markers.values().forEach(marker -> marker.entity().remove());
        refreshStateByPlayer.remove(player.getUniqueId());
        removeLegacyCompass(player);
    }

    public synchronized void clearAll() {
        for (Map<String, QuestMarker> markers : markersByPlayer.values()) markers.values().forEach(marker -> marker.entity().remove());
        markersByPlayer.clear();
        refreshStateByPlayer.clear();
    }

    private boolean isNavigationQuest(Quest quest) {
        return quest.type() == QuestType.REACH_LOCATION || storyQuestIds.contains(quest.id());
    }

    private QuestMarker createMarker(Player player, int index, Location target, boolean storyQuest) {
        Mannequin marker = target.getWorld().spawn(target, Mannequin.class, mannequin -> {
            mannequin.setInvisible(true);
            mannequin.setNoPhysics(true);
            mannequin.setImmovable(true);
            mannequin.setCollidable(false);
            mannequin.setInvulnerable(true);
            mannequin.setSilent(true);
            mannequin.setPersistent(false);
            mannequin.setRemoveWhenFarAway(false);
            mannequin.setVisibleByDefault(false);
            var transmitRange = mannequin.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
            if (transmitRange != null) transmitRange.setBaseValue(60_000_000.0D);
            mannequin.setWaypointColor(storyQuest ? STORY_QUEST_COLOR : QUEST_COLORS.get(index));
            mannequin.setWaypointStyle(DEFAULT_WAYPOINT_STYLE);
            mannequin.getPersistentDataContainer().set(RPGKeys.Quest.navigationCompass(), PersistentDataType.BYTE, (byte) 1);
        });
        player.showEntity(plugin, marker);
        return new QuestMarker(marker);
    }

    private Location resolveTarget(Location origin, PlayerProfile profile, Quest quest, QuestProgress progress) {
        if (progress.getCurrentAmount() >= quest.requiredAmount()) return resolveQuestGiver(quest);
        return switch (quest.type()) {
            case TALK_TO_NPC -> resolveNpc(origin, quest.targetKey());
            case HUNT, COLLECT -> null;
            case REACH_LOCATION -> resolveWorldTarget(profile, quest);
            case GLOBAL_EVENT -> null;
        };
    }

    private Location resolveQuestGiver(Quest quest) {
        return quest.questGiverNpcId() == null || quest.questGiverNpcId().isBlank()
                ? null
                : npcManager.getById(quest.questGiverNpcId()).map(npc -> npc.location()).orElse(null);
    }

    private Location resolveNpc(Location origin, String targetKey) {
        if (targetKey == null || targetKey.isBlank()) return null;
        Location staticTarget = npcManager.getById(targetKey).map(npc -> npc.location()).orElse(null);
        if (staticTarget != null) return staticTarget;

        String storyPrefix = "story_" + targetKey.toLowerCase(java.util.Locale.ROOT) + "_";
        return npcManager.getAll().stream()
                .filter(npc -> npc.type() == de.pixelrpg.rpg.npc.NpcType.STORY)
                .filter(npc -> npc.id().toLowerCase(java.util.Locale.ROOT).startsWith(storyPrefix))
                .map(de.pixelrpg.rpg.npc.RPGNpc::location)
                .filter(location -> location.getWorld() != null && origin.getWorld() != null)
                .filter(location -> location.getWorld().equals(origin.getWorld()))
                .min(java.util.Comparator.comparingDouble(location -> location.distanceSquared(origin)))
                .orElse(null);
    }

    private Location resolveWorldTarget(PlayerProfile profile, Quest quest) {
        PlayerProfile.NavigationTarget cached = profile.getQuestNavigationTarget(quest.id());
        if (cached == null) return quest.reachLocation();
        if (cached.worldId() == null) return null;
        org.bukkit.World world = Bukkit.getWorld(cached.worldId());
        if (world == null) return null;
        return new Location(world, cached.x(), cached.y(), cached.z());
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

    private record QuestMarker(Mannequin entity) { }
    private record QuestProgressEntry(Quest quest, QuestProgress progress) { }
    private record RefreshState(UUID worldId, int chunkX, int chunkZ, long mutationRevision) { }
}
