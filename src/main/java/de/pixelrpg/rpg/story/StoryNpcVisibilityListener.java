package de.pixelrpg.rpg.story;

import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestRepository;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShowEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class StoryNpcVisibilityListener implements Listener {
    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final QuestRepository questRepository;
    private final StoryManager storyManager;
    private final NpcManager npcManager;
    private final Map<UUID, Set<String>> visibleNpcIds = new ConcurrentHashMap<>();
    private final Consumer<UUID> profileChangeListener = this::queueRefresh;

    public StoryNpcVisibilityListener(Plugin plugin, PlayerProfileManager profileManager,
                                      QuestRepository questRepository, StoryManager storyManager,
                                      NpcManager npcManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.questRepository = questRepository;
        this.storyManager = storyManager;
        this.npcManager = npcManager;
        profileManager.addProfileChangeListener(profileChangeListener);
    }

    // Rebuilds story visibility shortly after the player has been fully placed in the world.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> refresh(event.getPlayer()), 3L);
    }

    // Re-evaluates nearby story NPCs when a player changes dimensions.
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        refresh(event.getPlayer());
    }

    // Re-evaluates visibility only when the player crosses a chunk boundary.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || !event.hasChangedBlock()) return;
        if (from.getWorld() == to.getWorld()
                && (from.getBlockX() >> 4) == (to.getBlockX() >> 4)
                && (from.getBlockZ() >> 4) == (to.getBlockZ() >> 4)) return;
        refresh(event.getPlayer());
    }

    // Prevents unauthorized tracking of story NPC entities before a client can see them.
    @EventHandler
    public void onTrack(PlayerTrackEntityEvent event) {
        RPGNpc npc = npcManager.getByEntity(event.getEntity().getUniqueId()).orElse(null);
        if (npc != null && npc.type() == NpcType.STORY && !canSee(event.getPlayer(), npc)) {
            event.setCancelled(true);
        }
    }

    // Defensively removes a story NPC if another visibility source tries to show it to an unauthorized player.
    @EventHandler
    public void onShow(PlayerShowEntityEvent event) {
        RPGNpc npc = npcManager.getByEntity(event.getEntity().getUniqueId()).orElse(null);
        if (npc != null && npc.type() == NpcType.STORY && !canSee(event.getPlayer(), npc)) {
            event.getPlayer().hideEntity(plugin, event.getEntity());
            visibleNpcIds.computeIfPresent(event.getPlayer().getUniqueId(), (ignored, ids) -> {
                Set<String> copy = new HashSet<>(ids);
                copy.remove(npc.id());
                return copy.isEmpty() ? null : Set.copyOf(copy);
            });
        }
    }

    // Drops the per-player visibility cache when a player leaves the server.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        visibleNpcIds.remove(event.getPlayer().getUniqueId());
    }

    public void shutdown() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            hidePreviouslyVisible(player);
        }
        visibleNpcIds.clear();
        profileManager.removeProfileChangeListener(profileChangeListener);
    }

    private void queueRefresh(UUID uuid) {
        if (uuid == null || !plugin.isEnabled()) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) refresh(player);
        });
    }

    private void refresh(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            hidePreviouslyVisible(player);
            return;
        }

        Set<String> visibleNow = new HashSet<>();
        Chunk center = player.getLocation().getChunk();
        for (int dx=-1; dx<=1; dx++) {
            for (int dz=-1; dz<=1; dz++) {
                for (RPGNpc npc : npcManager.getStoryNpcsInChunk(center.getWorld(), center.getX()+dx, center.getZ()+dz)) {
                    Entity entity = npcManager.getSpawnedEntity(npc.id()).orElse(null);
                    if (entity == null || !entity.isValid()) continue;
                    if (canSee(profile, npc)) {
                        player.showEntity(plugin, entity);
                        visibleNow.add(npc.id());
                    } else {
                        player.hideEntity(plugin, entity);
                    }
                }
            }
        }

        Set<String> previous = visibleNpcIds.put(player.getUniqueId(), Set.copyOf(visibleNow));
        if (previous != null) {
            for (String npcId : previous) {
                if (visibleNow.contains(npcId)) continue;
                npcManager.getSpawnedEntity(npcId).ifPresent(entity -> player.hideEntity(plugin, entity));
            }
        }
    }

    private void hidePreviouslyVisible(Player player) {
        Set<String> previous = visibleNpcIds.remove(player.getUniqueId());
        if (previous == null) return;
        for (String npcId : previous) {
            npcManager.getSpawnedEntity(npcId).ifPresent(entity -> player.hideEntity(plugin, entity));
        }
    }

    private boolean canSee(Player player, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        return profile != null && profile.isRegistered() && canSee(profile, npc);
    }

    private boolean canSee(PlayerProfile profile, RPGNpc npc) {
        for (StoryChapter chapter : storyManager.getAllChapters()) {
            if (!npc.id().startsWith("story_" + chapter.npcId().toLowerCase() + "_")) continue;
            for (String questId : chapter.questIds()) {
                Quest quest = questRepository.getQuest(questId);
                if (quest != null && profile.hasActiveQuest(quest.id())) return true;
            }
        }
        return false;
    }
}
