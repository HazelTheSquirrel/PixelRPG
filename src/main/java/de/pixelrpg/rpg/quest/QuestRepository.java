package de.pixelrpg.rpg.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestRepository {
    private final Plugin plugin;
    private final File questFolder;
    private final Map<String, Quest> questsById = new ConcurrentHashMap<>();
    private final Map<String, List<String>> prerequisitesByQuest = new ConcurrentHashMap<>();
    private final Map<String, List<String>> followUpsByQuest = new ConcurrentHashMap<>();
    private final Map<String, String> companionIdsByQuest = new ConcurrentHashMap<>();

    private int maxActiveQuests = 5;
    private int unlockEarlyLevels = 0;

    public QuestRepository(Plugin plugin) {
        this.plugin = plugin;
        this.questFolder = new File(plugin.getDataFolder(), "quest");
    }

    public void load() {
        questsById.clear();
        prerequisitesByQuest.clear();
        followUpsByQuest.clear();
        companionIdsByQuest.clear();
        maxActiveQuests = 5;
        unlockEarlyLevels = 0;
        loadJsonDefinitions("quests_v2.json");
        loadLegacyYaml();
        validateReferences();
    }

    /** Loads the active user-editable quest catalogue from a JSON definition file. */
    private void loadJsonDefinitions(String fileName) {
        JsonObject root = new JsonDataManager(plugin).load(fileName);
        JsonObject rules = object(root, "rules");
        if (rules != null) {
            maxActiveQuests = Math.max(1, number(rules, "maxActive", maxActiveQuests));
            unlockEarlyLevels = Math.max(0, number(rules, "unlockEarlyLevels", unlockEarlyLevels));
        }
        JsonArray definitions = root.getAsJsonArray("definitions");
        if (definitions == null) return;
        for (var element : definitions) {
            if (!element.isJsonObject()) continue;
            JsonObject json = element.getAsJsonObject();
            if (!validateDefinition(root, json)) continue;
            String id = string(json, "id", "").strip();
            QuestType type = parseType(string(json, "type", "HUNT"));
            int recommendedLevel = number(json, "recommendedLevel", number(json, "requiredLevel", 1));
            int categoryLevel = number(json, "categoryLevel", recommendedLevel);
            int requiredAmount = number(json, "requiredAmount", 1);
            JsonObject reward = object(json, "reward");
            Quest quest = new Quest(id, string(json, "title", id), string(json, "description", ""), type,
                    string(json, "targetKey", ""), requiredAmount, recommendedLevel, categoryLevel,
                    numberDouble(reward, "money", 0.0), numberLong(reward, "experience", 0L),
                    number(reward, "durationMinutes", 0), stringList(reward, "items"),
                    string(reward, "companionId", ""), null, null, 0.0);
            questsById.put(id, quest);
            List<String> prerequisites = stringList(json, "prerequisites");
            JsonObject requirements = object(json, "requirements");
            if (requirements != null) {
                String previousQuest = string(requirements, "previousQuest", "").strip();
                if (!previousQuest.isBlank() && !prerequisites.contains(previousQuest)) {
                    List<String> merged = new ArrayList<>(prerequisites);
                    merged.add(previousQuest);
                    prerequisites = List.copyOf(merged);
                }
            }
            prerequisitesByQuest.put(id, prerequisites);
            followUpsByQuest.put(id, stringList(json, "followUpQuestIds"));
            companionIdsByQuest.put(id, quest.rewardCompanionId());
        }
    }

    private boolean validateDefinition(JsonObject root, JsonObject json) {
        String id = string(json, "id", "").strip();
        if (id.isBlank() || questsById.containsKey(id)) return false;
        QuestType type;
        try {
            type = QuestType.valueOf(string(json, "type", "HUNT").trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Ignoring quest '" + id + "': unknown quest type.");
            return false;
        }
        int level = number(json, "recommendedLevel", number(json, "requiredLevel", 1));
        int category = number(json, "categoryLevel", level);
        int amount = number(json, "requiredAmount", 1);
        if (level < Level.MIN_LEVEL || level > Level.MAX_NORMAL_LEVEL || category < Level.MIN_LEVEL || category > Level.MAX_NORMAL_LEVEL || amount <= 0) return false;
        if ((type == QuestType.HUNT || type == QuestType.COLLECT) && string(json, "targetKey", "").isBlank()) return false;
        String companionId = string(object(json, "reward"), "companionId", "").strip();
        if (!companionId.isBlank()) {
            JsonObject companionRoot = new JsonDataManager(plugin).load("companions.json");
            JsonObject companion = findCompanionDefinition(companionRoot, companionId);
            if (companion == null || bool(companion, "adminOnly", false) || "UNIQUE".equalsIgnoreCase(string(companion, "rarity", ""))) {
                plugin.getLogger().warning("Ignoring quest '" + id + "': invalid normal quest companion reward '" + companionId + "'.");
                return false;
            }
        }
        return true;
    }

    private JsonObject findCompanionDefinition(JsonObject root, String id) {
        if (root == null || !root.has("definitions") || !root.get("definitions").isJsonArray()) return null;
        for (var element : root.getAsJsonArray("definitions")) {
            if (!element.isJsonObject()) continue;
            JsonObject definition = element.getAsJsonObject();
            if (id.equals(string(definition, "id", ""))) return definition;
        }
        return null;
    }

    private void validateReferences() {
        for (Map.Entry<String, List<String>> entry : prerequisitesByQuest.entrySet()) {
            entry.getValue().stream().filter(id -> !questsById.containsKey(id))
                    .forEach(id -> plugin.getLogger().warning("Quest '" + entry.getKey() + "' references unknown prerequisite '" + id + "'."));
        }
        for (Map.Entry<String, List<String>> entry : followUpsByQuest.entrySet()) {
            entry.getValue().stream().filter(id -> !questsById.containsKey(id))
                    .forEach(id -> plugin.getLogger().warning("Quest '" + entry.getKey() + "' references unknown follow-up quest '" + id + "'."));
        }
    }

    private void loadLegacyYaml() {
        if (!questFolder.exists()) questFolder.mkdirs();
        File[] files = questFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;
        for (File file : files) loadFile(file, parseCategoryLevel(file.getName()));
    }

    private void loadFile(File file, int fileCategoryLevel) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("quests");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || questsById.containsKey(id)) continue;
            QuestType type = parseType(section.getString("type", "HUNT"));
            int requiredLevel = readLevelRequirement(section, fileCategoryLevel);
            int categoryLevel = clampLevel(section.getInt("category-level", fileCategoryLevel));
            Quest quest = new Quest(id, section.getString("title", id), section.getString("description", ""), type,
                    section.getString("target-key", ""), section.getInt("required-amount", 1), requiredLevel, categoryLevel,
                    section.getDouble("reward-money", 0.0), section.getLong("reward-exp", 0L), section.getInt("duration-minutes", 0),
                    section.getStringList("reward-items"), null, readLocation(section, "escort-destination"), readLocation(section, "reach-location"), section.getDouble("reach-radius", 5.0));
            questsById.put(id, quest);
            prerequisitesByQuest.put(id, section.getStringList("prerequisites"));
            followUpsByQuest.put(id, section.getStringList("follow-up-quests"));
            companionIdsByQuest.put(id, "");
        }
    }

    private int readLevelRequirement(ConfigurationSection section, int fallback) {
        if (section.contains("required-level")) return clampLevel(section.getInt("required-level", fallback));
        String oldRank = section.getString("required-rank");
        return oldRank == null ? clampLevel(fallback) : oldRankToLevel(oldRank);
    }

    private Location readLocation(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) return null;
        World world = Bukkit.getWorld(section.getString("world", ""));
        if (world == null) return null;
        return new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
    }

    private static JsonObject object(JsonObject parent, String key) { return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null; }
    private static String string(JsonObject object, String key, String fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback; }
    private static int number(JsonObject object, String key, int fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.getAsJsonPrimitive(key).getAsInt() : fallback; }
    private static long numberLong(JsonObject object, String key, long fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.getAsJsonPrimitive(key).getAsLong() : fallback; }
    private static double numberDouble(JsonObject object, String key, double fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.getAsJsonPrimitive(key).getAsDouble() : fallback; }
    private static boolean bool(JsonObject object, String key, boolean fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isBoolean() ? object.getAsJsonPrimitive(key).getAsBoolean() : fallback; }
    private static List<String> stringList(JsonObject object, String key) { if (object == null || !object.has(key) || !object.get(key).isJsonArray()) return List.of(); List<String> result = new ArrayList<>(); for (var element : object.getAsJsonArray(key)) if (element.isJsonPrimitive()) result.add(element.getAsString()); return List.copyOf(result); }

    public Quest getQuest(String id) { return questsById.get(id); }
    public List<Quest> getAllQuests() { return List.copyOf(questsById.values()); }
    public List<Quest> getQuestsByType(QuestType type) { return questsById.values().stream().filter(quest -> quest.type() == type).toList(); }
    public int maxActiveQuests() { return maxActiveQuests; }
    public int unlockEarlyLevels() { return unlockEarlyLevels; }
    public boolean prerequisitesMet(de.pixelrpg.rpg.player.PlayerProfile profile, String questId) { return prerequisitesByQuest.getOrDefault(questId, List.of()).stream().allMatch(profile::hasCompletedQuest); }
    private int parseCategoryLevel(String name) { try { String digits = name.replaceAll("[^0-9]", ""); return digits.isBlank() ? 1 : clampLevel(Integer.parseInt(digits)); } catch (NumberFormatException exception) { return 1; } }
    private int clampLevel(int value) { return Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, value)); }
    private int oldRankToLevel(String rank) { return switch (rank.toUpperCase()) { case "F" -> 1; case "E" -> 10; case "D" -> 20; case "C" -> 35; case "B" -> 50; case "A" -> 70; case "S" -> 90; default -> 1; }; }
    private QuestType parseType(String value) { try { return QuestType.valueOf(value.trim().toUpperCase()); } catch (IllegalArgumentException exception) { return QuestType.HUNT; } }
}
