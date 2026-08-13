// src/main/java/de/pixelrpg/rpg/scoreboard/ScoreboardService.java (VOLLSTÄNDIG, ersetzt alte Datei — Quest-Tracker-Sektion ergänzt)
package de.pixelrpg.rpg.scoreboard;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
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
    private static final int MAX_TRACKED_QUESTS = 3;

    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final int updateIntervalTicks;

    private final Map<UUID, PlayerScoreboardState> stateByPlayer = new ConcurrentHashMap<>();

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
        this.updateIntervalTicks = updateIntervalTicks;
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                profileManager.getProfile(player.getUniqueId()).ifPresent(profile -> {
                    if (profile.isRegisteredInGuild() && profile.isScoreboardEnabled()) {
                        apply(player, profile);
                    } else if (!profile.isRegisteredInGuild()) {
                        clear(player);
                    }
                });
            }
        }, updateIntervalTicks, updateIntervalTicks);
    }

    // Zuständig für den initialen Scoreboard-Aufbau beim Login, falls der
    // Spieler bereits registriert ist und das HUD aktiviert hat.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profileManager.getProfile(event.getPlayer().getUniqueId()).ifPresent(profile -> {
            if (profile.isRegisteredInGuild() && profile.isScoreboardEnabled()) {
                apply(event.getPlayer(), profile);
            }
        });
    }

    // Zuständig für das Entfernen des zwischengespeicherten Scoreboard-Zustands
    // beim Logout, damit die Map nicht dauerhaft wächst (Memory-Leak-Schutz).
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stateByPlayer.remove(event.getPlayer().getUniqueId());
    }

    public void toggle(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            return;
        }

        boolean newState = !profile.isScoreboardEnabled();
        profile.setScoreboardEnabled(newState);
        profileManager.saveProfileAsync(player.getUniqueId());

        if (newState) {
            apply(player, profile);
        } else {
            clear(player);
        }
    }

    private void clear(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
        stateByPlayer.remove(player.getUniqueId());
    }

    private void apply(Player player, PlayerProfile profile) {
        List<Component> lines = buildLines(player, profile);
        int size = Math.min(lines.size(), MAX_LINES);

        PlayerScoreboardState state = stateByPlayer.computeIfAbsent(player.getUniqueId(),
                uuid -> createState(player));

        for (int i = 0; i < size; i++) {
            Component line = lines.get(i);
            Team team = state.teams[i];

            if (team == null) {
                team = registerLineTeam(state, i);
            }

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

    private PlayerScoreboardState createState(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("pixelrpg", Criteria.DUMMY,
                Component.text("PixelRPG", NamedTextColor.GOLD));
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
        return "\u00A7" + Integer.toHexString(index) + "\u00A7r";
    }

    private List<Component> buildLines(Player player, PlayerProfile profile) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Rank: ", NamedTextColor.GRAY).append(profile.getRank().displayName()));
        lines.add(Component.text("Class: ", NamedTextColor.GRAY).append(profile.getPlayerClass().displayName()));
        lines.add(Component.text("Gold: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.2f", profile.getMoney()), NamedTextColor.YELLOW)));
        lines.add(Component.text("Kills: ", NamedTextColor.GRAY)
                .append(Component.text(profile.getStatistic("MOBS_KILLED"), NamedTextColor.RED)));
        lines.add(Component.text("Deaths: ", NamedTextColor.GRAY)
                .append(Component.text(profile.getStatistic("DEATHS"), NamedTextColor.DARK_RED)));

        if (profile.isQuestTrackerEnabled() && !profile.getActiveQuests().isEmpty() && lines.size() < MAX_LINES) {
            appendQuestTrackerLines(lines, profile);
        }

        if (profile.isPartyHudEnabled()) {
            PartyAPI partyAPI = Bukkit.getServicesManager().load(PartyAPI.class);
            if (partyAPI != null && partyAPI.isInParty(player.getUniqueId())) {
                if (lines.size() < MAX_LINES) {
                    lines.add(Component.text(" "));
                }
                if (lines.size() < MAX_LINES) {
                    lines.add(Component.text("-- Party --", NamedTextColor.LIGHT_PURPLE));
                }

                for (UUID memberUuid : partyAPI.getPartyMembers(player.getUniqueId())) {
                    if (lines.size() >= MAX_LINES) {
                        break;
                    }
                    Player member = Bukkit.getPlayer(memberUuid);
                    if (member == null || !member.isOnline()) {
                        continue;
                    }
                    var healthAttribute = member.getAttribute(Attribute.MAX_HEALTH);
                    double maxHealth = healthAttribute != null ? healthAttribute.getValue() : member.getHealth();
                    boolean isSelf = memberUuid.equals(player.getUniqueId());
                    NamedTextColor nameColor = isSelf ? NamedTextColor.GOLD : NamedTextColor.WHITE;
                    lines.add(Component.text(member.getName() + ": ", nameColor)
                            .append(Component.text((int) member.getHealth() + "/" + (int) maxHealth + "❤", NamedTextColor.RED)));
                }
            }
        }

        return lines;
    }

    // Fügt bis zu MAX_TRACKED_QUESTS aktive Quests mit Fortschritt zur Sidebar hinzu.
    // Quelle der Quest-Titel ist der bereits übersetzte quest/*.yml-Bestand.
    private void appendQuestTrackerLines(List<Component> lines, PlayerProfile profile) {
        QuestManager questManager = PixelRPGPlugin.getInstance().getQuestManager();
        if (questManager == null) {
            return;
        }

        lines.add(Component.text(" "));
        if (lines.size() >= MAX_LINES) {
            return;
        }
        lines.add(Component.text("-- Quests --", NamedTextColor.YELLOW));

        int shown = 0;
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            if (shown >= MAX_TRACKED_QUESTS || lines.size() >= MAX_LINES) {
                break;
            }
            Quest quest = questManager.getRepository().getQuest(progress.getQuestId());
            if (quest == null) {
                continue;
            }
            lines.add(Component.text(quest.title() + ": ", NamedTextColor.GRAY)
                    .append(Component.text(progress.getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.GREEN)));
            shown++;
        }
    }
}