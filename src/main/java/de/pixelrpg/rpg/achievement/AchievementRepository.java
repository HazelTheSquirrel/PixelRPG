// src/main/java/de/pixelrpg/rpg/achievement/AchievementRepository.java
package de.pixelrpg.rpg.achievement;

import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.core.StatisticType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class AchievementRepository {

    private final Plugin plugin;
    private final File file;
    private final Map<String, AchievementDefinition> definitionsById = new ConcurrentHashMap<>();

    public AchievementRepository(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "achievements.yml");
    }

    public void load() {
        definitionsById.clear();
        if (!file.exists()) {
            createDefaultAchievements();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("achievements");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            AchievementTriggerType triggerType = parseTriggerType(section.getString("trigger", "STATISTIC_THRESHOLD"));
            StatisticType statisticType = section.contains("statistic") ? parseStatistic(section.getString("statistic")) : null;
            Rank requiredRank = section.contains("required-rank") ? parseRank(section.getString("required-rank")) : null;
            String rewardTitle = section.getString("reward-title", null);

            AchievementDefinition definition = new AchievementDefinition(
                    id,
                    section.getString("name", id),
                    section.getString("description", ""),
                    triggerType,
                    statisticType,
                    section.getLong("threshold", 1L),
                    requiredRank,
                    section.getDouble("reward-money", 0.0),
                    section.getLong("reward-exp", 0L),
                    rewardTitle
            );

            definitionsById.put(id, definition);
        }
    }

    private void createDefaultAchievements() {
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("achievements.first_blood.name", "First Blood");
        yaml.set("achievements.first_blood.description", "Defeat your first monster.");
        yaml.set("achievements.first_blood.trigger", "STATISTIC_THRESHOLD");
        yaml.set("achievements.first_blood.statistic", "MOBS_KILLED");
        yaml.set("achievements.first_blood.threshold", 1);
        yaml.set("achievements.first_blood.reward-exp", 50);

        yaml.set("achievements.veteran_hunter.name", "Veteran Hunter");
        yaml.set("achievements.veteran_hunter.description", "Defeat 100 monsters.");
        yaml.set("achievements.veteran_hunter.trigger", "STATISTIC_THRESHOLD");
        yaml.set("achievements.veteran_hunter.statistic", "MOBS_KILLED");
        yaml.set("achievements.veteran_hunter.threshold", 100);
        yaml.set("achievements.veteran_hunter.reward-money", 100.0);
        yaml.set("achievements.veteran_hunter.reward-title", "Hunter");

        yaml.set("achievements.rank_a_reached.name", "Seasoned Adventurer");
        yaml.set("achievements.rank_a_reached.description", "Reach Guild Rank A.");
        yaml.set("achievements.rank_a_reached.trigger", "RANK_REACHED");
        yaml.set("achievements.rank_a_reached.required-rank", "A");
        yaml.set("achievements.rank_a_reached.reward-money", 200.0);
        yaml.set("achievements.rank_a_reached.reward-title", "Veteran");

        yaml.set("achievements.choose_your_path.name", "Choose Your Path");
        yaml.set("achievements.choose_your_path.description", "Select your specialization class.");
        yaml.set("achievements.choose_your_path.trigger", "CLASS_CHOSEN");
        yaml.set("achievements.choose_your_path.reward-exp", 100);

        yaml.set("achievements.boss_slayer.name", "Boss Slayer");
        yaml.set("achievements.boss_slayer.description", "Defeat your first boss.");
        yaml.set("achievements.boss_slayer.trigger", "STATISTIC_THRESHOLD");
        yaml.set("achievements.boss_slayer.statistic", "BOSSES_DEFEATED");
        yaml.set("achievements.boss_slayer.threshold", 1);
        yaml.set("achievements.boss_slayer.reward-title", "Boss Slayer");

        yaml.set("achievements.dungeon_delver.name", "Dungeon Delver");
        yaml.set("achievements.dungeon_delver.description", "Clear 5 dungeons.");
        yaml.set("achievements.dungeon_delver.trigger", "STATISTIC_THRESHOLD");
        yaml.set("achievements.dungeon_delver.statistic", "DUNGEONS_CLEARED");
        yaml.set("achievements.dungeon_delver.threshold", 5);
        yaml.set("achievements.dungeon_delver.reward-title", "Dungeon Delver");

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create default achievements.yml", e);
        }
    }

    public void registerRuntime(AchievementDefinition definition) {
        definitionsById.put(definition.id(), definition);
    }

    public AchievementDefinition get(String id) {
        return definitionsById.get(id);
    }

    public List<AchievementDefinition> getAll() {
        return new ArrayList<>(definitionsById.values());
    }

    private AchievementTriggerType parseTriggerType(String raw) {
        try {
            return AchievementTriggerType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return AchievementTriggerType.STATISTIC_THRESHOLD;
        }
    }

    private StatisticType parseStatistic(String raw) {
        try {
            return StatisticType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private Rank parseRank(String raw) {
        try {
            return Rank.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Rank.F;
        }
    }
}