package de.pixelrpg.rpg.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.item.ItemDefinitionRegistry;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.profession.Profession;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestRepository {
    private final Plugin plugin;
    private final ItemDefinitionRegistry itemDefinitions;
    private final Map<String, Quest> questsById = new ConcurrentHashMap<>();
    private final Map<String, List<String>> prerequisitesByQuest = new ConcurrentHashMap<>();
    private final Map<String, List<String>> followUpsByQuest = new ConcurrentHashMap<>();
    private static final int MAX_ACTIVE_QUESTS = 5;

    public QuestRepository(Plugin plugin) {
        this.plugin = plugin;
        this.itemDefinitions = new ItemDefinitionRegistry(plugin);
    }

    public void load() {
        questsById.clear();
        prerequisitesByQuest.clear();
        followUpsByQuest.clear();
        loadDefinitionsFrom("quests_v2.json");
        loadDefinitionsFrom("quests_additional.json");
        loadDefinitionsFrom("quests_world_expansion.json");
        loadDefinitionsFrom("quests_crafting_orders.json");
        validateReferences();
    }

    private void loadDefinitionsFrom(String resourceName) {
        JsonObject root;
        try {
            root = new JsonDataManager(plugin).load(resourceName);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load quest data '" + resourceName + "': " + exception.getMessage());
            return;
        }
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
            Profession profession = parseProfession(string(json, "profession", ""));
            int requiredProfessionLevel = number(json, "requiredProfessionLevel", profession == null ? 1 : recommendedLevel);
            if (profession != null) {
                recommendedLevel = Math.min(recommendedLevel, Level.MAX_NORMAL_LEVEL);
                categoryLevel = Math.min(categoryLevel, Level.MAX_NORMAL_LEVEL);
            }
            Quest quest = new Quest(id, string(json, "title", "").strip(), string(json, "description", "").strip(), type,
                    canonicalItemId(string(json, "targetKey", "")), requiredAmount, recommendedLevel, categoryLevel,
                    numberDouble(reward, "money", 0.0), numberLong(reward, "experience", 0L),
                    number(reward, "durationMinutes", 0), stringList(reward, "items"),
                    string(reward, "companionId", "").strip(), string(json, "questGiverNpcId", "").strip(),
                    string(navigation, "structure", "").strip(), stringList(navigation, "biomes"),
                    Math.max(1, number(navigation, "radius", 1024)), bool(navigation, "findUnexplored", false), null,
                    profession, requiredProfessionLevel);
            questsById.put(id, quest);
            prerequisitesByQuest.put(id, mergePrerequisites(json));
            followUpsByQuest.put(id, stringList(json, "followUpQuestIds"));
        }
    }

    private boolean validateDefinition(JsonObject json) {
        String id = string(json, "id", "").strip();
        if (id.isBlank() || questsById.containsKey(id)) return false;
        String title = string(json, "title", "").strip();
        String description = string(json, "description", "").strip();
        if (title.isBlank() || description.isBlank()) {
            plugin.getLogger().warning("Ignoring quest '" + id + "': title and description are required.");
            return false;
        }
        QuestType type;
        try {
            type = QuestType.valueOf(string(json, "type", "HUNT").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Ignoring quest '" + id + "': unknown quest type.");
            return false;
        }
        int level = number(json, "recommendedLevel", 1);
        int category = number(json, "categoryLevel", level);
        int amount = number(json, "requiredAmount", 1);
        Profession profession = parseProfession(string(json, "profession", ""));
        int professionLevel = number(json, "requiredProfessionLevel", profession == null ? 1 : level);
        if (level < Level.MIN_LEVEL || level > (profession != null ? Profession.MAX_LEVEL : Level.MAX_NORMAL_LEVEL)
                || category < Level.MIN_LEVEL || category > (profession != null ? Profession.MAX_LEVEL : Level.MAX_NORMAL_LEVEL) || amount <= 0) return false;
        if (profession != null && (professionLevel < Profession.MIN_LEVEL || professionLevel > Profession.MAX_LEVEL)) return false;
        if (type == QuestType.GLOBAL_EVENT && string(json, "targetKey", "").isBlank()) return false;
        if (type == QuestType.TALK_TO_NPC && string(json, "targetKey", "").isBlank()) return false;
        if (type == QuestType.HUNT && !isVanillaEntityType(string(json, "targetKey", ""))) return false;
        if (type == QuestType.COLLECT && !isQuestItem(string(json, "targetKey", ""))) return false;
        if (type == QuestType.REACH_LOCATION && !hasWorldNavigation(object(json, "navigation"))) return false;
        return true;
    }

    private Profession parseProfession(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Profession.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unknown quest profession '" + value + "'.");
            return null;
        }
    }

    private boolean hasWorldNavigation(JsonObject navigation) {
        return navigation != null && (!string(navigation, "structure", "").isBlank() || !stringList(navigation, "biomes").isEmpty());
    }

    /** Accepts normal Minecraft materials and canonical PixelRPG item definition IDs. */
    private boolean isQuestItem(String key) {
        if (key == null || key.isBlank()) return false;
        String normalized = canonicalItemId(key);
        try {
            Material material = Material.matchMaterial(key.trim());
            if (material != null && material.isItem() && !normalized.startsWith("pixelrpg:")) return true;
        } catch (IllegalArgumentException ignored) {
        }
        if (itemDefinitions.find(normalized).isPresent()) return true;
        return normalized.startsWith("pixelrpg:") && normalized.length() > "pixelrpg:".length();
    }

    private String canonicalItemId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith("pixelrpg:")) return value;
        String body = value.substring("pixelrpg:".length());
        int slash = body.indexOf('/');
        if (slash > 0) body = body.substring(0, slash) + ":" + body.substring(slash + 1);
        return "pixelrpg:" + body;
    }

    private boolean isVanillaEntityType(String key) {
        try {
            var type = org.bukkit.entity.EntityType.valueOf(key.trim().toUpperCase(Locale.ROOT));
            return type.isAlive() && type != org.bukkit.entity.EntityType.PLAYER;
        } catch (IllegalArgumentException exception) {
            return false;
        }
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
        for (Map.Entry<String, List<String>> entry : prerequisitesByQuest.entrySet()) {
            entry.getValue().stream().filter(id -> !questsById.containsKey(id))
                    .forEach(id -> plugin.getLogger().warning("Quest '" + entry.getKey() + "' references unknown prerequisite '" + id + "'."));
        }
        for (Map.Entry<String, List<String>> entry : followUpsByQuest.entrySet()) {
            entry.getValue().stream().filter(id -> !questsById.containsKey(id))
                    .forEach(id -> plugin.getLogger().warning("Quest '" + entry.getKey() + "' references unknown follow-up quest '" + id + "'."));
        }
        for (String questId : questsById.keySet()) {
            if (hasPrerequisiteCycle(questId, new java.util.HashSet<>(), new java.util.HashSet<>())) {
                plugin.getLogger().warning("Quest '" + questId + "' participates in a prerequisite cycle.");
            }
        }
    }

    private boolean hasPrerequisiteCycle(String questId, java.util.Set<String> visiting, java.util.Set<String> visited) {
        if (!visiting.add(questId)) return true;
        if (visited.contains(questId)) {
            visiting.remove(questId);
            return false;
        }
        for (String prerequisite : prerequisitesByQuest.getOrDefault(questId, List.of())) {
            if (questsById.containsKey(prerequisite) && hasPrerequisiteCycle(prerequisite, visiting, visited)) return true;
        }
        visiting.remove(questId);
        visited.add(questId);
        return false;
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
    private QuestType parseType(String value) { try { return QuestType.valueOf(value.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException exception) { return QuestType.HUNT; } }
}
