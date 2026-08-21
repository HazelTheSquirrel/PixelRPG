package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

/** Keeps the player-specific values used by the native G character dialog available through vanilla score components. */
public final class CharacterCardScoreboardService {
    private static final Map<String, String> OBJECTIVES = Map.ofEntries(
            Map.entry("level", "px_cc_lvl"),
            Map.entry("health", "px_cc_hp"),
            Map.entry("maxHealth", "px_cc_maxhp"),
            Map.entry("mana", "px_cc_mana"),
            Map.entry("maxMana", "px_cc_maxmana"),
            Map.entry("armor", "px_cc_armor"),
            Map.entry("strength", "px_cc_str"),
            Map.entry("agility", "px_cc_agi"),
            Map.entry("stamina", "px_cc_sta"),
            Map.entry("intellect", "px_cc_int"),
            Map.entry("attackPower", "px_cc_atk"),
            Map.entry("spellPower", "px_cc_spell"),
            Map.entry("critChance", "px_cc_crit"),
            Map.entry("class", "px_cc_class")
    );

    private final JavaPlugin plugin;
    private final PlayerProfileManager profiles;
    private final StatEngine statEngine;
    private final Map<String, Objective> objectives = new HashMap<>();
    private BukkitTask task;

    public CharacterCardScoreboardService(JavaPlugin plugin, PlayerProfileManager profiles, StatEngine statEngine) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.statEngine = statEngine;
        initialiseObjectives();
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::syncOnlinePlayers, 1L, 5L);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    private void initialiseObjectives() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        OBJECTIVES.forEach((key, objectiveName) -> {
            Objective objective = scoreboard.getObjective(objectiveName);
            if (objective == null) {
                objective = scoreboard.registerNewObjective(
                        objectiveName,
                        Criteria.DUMMY,
                        Component.text("PixelRPG " + key),
                        RenderType.INTEGER
                );
            }
            objective.numberFormat(NumberFormat.noStyle());
            objectives.put(key, objective);
        });
    }

    private void syncOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sync(player);
        }
    }

    private void sync(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) return;

        StatEngine.CachedStats stats = statEngine.getCachedStats(player.getUniqueId());
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
                : stats.maxHealth();
        double armor = player.getAttribute(Attribute.ARMOR) != null
                ? player.getAttribute(Attribute.ARMOR).getValue()
                : stats.armor();

        set(player, "level", profile.getLevel());
        set(player, "health", round(player.getHealth()));
        set(player, "maxHealth", round(maxHealth));
        set(player, "mana", round(profile.getCurrentMana()));
        set(player, "maxMana", round(stats.maxMana()));
        set(player, "armor", round(armor));
        set(player, "strength", round(stats.strength()));
        set(player, "agility", round(stats.agility()));
        set(player, "stamina", round(stats.stamina()));
        set(player, "intellect", round(stats.intellect()));
        set(player, "attackPower", round(stats.attackPower()));
        set(player, "spellPower", round(stats.spellPower()));
        set(player, "critChance", round(stats.critChance()));

        Score classScore = score(player, "class");
        classScore.setScore(1);
        classScore.numberFormat(NumberFormat.fixed(profile.getPlayerClass().displayName()));
    }

    private void set(Player player, String key, int value) {
        score(player, key).setScore(Math.max(0, value));
    }

    private Score score(Player player, String key) {
        return objectives.get(key).getScore(player.getName());
    }

    private int round(double value) {
        return (int) Math.round(value);
    }
}
