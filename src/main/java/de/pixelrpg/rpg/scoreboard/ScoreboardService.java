// src/main/java/de/pixelrpg/rpg/scoreboard/ScoreboardService.java (VOLLSTÄNDIG, ersetzt alte Datei — Party-Anzeige inkl. sich selbst)
package de.pixelrpg.rpg.scoreboard;

import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ScoreboardService implements Listener {

    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final int updateIntervalTicks;

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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profileManager.getProfile(event.getPlayer().getUniqueId()).ifPresent(profile -> {
            if (profile.isRegisteredInGuild() && profile.isScoreboardEnabled()) {
                apply(event.getPlayer(), profile);
            }
        });
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
    }

    private void apply(Player player, PlayerProfile profile) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("pixelrpg", Criteria.DUMMY,
                Component.text("PixelRPG", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<Component> lines = buildLines(player, profile);
        int score = lines.size();

        for (Component line : lines) {
            String entry = "\u00A7" + Integer.toHexString(score) + "\u00A7r";
            Team team = board.registerNewTeam("line" + score);
            team.addEntry(entry);
            team.prefix(line);
            objective.getScore(entry).setScore(score);
            score--;
        }

        player.setScoreboard(board);
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

        if (profile.isPartyHudEnabled()) {
            PartyAPI partyAPI = Bukkit.getServicesManager().load(PartyAPI.class);
            if (partyAPI != null && partyAPI.isInParty(player.getUniqueId())) {
                lines.add(Component.text(" "));
                lines.add(Component.text("-- Party --", NamedTextColor.LIGHT_PURPLE));

                for (UUID memberUuid : partyAPI.getPartyMembers(player.getUniqueId())) {
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
}