package de.pixelrpg.rpg.scoreboard;

import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardService implements Listener, AutoCloseable {
    private final PlayerProfileManager profiles;
    private final GuildManager guilds;
    private final PartyManager parties;
    private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();

    public ScoreboardService(Plugin plugin, PlayerProfileManager profiles, GuildManager guilds, PartyManager parties) {
        this.profiles = profiles;
        this.guilds = guilds;
        this.parties = parties;
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    public void refresh(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered() || !profile.isScoreboardEnabled()) {
            clear(player);
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), ignored -> manager.getNewScoreboard());
        Objective objective = board.getObjective("pixelrpg");
        if (objective == null) {
            objective = board.registerNewObjective("pixelrpg", Criteria.DUMMY, Component.text("PIXELRPG", NamedTextColor.GOLD));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        for (String entry : board.getEntries()) board.resetScores(entry);
        objective.getScore(" ").setScore(6);
        objective.getScore("Level: " + profile.getLevel()).setScore(5);
        objective.getScore("Gold: " + String.format(java.util.Locale.ROOT, "%.2f", profile.getMoney())).setScore(4);
        String guild = guilds.getGuild(player.getUniqueId()).map(value -> value.name()).orElse("Keine");
        objective.getScore("Gilde: " + guild).setScore(3);
        String party = parties.getParty(player.getUniqueId()).map(value -> Integer.toString(value.getMembers().size())).orElse("Keine");
        objective.getScore("Party: " + party).setScore(2);
        objective.getScore("Tode: " + profile.getStatistic("DEATHS")).setScore(1);
        player.setScoreboard(board);
    }

    public void clear(Player player) {
        boards.remove(player.getUniqueId());
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) player.setScoreboard(manager.getMainScoreboard());
    }

    // Aktualisiert das persönliche RPG-Scoreboard beim Betreten des Servers.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    // Entfernt den transienten Scoreboard-Zustand beim Verlassen des Servers.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boards.remove(event.getPlayer().getUniqueId());
    }

    // Aktualisiert kontextabhängige Werte nach einem Weltenwechsel.
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        refresh(event.getPlayer());
    }

    @Override
    public void close() {
        for (Player player : Bukkit.getOnlinePlayers()) clear(player);
        boards.clear();
    }
}
