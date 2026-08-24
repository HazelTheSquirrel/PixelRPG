package de.pixelrpg.rpg.scoreboard;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.companion.Companion;
import de.pixelrpg.rpg.companion.CompanionService;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardService implements Listener {
    private static final int MAX_LINES = 15;
    private static final int MAX_TRACKED_QUESTS = 2;
    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final int updateIntervalTicks;
    private final Map<UUID, PlayerScoreboardState> stateByPlayer = new ConcurrentHashMap<>();
    private BukkitTask task;

    private static final class PlayerScoreboardState {
        final Scoreboard board;
        final Objective objective;
        final Team[] teams = new Team[MAX_LINES];
        final Component[] lastPrefixes = new Component[MAX_LINES];
        final boolean[] activeLine = new boolean[MAX_LINES];

        PlayerScoreboardState(Scoreboard board, Objective objective) {
            this.board = board;
            this.objective = objective;
        }
    }

    public ScoreboardService(Plugin plugin, PlayerProfileManager profileManager, int updateIntervalTicks) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.updateIntervalTicks = Math.max(1, updateIntervalTicks);
    }

    public void startTask() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                profileManager.getProfile(player.getUniqueId()).ifPresent(profile -> {
                    if (!profile.isRegistered()) {
                        clearScoreboard(player);
                        player.setExp(0.0F);
                        player.setLevel(0);
                    } else if (profile.isScoreboardEnabled()) {
                        apply(player, profile);
                    } else {
                        clearScoreboard(player);
                        updateExperienceBar(player, profile);
                    }
                });
            }
        }, updateIntervalTicks, updateIntervalTicks);
    }

    public void setEnabled(Player player, boolean enabled) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        profile.setScoreboardEnabled(enabled);
        if (enabled) apply(player, profile);
        else clearScoreboard(player);
    }

    public boolean isEnabled(Player player) {
        return profileManager.getProfile(player.getUniqueId()).map(PlayerProfile::isScoreboardEnabled).orElse(false);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) clearScoreboard(player);
        stateByPlayer.clear();
    }

    // Zuständig für den initialen PixelRPG-HUD-Aufbau beim Login.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profileManager.getProfile(event.getPlayer().getUniqueId()).ifPresent(profile -> {
            if (profile.isRegistered() && profile.isScoreboardEnabled()) apply(event.getPlayer(), profile);
        });
    }

    // Zuständig für das Entfernen des zwischengespeicherten HUD-Zustands beim Logout.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stateByPlayer.remove(event.getPlayer().getUniqueId());
    }

    private void clearScoreboard(Player player) {
        PlayerScoreboardState state = stateByPlayer.remove(player.getUniqueId());
        if (state == null) return;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null && player.getScoreboard() == state.board) player.setScoreboard(manager.getMainScoreboard());
    }

    private void apply(Player player, PlayerProfile profile) {
        updateExperienceBar(player, profile);
        List<Component> lines = buildLines(player, profile);
        int size = Math.min(lines.size(), MAX_LINES);
        PlayerScoreboardState state = stateByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> createState(player));

        for (int i = 0; i < size; i++) {
            Component line = lines.get(i);
            Team team = state.teams[i];
            if (team == null) team = registerLineTeam(state, i);
            if (!line.equals(state.lastPrefixes[i])) {
                team.prefix(line);
                state.lastPrefixes[i] = line;
            }
            if (!state.activeLine[i]) {
                state.objective.getScore(entryFor(i)).setScore(MAX_LINES - i);
                state.activeLine[i] = true;
            }
        }

        for (int i = size; i < MAX_LINES; i++) {
            if (state.activeLine[i]) {
                state.board.resetScores(entryFor(i));
                state.activeLine[i] = false;
                state.lastPrefixes[i] = null;
            }
        }
    }

    private void updateExperienceBar(Player player, PlayerProfile profile) {
        long totalExperience = profile.getExperience();
        int level = profile.getLevel();
        long currentLevelStart = Level.getExperienceForCurrentLevel(level);
        long nextLevel = Level.getExperienceForNextLevel(level);
        long intoLevel = Math.max(0L, totalExperience - currentLevelStart);
        long required = Math.max(1L, nextLevel - currentLevelStart);
        float progress = Math.clamp((float) intoLevel / (float) required, 0.0F, 1.0F);
        player.setLevel(level);
        player.setExp(progress);
    }

    private PlayerScoreboardState createState(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) throw new IllegalStateException("Bukkit scoreboard manager is unavailable.");
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("pixelrpg", Criteria.DUMMY, Component.text("PIXELRPG", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        PlayerScoreboardState state = new PlayerScoreboardState(board, objective);
        player.setScoreboard(board);
        return state;
    }

    private Team registerLineTeam(PlayerScoreboardState state, int index) {
        Team team = state.board.registerNewTeam("line" + index);
        team.addEntry(entryFor(index));
        state.teams[index] = team;
        return team;
    }

    private String entryFor(int index) {
        return "\u200B".repeat(index + 1);
    }

    private List<Component> buildLines(Player player, PlayerProfile profile) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text(" "));
        lines.add(Component.text(player.getName(), NamedTextColor.WHITE));
        lines.add(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(profile.getLevel(), NamedTextColor.GOLD)));
        lines.add(Component.text(" "));

        CompanionService companionService = PixelRPGPlugin.getInstance().getCompanionService();
        Companion activeCompanion = companionService == null ? null : companionService.getActive(player.getUniqueId());
        lines.add(Component.text("Companion:", NamedTextColor.GRAY));
        lines.add(Component.text(activeCompanion == null ? "Keiner" : activeCompanion.name(), activeCompanion == null ? NamedTextColor.DARK_GRAY : NamedTextColor.AQUA));
        lines.add(Component.text(" "));

        appendQuestTrackerLines(lines, profile);
        lines.add(Component.text(" "));
        lines.add(Component.text("Tode: ", NamedTextColor.GRAY).append(Component.text(profile.getStatistic("DEATHS"), NamedTextColor.DARK_RED)));
        return lines;
    }

    // Fügt die aktiven Quests mit Ziel und Fortschritt in den rechten PixelRPG-HUD ein.
    private void appendQuestTrackerLines(List<Component> lines, PlayerProfile profile) {
        lines.add(Component.text("Quests:", NamedTextColor.YELLOW));
        QuestManager questManager = PixelRPGPlugin.getInstance().getQuestManager();
        if (questManager == null || profile.getActiveQuests().isEmpty()) {
            lines.add(Component.text("Keine", NamedTextColor.DARK_GRAY));
            return;
        }

        int shown = 0;
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            if (shown >= MAX_TRACKED_QUESTS || lines.size() >= MAX_LINES - 3) break;
            Quest quest = questManager.getRepository().getQuest(progress.getQuestId());
            if (quest == null) continue;
            String title = quest.title();
            if (title.length() > 28) title = title.substring(0, 28) + "…";
            lines.add(Component.text(title, NamedTextColor.WHITE));
            lines.add(Component.text(progress.getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.GREEN));
            shown++;
        }
    }
}
