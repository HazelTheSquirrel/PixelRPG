package de.pixelrpg.rpg.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.player.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestRepository {
    private final Plugin plugin;
    private final Map<String, Quest> questsById = new ConcurrentHashMap<>();
    private final Map<String, List<String>> prerequisitesByQuest = new ConcurrentHashMap<>();
    private final Map<String, List<String>> followUpsByQuest = new ConcurrentHashMap<>();

    private static final int MAX_ACTIVE_QUESTS = 5;

    public QuestRepository(Plugin plugin) { this.plugin = plugin; }

    public void load() {
        questsById.clear();
        prerequisitesByQuest.clear();
        followUpsByQuest.clear();
        loadJsonDefinitions();
        validateReferences();
    }

    private void loadJsonDefinitions() {
        JsonObject root = new JsonDataManager(plugin).load("quests_v2.json");
        JsonArray definitions = root.getAsJsonArray("definitions");
        if (definitions == null) return;
        for (var element : definitions) {
            if (!element.isJsonObject()) continue;
            JsonObject json = element.getAsJsonObject();
            if (!validateDefinition(json)) continue;
            String id = string(json, "id", "").strip();
            QuestType type = parseType(string(json, "type", "HUNT"));
            int recommendedLevel = number(json, "recommendedLevel", 1);
            int categoryLevel = number(json, "categoryLevel", recommendedLevel);
            int requiredAmount = number(json, "requiredAmount", 1);
            JsonObject reward = object(json, "reward");
            JsonObject navigation = object(json, "navigation");
            Quest quest = new Quest(id, string(json, "title", id), string(json, "description", ""), type,
                    string(json, "targetKey", ""), requiredAmount, recommendedLevel, categoryLevel,
                    numberDouble(reward, "money", 0.0), numberLong(reward, "experience", 0L),
                    number(reward, "durationMinutes", 0), stringList(reward, "items"),
                    string(reward, "companionId", ""), string(json, "questGiverNpcId", ""),
                    string(navigation, "structure", ""), stringList(navigation, "biomes"),
                    Math.max(1, number(navigation, "radius", 1024)), bool(navigation, "findUnexplored", false), null);
            questsById.put(id, quest);
            prerequisitesByQuest.put(id, mergePrerequisites(json));
            followUpsByQuest.put(id, stringList(json, "followUpQuestIds"));
        }
    }

    private boolean validateDefinition(JsonObject json) {
        String id = string(json, "id", "").strip();
        if (id.isBlank() || questsById.containsKey(id)) return false;
        QuestType type;
        try { type = QuestType.valueOf(string(json, "type", "HUNT").trim().toUpperCase()); }
        catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Ignoring quest '" + id + "': unknown quest type.");
            return false;
        }
        int level = number(json, "recommendedLevel", 1);
        int category = number(json, "categoryLevel", level);
        int amount = number(json, "requiredAmount", 1);
        if (level < Level.MIN_LEVEL || level > Level.MAX_NORMAL_LEVEL || category < Level.MIN_LEVEL || category > Level.MAX_NORMAL_LEVEL || amount <= 0) return false;

        if (string(json, "questGiverNpcId", "").isBlank()) {
            plugin.getLogger().warning("Ignoring quest '" + id + "': questGiverNpcId is required for every quest.");
            return false;
        }

        switch (type) {
            case HUNT -> {
                if (!isVanillaEntityType(string(json, "targetKey", "")) || !hasWorldNavigation(object(json, "navigation"))) {
                    plugin.getLogger().warning("Ignoring quest '" + id + "': HUNT needs a vanilla entity and a biome/structure navigation target.");
                    return false;
                }
            }
            case COLLECT -> {
                if (!isVanillaMaterial(string(json, "targetKey", "")) || !hasWorldNavigation(object(json, "navigation"))) {
                    plugin.getLogger().warning("Ignoring quest '" + id + "': COLLECT needs a vanilla item and a biome/structure navigation target.");
                    return false;
                }
            }
            case TALK_TO_NPC -> { if (string(json, "targetKey", "").isBlank()) return false; }
            case REACH_LOCATION -> {
                if (!hasWorldNavigation(object(json, "navigation"))) return false;
            }
            case GLOBAL_EVENT -> { if (string(json, "targetKey", "").isBlank()) return false; }
        }
        return true;
    }

    private boolean hasWorldNavigation(JsonObject navigation) {
        return navigation != null && (!string(navigation, "structure", "").isBlank() || !stringList(navigation, "biomes").isEmpty());
    }

    private boolean isVanillaMaterial(String key) {
        try { return key != null && !key.isBlank() && Material.valueOf(key.trim().toUpperCase()).isItem(); }
        catch (IllegalArgumentException exception) { return false; }
    }

    private boolean isVanillaEntityType(String key) {
        try {
            var type = org.bukkit.entity.EntityType.valueOf(key.trim().toUpperCase());
            return type.isAlive() && type != org.bukkit.entity.EntityType.PLAYER;
        } catch (IllegalArgumentException exception) { return false; }
    }

    private List<String> mergePrerequisites(JsonObject json) {
        List<String> prerequisites = new ArrayList<>(stringList(json, "prerequisites"));
        JsonObject requirements = object(json, "requirements");
        if (requirements != null) {
            String previousQuest = string(requirements, "previousQuest", "").strip();
            if (!previousQuest.isBlank() && !prerequisites.contains(previousQuest)) prerequisites.add(previousQuest);
        }
        return List.copyOf(prerequisites);
    }

    private void validateReferences() {
        for (Map.Entry<String, List<String>> entry : prerequisitesByQuest.entrySet())
            entry.getValue().stream().filter(id -> !questsById.containsKey(id)).forEach(id -> plugin.getLogger().warning("Quest '" + entry.getKey() + "' references unknown prerequisite '" + id + "'."));
        for (Map.Entry<String, List<String>> entry : followUpsByQuest.entrySet())
            entry.getValue().stream().filter(id -> !questsById.containsKey(id)).forEach(id -> plugin.getLogger().warning("Quest '" + entry.getKey() + "' references unknown follow-up quest '" + id + "'."));
    }

    private static JsonObject object(JsonObject parent, String key) { return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null; }
    private static String string(JsonObject object, String key, String fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback; }
    private static int number(JsonObject object, String key, int fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.getAsJsonPrimitive(key).getAsInt() : fallback; }
    private static long numberLong(JsonObject object, String key, long fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.getAsJsonPrimitive(key).getAsLong() : fallback; }
    private static double numberDouble(JsonObject object, String key, double fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.getAsJsonPrimitive(key).getAsDouble() : fallback; }
    private static boolean bool(JsonObject object, String key, boolean fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isBoolean() ? object.getAsJsonPrimitive(key).getAsBoolean() : fallback; }
    private static List<String> stringList(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (var element : object.getAsJsonArray(key)) if (element.isJsonPrimitive()) result.add(element.getAsString());
        return List.copyOf(result);
    }

    public Quest getQuest(String id) { return questsById.get(id); }
    public List<Quest> getAllQuests() { return List.copyOf(questsById.values()); }
    public List<Quest> getQuestsByType(QuestType type) { return questsById.values().stream().filter(quest -> quest.type() == type).toList(); }
    public int maxActiveQuests() { return MAX_ACTIVE_QUESTS; }
    public int unlockEarlyLevels() { return 0; }
    public boolean prerequisitesMet(PlayerProfile profile, String questId) { return prerequisitesByQuest.getOrDefault(questId, List.of()).stream().allMatch(profile::hasCompletedQuest); }
    public List<String> getFollowUpQuestIds(String questId) { return followUpsByQuest.getOrDefault(questId, List.of()); }
    private QuestType parseType(String value) { try { return QuestType.valueOf(value.trim().toUpperCase()); } catch (IllegalArgumentException exception) { return QuestType.HUNT; } }
}
