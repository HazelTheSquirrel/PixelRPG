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

/** Owns user-editable JSON data files in the plugin data directory. */
public final class JsonDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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
        Path path = dataFolder.resolve(fileName).normalize();
        if (!path.startsWith(dataFolder)) {
            throw new IllegalArgumentException("JSON path escapes data directory: " + fileName);
        }
        if (Files.notExists(path)) {
            copyDefault(fileName, path);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                throw new IllegalStateException("JSON root must be an object: " + path);
            }
            JsonObject object = element.getAsJsonObject();
            if (fileName.equals("recipes/crafting-recipes.json")) {
                if (!object.has("recipes")) {
                    return repairCraftingRecipes(path);
                }
                JsonElement recipes = object.get("recipes");
                if (!recipes.isJsonArray()) {
                    return repairCraftingRecipes(path);
                }
                for (JsonElement recipe : recipes.getAsJsonArray()) {
                    if (!recipe.isJsonObject() || !recipe.getAsJsonObject().has("category")) {
                        return repairCraftingRecipes(path);
                    }
                }
            }
            return object;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load JSON data file: " + path, exception);
        }
    }

    public void save(String fileName, JsonObject object) {
        Path path = dataFolder.resolve(fileName).normalize();
        if (!path.startsWith(dataFolder)) {
            throw new IllegalArgumentException("JSON path escapes data directory: " + fileName);
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(object, writer);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to save JSON data file " + path, exception);
        }
    }

    private JsonObject repairCraftingRecipes(Path path) {
        Path backup = path.resolveSibling("crafting-recipes.legacy-" + Instant.now().toEpochMilli() + ".json");
        try {
            Files.move(path, backup);
            plugin.getLogger().warning("Found an incompatible crafting-recipes.json. The old file was backed up to " + backup.getFileName() + " and the current bundled recipe definitions are being installed.");
            copyDefault("recipes/crafting-recipes.json", path);

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject() || !element.getAsJsonObject().has("recipes")) {
                    throw new IllegalStateException("Bundled crafting-recipes.json does not contain a 'recipes' array");
                }
                return element.getAsJsonObject();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to repair incompatible crafting recipe data: " + path, exception);
        }
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
}
