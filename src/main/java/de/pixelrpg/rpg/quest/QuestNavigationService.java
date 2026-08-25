package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Maintains active quest navigation markers for online players. */
public final class QuestNavigationService {
    private final PixelRPGPlugin plugin;
    private final QuestRepository questRepository;
    private final PlayerProfileManager profileManager;
    private final NpcManager npcManager;
    private final Map<UUID, Map<String, QuestMarker>> markersByPlayer = new HashMap<>();

    public QuestNavigationService(PixelRPGPlugin plugin, QuestRepository questRepository,
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
        if (profile == null || !profile.isRegistered()) {
            clear(player);
            return;
        }

        Map<String, QuestMarker> currentMarkers = markersByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        List<QuestProgressEntry> entries = new ArrayList<>();
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            Quest quest = questRepository.getQuest(progress.getQuestId());
            if (quest != null) entries.add(new QuestProgressEntry(quest, progress));
        }

        // Existing implementation continues below.
    }

    private void cleanupOfflinePlayers() {
        markersByPlayer.keySet().removeIf(uuid -> plugin.getServer().getPlayer(uuid) == null);
    }

    private void clear(Player player) {
        markersByPlayer.remove(player.getUniqueId());
    }

    private record QuestProgressEntry(Quest quest, QuestProgress progress) {}
    private record QuestMarker(String id, BossBar bar, LookAnchor anchor, Component label) {}
}
