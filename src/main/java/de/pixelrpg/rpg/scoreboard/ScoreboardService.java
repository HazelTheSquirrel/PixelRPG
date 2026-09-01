package de.pixelrpg.rpg.scoreboard;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.companion.Companion;
import de.pixelrpg.rpg.companion.CompanionService;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.party.Party;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final int updateIntervalTicks;
    private final Map<UUID, PlayerScoreboardState> stateByPlayer = new ConcurrentHashMap<>();
    private BukkitTask task;

    private static final class PlayerScoreboardState {
        final Scoreboard board;
        final Objective objective;
        final Team[] teams = new Team[MAX_LINES];
        List<Component> lastLines = List.of();
        final boolean[] activeLine = new boolean[MAX_LINES];
        final Map<UUID, String> guildTeamByPlayer = new ConcurrentHashMap<>();

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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profileManager.getProfile(event.getPlayer().getUniqueId()).ifPresent(profile -> {
            if (profile.isRegistered() && profile.isScoreboardEnabled()) apply(event.getPlayer(), profile);
        });
    }

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
        PlayerScoreboardState state = stateByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> createState(player));
        if (!lines.equals(state.lastLines)) {
            state.lastLines = List.copyOf(lines);

            int size = Math.min(lines.size(), MAX_LINES);
            for (int i = 0; i < size; i++) {
                Component line = lines.get(i);
                Team team = state.teams[i];
                if (team == null) team = registerLineTeam(state, i);
                team.prefix(line);
                if (!state.activeLine[i]) {
                    state.objective.getScore(entryFor(i)).setScore(MAX_LINES - i);
                    state.activeLine[i] = true;
                }
            }

            for (int i = size; i < MAX_LINES; i++) {
                if (state.activeLine[i]) {
                    state.board.resetScores(entryFor(i));
                    state.activeLine[i] = false;
                }
            }
        }

        // Guild prefixes are separate from sidebar line caching and therefore must be refreshed every tick.
        applyGuildPrefixes(state);
    }

    private void applyGuildPrefixes(PlayerScoreboardState state) {
        GuildManager guildManager;
        try {
            guildManager = GuildManager.getInstance();
        } catch (IllegalStateException ignored) {
            return;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            UUID playerId = onlinePlayer.getUniqueId();
            Guild guild = guildManager.getGuild(playerId).orElse(null);
            String previousTeamName = state.guildTeamByPlayer.get(playerId);

            if (guild == null) {
                if (previousTeamName != null) {
                    Team previous = state.board.getTeam(previousTeamName);
                    if (previous != null) previous.removeEntry(onlinePlayer.getName());
                    state.guildTeamByPlayer.remove(playerId);
                }
                continue;
            }

            String teamName = guildTeamName(guild.id());
            Team team = state.board.getTeam(teamName);
            if (team == null) team = state.board.registerNewTeam(teamName);
            team.prefix(Component.text("[" + guild.name() + "] ", NamedTextColor.GOLD));
            team.addEntry(onlinePlayer.getName());

            if (previousTeamName != null && !previousTeamName.equals(teamName)) {
                Team previous = state.board.getTeam(previousTeamName);
                if (previous != null) previous.removeEntry(onlinePlayer.getName());
            }
            state.guildTeamByPlayer.put(playerId, teamName);
        }
    }

    private String guildTeamName(UUID guildId) {
        return "prg_" + guildId.toString().replace("-", "").substring(0, 11);
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
        objective.numberFormat(NumberFormat.blank());
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
        appendGuildLine(lines, player);
        appendPartyLine(lines, player);
        lines.add(Component.text(" "));
        CompanionService companionService = PixelRPGPlugin.getInstance().getCompanionService();
        Companion activeCompanion = companionService == null ? null : companionService.getActive(player.getUniqueId());
        lines.add(Component.text("Companion:", NamedTextColor.GRAY));
        lines.add(Component.text(activeCompanion == null ? "Keiner" : activeCompanion.name(), activeCompanion == null ? NamedTextColor.DARK_GRAY : NamedTextColor.AQUA));
        lines.add(Component.text(" "));
        lines.add(Component.text("Gold: ", NamedTextColor.GRAY).append(Component.text(formatGold(profile.getMoney()), NamedTextColor.GOLD)));
        lines.add(Component.text("Tode: ", NamedTextColor.GRAY).append(Component.text(profile.getStatistic("DEATHS"), NamedTextColor.DARK_RED)));
        return lines;
    }

    private void appendGuildLine(List<Component> lines, Player player) {
        Guild guild;
        try {
            guild = GuildManager.getInstance().getGuild(player.getUniqueId()).orElse(null);
        } catch (IllegalStateException ignored) {
            guild = null;
        }
        lines.add(Component.text("Gilde: ", NamedTextColor.GRAY)
                .append(Component.text(guild == null ? "Keine" : guild.name(), guild == null ? NamedTextColor.DARK_GRAY : NamedTextColor.GOLD)));
    }

    private void appendPartyLine(List<Component> lines, Player player) {
        Party party = PixelRPGPlugin.getInstance().getPartyManager().getParty(player.getUniqueId()).orElse(null);
        Component line = Component.text("Party: ", NamedTextColor.GRAY);
        if (party == null || party.getMembers().isEmpty()) {
            lines.add(line.append(Component.text("Keine", NamedTextColor.DARK_GRAY)));
            return;
        }
        boolean first = true;
        for (UUID memberId : party.getMembers()) {
            if (!first) line = line.append(Component.text(", ", NamedTextColor.DARK_GRAY));
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            String name = offlinePlayer.getName() == null ? memberId.toString().substring(0, 8) : offlinePlayer.getName();
            Component member = Component.text(name, memberId.equals(player.getUniqueId()) ? NamedTextColor.AQUA : NamedTextColor.WHITE);
            if (offlinePlayer instanceof Player onlinePlayer && onlinePlayer.isOnline()) member = member.hoverEvent(onlinePlayer.asHoverEvent());
            line = line.append(member);
            first = false;
        }
        lines.add(line);
    }

    private String formatGold(double amount) {
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }
}
