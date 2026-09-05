package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Maintains one native Minecraft locator-bar waypoint per active quest. */
public final class QuestNavigationService {
    private static final List<Color> QUEST_COLORS = List.of(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.FUCHSIA);
    private static final NamespacedKey DEFAULT_WAYPOINT_STYLE = NamespacedKey.minecraft("default");
    private static final long TARGET_CACHE_TTL_MILLIS = 30_000L;
    private static final int MAX_TARGET_CACHE_ENTRIES = 2048;

    private final Plugin plugin;
    private final QuestRepository questRepository;
    private final PlayerProfileManager profileManager;
    private final NpcManager npcManager;
    private final Map<UUID, Map<String, QuestMarker>> markersByPlayer = new HashMap<>();
    private final Map<UUID, RefreshState> refreshStateByPlayer = new HashMap<>();
    private final Map<NavigationCacheKey, CachedTarget> targetCache = new LinkedHashMap<>(64, 0.75F, true);

    public QuestNavigationService(Plugin plugin, QuestRepository questRepository,
                                  PlayerProfileManager profileManager, NpcManager npcManager) {
        this.plugin = plugin;
        this.questRepository = questRepository;
        this.profileManager = profileManager;
        this.npcManager = npcManager;
    }

    /** Refreshes every active quest's locator-bar target for one player. */
    public synchronized void refresh(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            clear(player);
            return;
        }

        UUID playerId = player.getUniqueId();
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
            if (quest != null) entries.add(new QuestProgressEntry(quest, progress));
        }
        entries.sort(java.util.Comparator.comparing(entry -> entry.quest().id()));

        Map<String, Integer> visibleQuestIndexes = new HashMap<>();
        for (int index = 0; index < entries.size() && index < 5; index++) visibleQuestIndexes.put(entries.get(index).quest().id(), index);

        for (int index = 0; index < entries.size() && index < 5; index++) {
            QuestProgressEntry entry = entries.get(index);
            Location target = resolveTarget(playerLocation, entry.quest(), entry.progress());
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
        trimTargetCache();
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
        targetCache.clear();
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
            stand.setWaypointStyle(DEFAULT_WAYPOINT_STYLE);
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
        if (quest.targetStructureKey() == null && quest.targetBiomeKeys().isEmpty()) return quest.reachLocation();

        NavigationCacheKey cacheKey = new NavigationCacheKey(
                origin.getWorld().getUID(),
                origin.getBlockX() >> 4,
                origin.getBlockZ() >> 4,
                quest.id());
        long now = System.currentTimeMillis();
        CachedTarget cached = targetCache.get(cacheKey);
        if (cached != null && now - cached.createdAtMillis() <= TARGET_CACHE_TTL_MILLIS) return cached.location().clone();

        Location target = findWorldTarget(origin, quest);
        if (target != null) {
            targetCache.put(cacheKey, new CachedTarget(target.clone(), now));
            trimTargetCache();
        }
        return target;
    }

    private Location findWorldTarget(Location origin, Quest quest) {
        if (quest.targetStructureKey() != null && !quest.targetStructureKey().isBlank()) {
            var registry = io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.STRUCTURE);
            var key = NamespacedKey.fromString(quest.targetStructureKey());
            if (key != null) {
                var structure = registry.get(key);
                if (structure != null) {
                    var result = origin.getWorld().locateNearestStructure(origin, structure, quest.navigationRadius(), false);
                    if (result != null) return result.getLocation();
                }
            }
        }

        if (!quest.targetBiomeKeys().isEmpty()) {
            var registry = io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.BIOME);
            var biomes = quest.targetBiomeKeys().stream()
                    .map(NamespacedKey::fromString)
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

    private void trimTargetCache() {
        while (targetCache.size() > MAX_TARGET_CACHE_ENTRIES) {
            var iterator = targetCache.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            } else return;
        }
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
    private record NavigationCacheKey(UUID worldId, int chunkX, int chunkZ, String questId) { }
    private record CachedTarget(Location location, long createdAtMillis) { }
    private record RefreshState(UUID worldId, int chunkX, int chunkZ, long mutationRevision) { }
}
