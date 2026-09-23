package de.pixelrpg.rpg.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public final class QuestRepository {
    private static final int MAX_ACTIVE_QUESTS = 5;

    private final Plugin plugin;
    private final Path file;
    private final ExecutorService executor;
    private final Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
    private volatile boolean loaded;

    public QuestRepository(Plugin plugin, Path file, ExecutorService executor) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.file = Objects.requireNonNull(file, "file");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<Void> loadAsync() {
        return CompletableFuture.runAsync(this::load, executor);
    }

    private void load() {
        if (!Files.exists(file)) throw new IllegalStateException("Missing quest definitions: " + file);
        Map<String, QuestDefinition> loadedDefinitions = new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray entries = root.getAsJsonArray("definitions");
            if (entries == null) throw new IllegalArgumentException("Missing quest definitions array");
            for (JsonElement element : entries) {
                QuestDefinition definition = parse(element);
                if (loadedDefinitions.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException("Duplicate quest id: " + definition.id());
                }
            }
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load quest definitions.", exception);
            throw new IllegalStateException("Quest definitions could not be loaded", exception);
        }

        validateReferences(loadedDefinitions);
        synchronized (definitions) {
            definitions.clear();
            definitions.putAll(loadedDefinitions);
        }
        loaded = true;
    }

    private QuestDefinition parse(JsonElement element) {
        if (!element.isJsonObject()) throw new IllegalArgumentException("Quest definition must be an object");
        JsonObject json = element.getAsJsonObject();
        String id = requiredString(json, "id");
        QuestType type = QuestType.valueOf(requiredString(json, "type").toUpperCase(Locale.ROOT));
        JsonObject rewardJson = object(json, "reward");
        JsonObject navigationJson = object(json, "navigation");
        return new QuestDefinition(
                id,
                requiredString(json, "title"),
                requiredString(json, "description"),
                type,
                string(json, "targetKey", ""),
                number(json, "requiredAmount", 1),
                number(json, "recommendedLevel", 1),
                number(json, "categoryLevel", number(json, "recommendedLevel", 1)),
                new QuestReward(
                        numberDouble(rewardJson, "money", 0.0D),
                        numberLong(rewardJson, "experience", 0L),
                        stringList(rewardJson, "items"),
                        string(rewardJson, "companionId", "")
                ),
                number(rewardJson, "durationMinutes", 0),
                string(json, "questGiverNpcId", ""),
                new QuestNavigation(
                        string(navigationJson, "structure", ""),
                        stringList(navigationJson, "biomes"),
                        number(navigationJson, "radius", 1024),
                        bool(navigationJson, "findUnexplored", false)
                ),
                string(json, "profession", "").toUpperCase(Locale.ROOT),
                number(json, "requiredProfessionLevel", 1),
                mergePrerequisites(json),
                stringList(json, "followUpQuestIds")
        );
    }

    private void validateReferences(Map<String, QuestDefinition> values) {
        for (QuestDefinition definition : values.values()) {
            for (String prerequisite : definition.prerequisites()) {
                if (!values.containsKey(prerequisite)) {
                    throw new IllegalArgumentException("Quest '" + definition.id() + "' references missing prerequisite '" + prerequisite + "'");
                }
            }
            for (String followUp : definition.followUpQuestIds()) {
                if (!values.containsKey(followUp)) {
                    throw new IllegalArgumentException("Quest '" + definition.id() + "' references missing follow-up '" + followUp + "'");
                }
            }
        }
        for (QuestDefinition definition : values.values()) {
            if (hasCycle(definition.id(), values, new java.util.HashSet<>(), new java.util.HashSet<>())) {
                throw new IllegalArgumentException("Quest prerequisite cycle detected at '" + definition.id() + "'");
            }
        }
    }

    private boolean hasCycle(String id, Map<String, QuestDefinition> values, java.util.Set<String> visiting, java.util.Set<String> visited) {
        if (!visiting.add(id)) return true;
        if (visited.contains(id)) {
            visiting.remove(id);
            return false;
        }
        for (String prerequisite : values.get(id).prerequisites()) {
            if (hasCycle(prerequisite, values, visiting, visited)) return true;
        }
        visiting.remove(id);
        visited.add(id);
        return false;
    }

    private static List<String> mergePrerequisites(JsonObject json) {
        List<String> result = new ArrayList<>(stringList(json, "prerequisites"));
        JsonObject requirements = object(json, "requirements");
        String previous = string(requirements, "previousQuest", "");
        if (!previous.isBlank() && !result.contains(previous)) result.add(previous);
        return List.copyOf(result);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public int maxActiveQuests() {
        return MAX_ACTIVE_QUESTS;
    }

    public QuestDefinition get(String id) {
        synchronized (definitions) {
            return definitions.get(id);
        }
    }

    public List<QuestDefinition> all() {
        synchronized (definitions) {
            return List.copyOf(definitions.values());
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null;
    }

    private static String requiredString(JsonObject object, String key) {
        String value = string(object, key, "");
        if (value.isBlank()) throw new IllegalArgumentException("Missing '" + key + "'");
        return value.strip();
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }

    private static int number(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.getAsJsonPrimitive(key).getAsInt() : fallback;
    }

    private static long numberLong(JsonObject object, String key, long fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.getAsJsonPrimitive(key).getAsLong() : fallback;
    }

    private static double numberDouble(JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.getAsJsonPrimitive(key).getAsDouble() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isBoolean() ? object.getAsJsonPrimitive(key).getAsBoolean() : fallback;
    }

    private static List<String> stringList(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) if (element.isJsonPrimitive()) result.add(element.getAsString());
        return List.copyOf(result);
    }
}
