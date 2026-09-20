package de.pixelrpg.rpg.dialogue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public final class DataDrivenDialogueLoader {
    private final Plugin plugin;
    private final Gson gson = new Gson();

    public DataDrivenDialogueLoader(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void loadInto(DialogueTreeService service) {
        File root = new File(plugin.getDataFolder(), "dialogues");
        if (!root.exists()) {
            copyDefaults(root);
        }
        if (!root.isDirectory()) return;

        File[] files = root.listFiles((directory, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                JsonObject rootObject = JsonParser.parseReader(reader).getAsJsonObject();
                service.register(parseTree(rootObject, file.getName()));
            } catch (IOException | RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to load dialogue file " + file.getName(), exception);
            }
        }
    }

    private DialogueTree parseTree(JsonObject root, String sourceName) {
        String id = required(root, "id", sourceName);
        String startNode = required(root, "start", sourceName);
        JsonArray rawNodes = root.getAsJsonArray("nodes");
        if (rawNodes == null) throw new IllegalArgumentException("Missing nodes in " + sourceName);

        List<DialogueNode> nodes = new ArrayList<>();
        rawNodes.forEach(element -> {
            JsonObject node = element.getAsJsonObject();
            String nodeId = required(node, "id", sourceName);
            String title = node.has("title") ? node.get("title").getAsString() : id;
            List<DialogBody> body = new ArrayList<>();
            if (node.has("body")) {
                node.getAsJsonArray("body").forEach(entry ->
                        body.add(DialogBody.plainMessage(Component.text(entry.getAsString()))));
            }
            List<DialogueOption> options = new ArrayList<>();
            if (node.has("options")) {
                node.getAsJsonArray("options").forEach(entry -> {
                    JsonObject option = entry.getAsJsonObject();
                    String label = required(option, "label", sourceName);
                    String next = option.has("next") && !option.get("next").isJsonNull()
                            ? option.get("next").getAsString() : null;
                    boolean completes = option.has("completes") && option.get("completes").getAsBoolean();
                    options.add(new DialogueOption(
                            Component.text(label),
                            DialogueCondition.always(),
                            next,
                            DialogueOption.DialogueAction.none(),
                            completes));
                });
            }
            boolean once = node.has("once") && node.get("once").getAsBoolean();
            nodes.add(new DialogueNode(nodeId, Component.text(title), body, options, once));
        });
        return new DialogueTree(id, startNode, nodes);
    }

    private String required(JsonObject object, String key, String sourceName) {
        if (!object.has(key) || object.get(key).isJsonNull() || object.get(key).getAsString().isBlank()) {
            throw new IllegalArgumentException("Missing '" + key + "' in " + sourceName);
        }
        return object.get(key).getAsString();
    }

    private void copyDefaults(File root) {
        if (!root.mkdirs()) return;
        try {
            Files.writeString(new File(root, "resident.json").toPath(), """
                    {
                      "id": "npc.resident.basic",
                      "start": "greeting",
                      "nodes": [
                        {
                          "id": "greeting",
                          "title": "Bewohner",
                          "body": [
                            "Die Welt war schon da, lange bevor du sie betreten hast.",
                            "Wenn du wissen willst, was hier geschieht, sprich mit den Menschen, die hier leben."
                          ],
                          "once": false,
                          "options": [
                            {
                              "label": "Danke.",
                              "next": null,
                              "completes": true
                            }
                          ]
                        }
                      ]
                    }
                    """);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to create default resident dialogue.", exception);
        }
    }
}
