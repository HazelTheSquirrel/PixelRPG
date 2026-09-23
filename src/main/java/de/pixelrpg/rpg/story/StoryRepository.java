package de.pixelrpg.rpg.story;

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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public final class StoryRepository implements AutoCloseable {
    private final Plugin plugin;
    private final Path file;
    private final ExecutorService executor;

    public StoryRepository(Plugin plugin, Path file, ExecutorService executor) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.file = Objects.requireNonNull(file, "file");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<List<StoryChapter>> loadAsync() {
        return CompletableFuture.supplyAsync(this::load, executor);
    }

    private List<StoryChapter> load() {
        if (!Files.exists(file)) {
            throw new IllegalStateException("Story definition file does not exist: " + file);
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray chapters = root.has("chapters") && root.get("chapters").isJsonArray()
                    ? root.getAsJsonArray("chapters")
                    : new JsonArray();

            Map<String, StoryChapter> byId = new LinkedHashMap<>();
            for (JsonElement element : chapters) {
                JsonObject chapter = element.getAsJsonObject();
                String id = requiredString(chapter, "id");
                int order = requiredInt(chapter, "order");
                String title = requiredString(chapter, "title");
                long expReward = chapter.has("exp-reward") ? chapter.get("exp-reward").getAsLong() : 0L;

                JsonArray lines = chapter.has("dialogue") && chapter.get("dialogue").isJsonArray()
                        ? chapter.getAsJsonArray("dialogue")
                        : new JsonArray();
                List<String> dialogueLines = new ArrayList<>();
                for (JsonElement line : lines) {
                    dialogueLines.add(line.getAsString());
                }

                StoryChapter parsed = new StoryChapter(order, id, title, dialogueLines, expReward);
                if (byId.putIfAbsent(id, parsed) != null) {
                    throw new IllegalArgumentException("Duplicate story chapter id: " + id);
                }
            }

            List<StoryChapter> result = new ArrayList<>(byId.values());
            result.sort(Comparator.comparingInt(StoryChapter::order));
            validateOrder(result);
            return List.copyOf(result);
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load story definitions: " + file, exception);
            throw new IllegalStateException("Story definitions could not be loaded", exception);
        }
    }

    private void validateOrder(List<StoryChapter> chapters) {
        int expected = 0;
        for (StoryChapter chapter : chapters) {
            if (chapter.order() != expected++) {
                throw new IllegalArgumentException("Story chapter order must be contiguous from 0; found " + chapter.order());
            }
        }
    }

    private String requiredString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing story field: " + key);
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) throw new IllegalArgumentException("Blank story field: " + key);
        return value;
    }

    private int requiredInt(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing story field: " + key);
        }
        return object.get(key).getAsInt();
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
