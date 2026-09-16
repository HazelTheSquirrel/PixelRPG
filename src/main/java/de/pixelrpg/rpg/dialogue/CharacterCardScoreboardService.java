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
import org.bukkit.scoreboard.Scoreboard;

import java.util.LinkedHashMap;
import java.util.Map;

/** Keeps player-specific RPG values available to the native character-card presentation. */
public final class CharacterCardScoreboardService {
    private static final Map<String, String> OBJECTIVES = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("level", "px_cc_lvl"),
            Map.entry("health", "px_cc_hp"),
            Map.entry("maxHealth", "px_cc_maxhp"),
            Map.entry("armor", "px_cc_armor"),
            Map.entry("movementSpeed", "px_cc_speed"),
            Map.entry("reach", "px_cc_reach"),
            Map.entry("critChance", "px_cc_crit"),
            Map.entry("critDamage", "px_cc_critdmg"),
            Map.entry("lifesteal", "px_cc_lifesteal"),
            Map.entry("attackPower", "px_cc_atk")
    ));

    private final JavaPlugin plugin;
    private final PlayerProfileManager profiles;
    private final StatEngine statEngine;
    private final Map<String, Objective> objectives = new LinkedHashMap<>();
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
                objective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text("PixelRPG " + key), RenderType.INTEGER);
            }
            objective.numberFormat(NumberFormat.noStyle());
            objectives.put(key, objective);
        });
    }

    private void syncOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) sync(player);
    }

    private void sync(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;

        StatEngine.CachedStats stats = statEngine.getCachedStats(player.getUniqueId());
        Attribute maxHealthAttribute = Attribute.MAX_HEALTH;
        Attribute armorAttribute = Attribute.ARMOR;
        var maxHealthInstance = player.getAttribute(maxHealthAttribute);
        var armorInstance = player.getAttribute(armorAttribute);

        double maxHealth = maxHealthInstance != null ? maxHealthInstance.getValue() : stats.maxHealth();
        double armor = armorInstance != null ? armorInstance.getValue() : stats.armor();

        set(player, "level", profile.getLevel());
        set(player, "health", round(player.getHealth()));
        set(player, "maxHealth", round(maxHealth));
        set(player, "armor", round(armor));
        set(player, "movementSpeed", round(stats.movementSpeedBonus()));
        set(player, "reach", round(stats.entityReach()));
        set(player, "critChance", round(stats.critChance()));
        set(player, "critDamage", round(stats.critDamageMultiplier()));
        set(player, "lifesteal", round(stats.lifestealBonus()));
        set(player, "attackPower", round(stats.attackPower()));
    }

    private void set(Player player, String key, int value) {
        Objective objective = objectives.get(key);
        if (objective != null) objective.getScore(player.getName()).setScore(Math.max(0, value));
    }

    private int round(double value) {
        return (int) Math.round(value);
    }
}
