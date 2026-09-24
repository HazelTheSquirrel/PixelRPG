package de.pixelrpg.rpg.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/** Owns editable JSON content copies in the plugin data directory. */
public final class JsonDataManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Plugin plugin;
    private final Path dataFolder;

    public JsonDataManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.dataFolder = plugin.getDataFolder().toPath().resolve("data");
    }

    public void initialize() {
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create JSON data directory: " + dataFolder, exception);
        }
    }

    public JsonObject load(String fileName) {
        Path path = resolve(fileName);
        if (Files.notExists(path)) {
            copyDefault(fileName, path);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                throw new IllegalStateException("JSON root must be an object: " + path);
            }
            return element.getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load JSON data file: " + path, exception);
        }
    }

    public void save(String fileName, JsonObject object) {
        Objects.requireNonNull(object, "object");
        Path path = resolve(fileName);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(object, writer);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Unable to save JSON data file " + path, exception);
        }
    }

    private Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("JSON file name must not be blank");
        }
        Path path = dataFolder.resolve(fileName).normalize();
        if (!path.startsWith(dataFolder)) {
            throw new IllegalArgumentException("JSON path escapes data directory: " + fileName);
        }
        return path;
    }

    private void copyDefault(String fileName, Path target) {
        String resourcePath = "data/" + fileName;
        try (var input = plugin.getResource(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing bundled JSON resource: " + resourcePath);
            }
            Files.createDirectories(target.getParent());
            Files.copy(input, target);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create default JSON data file: " + target, exception);
        }
    }

    public void backup(String fileName) {
        Path source = resolve(fileName);
        if (Files.notExists(source)) {
            return;
        }
        Path backup = source.resolveSibling(source.getFileName() + ".backup-" + Instant.now().toEpochMilli());
        try {
            Files.move(source, backup);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to back up JSON data file: " + source, exception);
        }
    }
}
