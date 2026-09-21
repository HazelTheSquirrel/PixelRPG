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
    private final PlayerKnowledgeStore knowledgeStore;
    private final WorldState worldState;
    private final NpcKnowledgeStore npcKnowledgeStore;
    private final NpcRelationshipStore npcRelationshipStore;
    private final de.pixelrpg.rpg.lore.LoreRegistry loreRegistry;

    public DataDrivenDialogueLoader(Plugin plugin, PlayerKnowledgeStore knowledgeStore, WorldState worldState) {
        this(plugin, knowledgeStore, worldState, null, null, null);
    }

    public DataDrivenDialogueLoader(
            Plugin plugin,
            PlayerKnowledgeStore knowledgeStore,
            WorldState worldState,
            NpcKnowledgeStore npcKnowledgeStore,
            NpcRelationshipStore npcRelationshipStore,
            de.pixelrpg.rpg.lore.LoreRegistry loreRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.knowledgeStore = Objects.requireNonNull(knowledgeStore, "knowledgeStore");
        this.worldState = Objects.requireNonNull(worldState, "worldState");
        this.npcKnowledgeStore = npcKnowledgeStore;
        this.npcRelationshipStore = npcRelationshipStore;
        this.loreRegistry = loreRegistry;
    }

    public void loadInto(DialogueTreeService service) {
        File root = new File(plugin.getDataFolder(), "dialogues");
        if (!root.exists()) copyDefaults(root);
        if (!root.isDirectory()) return;
        loadDirectory(service, root);
    }

    private void loadDirectory(DialogueTreeService service, File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                loadDirectory(service, file);
                continue;
            }
            if (!file.getName().endsWith(".json")) continue;
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
                    String condition = option.has("condition") ? option.get("condition").getAsString() : "always";
                    String action = option.has("action") ? option.get("action").getAsString() : "none";
                    options.add(new DialogueOption(
                            Component.text(label),
                            parseCondition(condition, sourceName),
                            next,
                            parseAction(action, sourceName),
                            completes));
                });
            }
            boolean once = node.has("once") && node.get("once").getAsBoolean();
            nodes.add(new DialogueNode(nodeId, Component.text(title), body, options, once));
        });
        return new DialogueTree(id, startNode, nodes);
    }

    private DialogueCondition parseCondition(String raw, String sourceName) {
        if (raw == null || raw.isBlank() || raw.equals("always")) return DialogueConditions.always();
        if (raw.startsWith("knowledge:")) {
            return DialogueConditions.playerKnows(knowledgeStore, value(raw, "knowledge:", sourceName));
        }
        if (raw.startsWith("world:")) {
            return DialogueConditions.worldFlag(worldState, value(raw, "world:", sourceName));
        }
        if (raw.startsWith("npc:")) {
            return DialogueConditions.npc(value(raw, "npc:", sourceName));
        }
        throw new IllegalArgumentException("Unknown dialogue condition '" + raw + "' in " + sourceName);
    }

    private DialogueOption.DialogueAction parseAction(String raw, String sourceName) {
        if (raw == null || raw.isBlank() || raw.equals("none")) return DialogueActions.none();
        if (raw.startsWith("learn:")) {
            return DialogueActions.learn(knowledgeStore, value(raw, "learn:", sourceName));
        }
        if (raw.startsWith("world:set:")) {
            return DialogueActions.setWorldFlag(worldState, value(raw, "world:set:", sourceName));
        }
        if (raw.startsWith("world:clear:")) {
            return DialogueActions.clearWorldFlag(worldState, value(raw, "world:clear:", sourceName));
        }
        throw new IllegalArgumentException("Unknown dialogue action '" + raw + "' in " + sourceName);
    }

    private String value(String raw, String prefix, String sourceName) {
        String value = raw.substring(prefix.length()).trim();
        if (value.isBlank()) throw new IllegalArgumentException("Missing value for '" + prefix + "' in " + sourceName);
        return value;
    }

    private String required(JsonObject object, String key, String sourceName) {
        if (!object.has(key) || object.get(key).isJsonNull() || object.get(key).getAsString().isBlank()) {
            throw new IllegalArgumentException("Missing '" + key + "' in " + sourceName);
        }
        return object.get(key).getAsString();
    }

    private void copyDefaults(File root) {
        File npcDirectory = new File(root, "npc");
        if (!npcDirectory.mkdirs() && !npcDirectory.isDirectory()) return;
        try {
            Files.writeString(new File(npcDirectory, "resident.json").toPath(), """
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
                              "action": "learn:world.basic.resident",
                              "completes": true
                            }
                          ]
                        }
                      ]
                    }
                    """, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to create default resident dialogue.", exception);
        }
    }
}
