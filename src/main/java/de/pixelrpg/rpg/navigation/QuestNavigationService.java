package de.pixelrpg.rpg.navigation;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.Comparator;

/** Keeps the vanilla compass synchronized with the player's active PixelRPG quest navigation target. */
public final class QuestNavigationService implements Listener {
    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final QuestManager questManager;

    public QuestNavigationService(Plugin plugin, PlayerProfileManager profileManager, QuestManager questManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.questManager = questManager;
    }

    /** Refreshes compass navigation for a player after a quest-state change. */
    public void refresh(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered() || !profile.isQuestTrackerEnabled()) {
            resetCompass(player);
            return;
        }

        QuestNavigationTarget target = findTarget(profile);
        if (target == null || target.location() == null || !player.getWorld().getUID().equals(target.location().getWorld().getUID())) {
            resetCompass(player);
            return;
        }
        player.setCompassTarget(target.location());
    }

    /** Selects an active quest for navigation and persists the existing target representation. */
    public boolean track(Player player, String questId) {
        if (player == null || questId == null || questId.isBlank()) return false;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered() || !profile.hasActiveQuest(questId)) return false;
        if (profile.getQuestNavigationTarget(questId) == null) {
            Quest quest = questManager.getRepository().getQuest(questId);
            if (quest == null || !quest.hasNavigationTarget()) return false;
        }
        profile.setQuestTrackerEnabled(true);
        refresh(player);
        return true;
    }

    /** Enables or disables the persistent quest tracker and immediately reconciles compass state. */
    public void setEnabled(Player player, boolean enabled) {
        if (player == null) return;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        profile.setQuestTrackerEnabled(enabled);
        refresh(player);
    }

    public boolean isEnabled(Player player) {
        return player != null && profileManager.getProfile(player.getUniqueId())
                .map(PlayerProfile::isQuestTrackerEnabled)
                .orElse(false);
    }

    private QuestNavigationTarget findTarget(PlayerProfile profile) {
        return profile.getActiveQuests().entrySet().stream()
                .map(entry -> {
                    PlayerProfile.NavigationTarget target = profile.getQuestNavigationTarget(entry.getKey());
                    Quest quest = questManager.getRepository().getQuest(entry.getKey());
                    return target == null || quest == null ? null : new QuestNavigationTarget(quest, target);
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt((QuestNavigationTarget value) -> questManager.isStoryQuest(value.quest()) ? 0 : 1)
                        .thenComparing(value -> value.quest().id(), String.CASE_INSENSITIVE_ORDER))
                .findFirst()
                .orElse(null);
    }

    private void resetCompass(Player player) {
        Location current = player.getLocation();
        if (current.getWorld() != null) player.setCompassTarget(current);
    }

    // Rebuilds quest navigation when a player joins with persisted quest state.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> refresh(event.getPlayer()));
    }

    // A world change can invalidate the current quest target's world, so navigation is reconciled.
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        refresh(event.getPlayer());
    }

    // Quest completion removes the active target and therefore requires a compass reset or next target.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        refresh(event.getPlayer());
    }

    private record QuestNavigationTarget(Quest quest, PlayerProfile.NavigationTarget location) { }
}
