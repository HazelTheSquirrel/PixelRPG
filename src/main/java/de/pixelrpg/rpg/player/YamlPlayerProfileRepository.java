package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.quest.QuestProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public final class YamlPlayerProfileRepository implements PlayerProfileRepository {
    private final File playersFolder;

    public YamlPlayerProfileRepository(File dataFolder) { this.playersFolder = new File(dataFolder, "players"); }

    @Override
    public void init() { if (!playersFolder.exists()) playersFolder.mkdirs(); }

    @Override
    public Optional<PlayerProfile> load(UUID uuid) {
        File file = new File(playersFolder, uuid + ".yml");
        if (!file.exists()) return Optional.empty();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        PlayerProfile profile = new PlayerProfile(uuid);
        profile.setRegisteredInGuild(yaml.getBoolean("registered", false));
        profile.setExperience(yaml.getLong("experience", 0L));
        profile.setMoney(yaml.getDouble("money", 0.0));

        ConfigurationSection professionSection = yaml.getConfigurationSection("professions");
        if (professionSection != null) loadProfessions(profile, professionSection);
        profile.setUnlockedRecipes(new HashSet<>(yaml.getStringList("unlocked-recipes")));
        profile.setUnlockedWaypoints(new HashSet<>(yaml.getStringList("unlocked-waypoints")));
        profile.setStoryChapterIndex(yaml.getInt("story-chapter-index", -1));
        profile.setCompletedQuests(new HashSet<>(yaml.getStringList("completed-quests")));
        ConfigurationSection activeSection = yaml.getConfigurationSection("active-quests");
        if (activeSection != null) for (String questId : activeSection.getKeys(false)) profile.startQuest(new QuestProgress(questId, activeSection.getInt(questId + ".amount", 0), activeSection.getLong(questId + ".expiry", 0L)));
        ConfigurationSection statsSection = yaml.getConfigurationSection("statistics");
        if (statsSection != null) for (String key : statsSection.getKeys(false)) profile.setStatistic(key, statsSection.getLong(key));
        profile.setScoreboardEnabled(yaml.getBoolean("scoreboard-enabled", false));
        profile.setPartyHudEnabled(yaml.getBoolean("party-hud-enabled", false));
        profile.setQuestTrackerEnabled(yaml.getBoolean("quest-tracker-enabled", false));
        profile.setPlaytimeMillis(yaml.getLong("playtime-millis", 0L));
        profile.markClean();
        return Optional.of(profile);
    }

    private void loadProfessions(PlayerProfile profile, ConfigurationSection section) {
        for (Profession profession : Profession.values()) {
            String key = profession.name().toLowerCase();
            if (section.contains(key + ".level")) {
                profile.setProfessionLevel(profession, section.getInt(key + ".level", Profession.MIN_LEVEL));
                profile.setProfessionExperience(profession, section.getLong(key + ".experience", 0L));
                if (section.getBoolean(key + ".learned", false)) profile.learnProfession(profession);
                continue;
            }
            String[] legacyKeys = switch (profession) {
                case BLACKSMITH -> new String[]{"blacksmithing"};
                case PROVISIONER -> new String[]{"cooking", "fishing", "skinning", "herbalism"};
                case ALCHEMIST -> new String[]{"alchemy", "herbalism"};
                case SCHOLAR -> new String[0];
            };
            int level = maxLegacyLevel(section, legacyKeys);
            long experience = maxLegacyExperience(section, legacyKeys);
            profile.setProfessionLevel(profession, level);
            profile.setProfessionExperience(profession, experience);
            if (level > Profession.MIN_LEVEL) profile.learnProfession(profession);
        }
    }

    private int maxLegacyLevel(ConfigurationSection section, String[] keys) {
        int max = Profession.MIN_LEVEL;
        for (String key : keys) max = Math.max(max, section.getInt(key + ".level", section.getInt(key, Profession.MIN_LEVEL)));
        return max;
    }

    private long maxLegacyExperience(ConfigurationSection section, String[] keys) {
        long max = 0L;
        for (String key : keys) max = Math.max(max, section.getLong(key + ".experience", 0L));
        return max;
    }

    @Override
    public void save(PlayerProfile profile) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("registered", profile.isRegisteredInGuild());
        yaml.set("experience", profile.getExperience());
        yaml.set("money", profile.getMoney());
        for (Profession profession : Profession.values()) {
            String key = profession.name().toLowerCase();
            yaml.set("professions." + key + ".level", profile.getProfessionLevel(profession));
            yaml.set("professions." + key + ".experience", profile.getProfessionExperience(profession));
            yaml.set("professions." + key + ".learned", profile.hasLearnedProfession(profession));
        }
        yaml.set("unlocked-recipes", new ArrayList<>(profile.getUnlockedRecipes()));
        yaml.set("unlocked-waypoints", new ArrayList<>(profile.getUnlockedWaypoints()));
        yaml.set("story-chapter-index", profile.getStoryChapterIndex());
        yaml.set("completed-quests", new ArrayList<>(profile.getCompletedQuests()));
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            String path = "active-quests." + progress.getQuestId();
            yaml.set(path + ".amount", progress.getCurrentAmount());
            yaml.set(path + ".expiry", progress.getExpiryTimestampMillis());
        }
        for (var entry : profile.getAllStatistics().entrySet()) yaml.set("statistics." + entry.getKey(), entry.getValue());
        yaml.set("scoreboard-enabled", profile.isScoreboardEnabled());
        yaml.set("party-hud-enabled", profile.isPartyHudEnabled());
        yaml.set("quest-tracker-enabled", profile.isQuestTrackerEnabled());
        yaml.set("playtime-millis", profile.getPlaytimeMillis());
        if (!playersFolder.exists()) playersFolder.mkdirs();
        File target = new File(playersFolder, profile.getUuid() + ".yml");
        File tempFile = new File(playersFolder, profile.getUuid() + ".yml.tmp");
        yaml.save(tempFile);
        try { Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING); }
    }

    @Override public void shutdown() { }
}
