package de.pixelrpg.rpg.content;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public final class JsonContentCatalogLoader implements AutoCloseable {
    private final Plugin plugin;
    private final Path file;
    private final ExecutorService executor;

    public JsonContentCatalogLoader(Plugin plugin, Path file, ExecutorService executor) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.file = Objects.requireNonNull(file, "file");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.gson = new Gson();
    }

    public CompletableFuture<ContentCatalog> loadAsync() {
        return CompletableFuture.supplyAsync(this::load, executor);
    }

    private ContentCatalog load() {
        if (!Files.exists(file)) {
            return new ContentCatalog(Map.of());
        }

        Map<ContentId, TextContent> texts = new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject textObject = root.has("texts") && root.get("texts").isJsonObject()
                    ? root.getAsJsonObject("texts")
                    : new JsonObject();

            for (Map.Entry<String, JsonElement> entry : textObject.entrySet()) {
                ContentId id = new ContentId(entry.getKey());
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Content text must be a string: " + id.value());
                }
                texts.put(id, new TextContent(id, entry.getValue().getAsString()));
            }
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load content catalog: " + file, exception);
            throw new IllegalStateException("Content catalog could not be loaded", exception);
        }

        return new ContentCatalog(texts);
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
