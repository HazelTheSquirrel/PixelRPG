// src/main/java/de/pixelrpg/rpg/player/YamlPlayerProfileRepository.java (VOLLSTÄNDIG, ersetzt alte Datei — atomarer Schreibvorgang über Temp-Datei + Rename gegen korrupte Dateien bei Absturz mitten im Speichern)
package de.pixelrpg.rpg.player;

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

    public YamlPlayerProfileRepository(File dataFolder) {
        this.playersFolder = new File(dataFolder, "players");
    }

    @Override
    public void init() {
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
    }

    @Override
    public Optional<PlayerProfile> load(UUID uuid) {
        File file = new File(playersFolder, uuid + ".yml");
        if (!file.exists()) {
            return Optional.empty();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        PlayerProfile profile = new PlayerProfile(uuid);
        profile.setRegisteredInGuild(yaml.getBoolean("registered", false));
        profile.setExperience(yaml.getLong("experience", 0L));
        profile.setPlayerClass(parseClass(yaml.getString("player-class", "NONE")));
        profile.setMoney(yaml.getDouble("money", 0.0));
        profile.setReceivedStartBonus(yaml.getBoolean("start-bonus", false));

        for (PlayerAttribute attribute : PlayerAttribute.values()) {
            int value = yaml.getInt("attributes." + attribute.name().toLowerCase(), 0);
            profile.setAttributePoints(attribute, value);
        }

        profile.setUnlockedWaypoints(new HashSet<>(yaml.getStringList("unlocked-waypoints")));
        profile.setStoryChapterIndex(yaml.getInt("story-chapter-index", -1));
        profile.setCompletedQuests(new HashSet<>(yaml.getStringList("completed-quests")));

        ConfigurationSection activeSection = yaml.getConfigurationSection("active-quests");
        if (activeSection != null) {
            for (String questId : activeSection.getKeys(false)) {
                int amount = activeSection.getInt(questId + ".amount", 0);
                long expiry = activeSection.getLong(questId + ".expiry", 0L);
                profile.startQuest(new QuestProgress(questId, amount, expiry));
            }
        }

        ConfigurationSection statsSection = yaml.getConfigurationSection("statistics");
        if (statsSection != null) {
            for (String key : statsSection.getKeys(false)) {
                profile.setStatistic(key, statsSection.getLong(key));
            }
        }

        profile.setScoreboardEnabled(yaml.getBoolean("scoreboard-enabled", true));
        profile.setPartyHudEnabled(yaml.getBoolean("party-hud-enabled", true));
        profile.setQuestTrackerEnabled(yaml.getBoolean("quest-tracker-enabled", true));
        profile.setPlaytimeMillis(yaml.getLong("playtime-millis", 0L));

        profile.markClean();
        return Optional.of(profile);
    }

    @Override
    public void save(PlayerProfile profile) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("registered", profile.isRegisteredInGuild());
        yaml.set("experience", profile.getExperience());
        yaml.set("player-class", profile.getPlayerClass().name());
        yaml.set("money", profile.getMoney());
        yaml.set("start-bonus", profile.hasReceivedStartBonus());

        for (PlayerAttribute attribute : PlayerAttribute.values()) {
            yaml.set("attributes." + attribute.name().toLowerCase(), profile.getAttributePoints(attribute));
        }

        yaml.set("unlocked-waypoints", new ArrayList<>(profile.getUnlockedWaypoints()));
        yaml.set("story-chapter-index", profile.getStoryChapterIndex());
        yaml.set("completed-quests", new ArrayList<>(profile.getCompletedQuests()));

        for (QuestProgress progress : profile.getActiveQuests().values()) {
            String path = "active-quests." + progress.getQuestId();
            yaml.set(path + ".amount", progress.getCurrentAmount());
            yaml.set(path + ".expiry", progress.getExpiryTimestampMillis());
        }

        for (var entry : profile.getAllStatistics().entrySet()) {
            yaml.set("statistics." + entry.getKey(), entry.getValue());
        }

        yaml.set("scoreboard-enabled", profile.isScoreboardEnabled());
        yaml.set("party-hud-enabled", profile.isPartyHudEnabled());
        yaml.set("quest-tracker-enabled", profile.isQuestTrackerEnabled());
        yaml.set("playtime-millis", profile.getPlaytimeMillis());

        // Atomarer Schreibvorgang: erst in eine Temp-Datei speichern, dann per
        // Rename ersetzen. Verhindert eine halb geschriebene/korrupte Profildatei,
        // falls der Prozess genau während des Schreibens beendet wird.
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
        File target = new File(playersFolder, profile.getUuid() + ".yml");
        File tempFile = new File(playersFolder, profile.getUuid() + ".yml.tmp");
        yaml.save(tempFile);
        try {
            Files.move(tempFile.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void shutdown() {
    }

    private PlayerClass parseClass(String raw) {
        try {
            return PlayerClass.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return PlayerClass.NONE;
        }
    }
}