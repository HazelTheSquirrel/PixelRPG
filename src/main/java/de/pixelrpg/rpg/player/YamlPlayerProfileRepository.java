package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.equipment.EquipmentSlot;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.quest.QuestProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class YamlPlayerProfileRepository implements PlayerProfileRepository {
    private final File playersFolder;
    public YamlPlayerProfileRepository(File dataFolder) { this.playersFolder = new File(dataFolder, "players"); }
    @Override public void init() { if (!playersFolder.exists()) playersFolder.mkdirs(); }

    @Override
    public Optional<PlayerProfile> load(UUID uuid) {
        File file = new File(playersFolder, uuid + ".yml");
        if (!file.exists()) return Optional.empty();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        PlayerProfile profile = new PlayerProfile(uuid);
        profile.setRegistered(yaml.getBoolean("registered", false));
        profile.setExperience(yaml.getLong("experience", 0L));
        if (yaml.contains("money-minor-units")) profile.setMoneyMinorUnits(yaml.getLong("money-minor-units", 0L));
        else profile.setMoney(yaml.getDouble("money", 0.0D));
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
        Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        ConfigurationSection equipmentSection = yaml.getConfigurationSection("equipment");
        if (equipmentSection != null) for (EquipmentSlot slot : EquipmentSlot.values()) { ItemStack item = equipmentSection.getItemStack(slot.name().toLowerCase()); if (item != null && !item.isEmpty()) equipment.put(slot, item); }
        profile.setEquipment(equipment);
        ConfigurationSection companionsSection = yaml.getConfigurationSection("companions");
        if (companionsSection != null) {
            Map<String, CompanionState> companions = new LinkedHashMap<>();
            for (String id : companionsSection.getKeys(false)) {
                ConfigurationSection section = companionsSection.getConfigurationSection(id);
                if (section == null) continue;
                Map<String, ItemStack> items = new LinkedHashMap<>();
                ConfigurationSection equipmentSection2 = section.getConfigurationSection("equipment");
                if (equipmentSection2 != null) for (String slot : equipmentSection2.getKeys(false)) {
                    ItemStack item = equipmentSection2.getItemStack(slot);
                    if (item != null && !item.isEmpty()) items.put(slot, item);
                }
                companions.put(id, new CompanionState(id, section.getString("name", id),
                        Math.max(1, section.getInt("level", 1)), Math.max(0L, section.getLong("experience", 0L)),
                        section.getBoolean("unlocked", true), section.getBoolean("active", false), items,
                        section.getString("skin.value", ""), section.getString("skin.signature", "")));
            }
            profile.setCompanions(companions);
        }
        profile.setScoreboardEnabled(yaml.getBoolean("scoreboard-enabled", true));
        profile.setPartyHudEnabled(yaml.getBoolean("party-hud-enabled", false));
        profile.setQuestTrackerEnabled(yaml.getBoolean("quest-tracker-enabled", false));
        profile.setPlaytimeMillis(yaml.getLong("playtime-millis", 0L));
        profile.setPersistenceRevision(yaml.getLong("persistence-revision", 0L));
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
            String legacyKey = legacyKey(profession);
            if (legacyKey == null) continue;
            int level = section.getInt(legacyKey + ".level", section.getInt(legacyKey, Profession.MIN_LEVEL));
            long experience = section.getLong(legacyKey + ".experience", 0L);
            profile.setProfessionLevel(profession, level);
            profile.setProfessionExperience(profession, experience);
            if (level > Profession.MIN_LEVEL) profile.learnProfession(profession);
        }
    }

    private String legacyKey(Profession profession) {
        return switch (profession) {
            case BLACKSMITH -> "blacksmithing";
            case COOK -> "cooking";
            case FISHERMAN -> "fishing";
            case TAILOR -> "skinning";
            case ALCHEMIST -> "alchemy";
            case SCHOLAR, FARMER, MASON, WOODCUTTER -> null;
        };
    }

    @Override
    public long save(PlayerProfile profile) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("registered", profile.isRegistered()); yaml.set("experience", profile.getExperience()); yaml.set("money-minor-units", profile.getMoneyMinorUnits());
        for (Profession profession : Profession.values()) { String key = profession.name().toLowerCase(); yaml.set("professions." + key + ".level", profile.getProfessionLevel(profession)); yaml.set("professions." + key + ".experience", profile.getProfessionExperience(profession)); yaml.set("professions." + key + ".learned", profile.hasLearnedProfession(profession)); }
        yaml.set("unlocked-recipes", new ArrayList<>(profile.getUnlockedRecipes())); yaml.set("unlocked-waypoints", new ArrayList<>(profile.getUnlockedWaypoints())); yaml.set("story-chapter-index", profile.getStoryChapterIndex()); yaml.set("completed-quests", new ArrayList<>(profile.getCompletedQuests()));
        for (QuestProgress progress : profile.getActiveQuests().values()) { String path = "active-quests." + progress.getQuestId(); yaml.set(path + ".amount", progress.getCurrentAmount()); yaml.set(path + ".expiry", progress.getExpiryTimestampMillis()); }
        for (var entry : profile.getAllStatistics().entrySet()) yaml.set("statistics." + entry.getKey(), entry.getValue());
        for (var entry : profile.getEquipment().entrySet()) yaml.set("equipment." + entry.getKey().name().toLowerCase(), entry.getValue());
        for (CompanionState companion : profile.getCompanions().values()) {
            String path = "companions." + companion.id();
            yaml.set(path + ".name", companion.name());
            yaml.set(path + ".level", companion.level());
            yaml.set(path + ".experience", companion.experience());
            yaml.set(path + ".unlocked", companion.unlocked());
            yaml.set(path + ".active", companion.active());
            yaml.set(path + ".skin.value", companion.skinValue());
            yaml.set(path + ".skin.signature", companion.skinSignature());
            for (var item : companion.equipment().entrySet()) yaml.set(path + ".equipment." + item.getKey(), item.getValue());
        }
        yaml.set("scoreboard-enabled", profile.isScoreboardEnabled()); yaml.set("party-hud-enabled", profile.isPartyHudEnabled()); yaml.set("quest-tracker-enabled", profile.isQuestTrackerEnabled()); yaml.set("playtime-millis", profile.getPlaytimeMillis());
        long nextRevision = profile.getPersistenceRevision() + 1L; yaml.set("persistence-revision", nextRevision);
        if (!playersFolder.exists()) playersFolder.mkdirs();
        File target = new File(playersFolder, profile.getUuid() + ".yml"); File tempFile = new File(playersFolder, profile.getUuid() + ".yml.tmp"); yaml.save(tempFile);
        try { Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); } catch (AtomicMoveNotSupportedException e) { Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        return nextRevision;
    }
    @Override public void shutdown() { }
}
