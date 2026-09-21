package de.pixelrpg.rpg.dialogue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pixelrpg.rpg.lore.LoreRegistry;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.npc.NpcFaction;
import de.pixelrpg.rpg.npc.NpcProfileStore;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.Quest;
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
    private final LoreRegistry loreRegistry;
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final NpcProfileStore profileStore;
    private final PlayerFactionRelationshipStore playerFactionRelationships;
    private final NpcNetworkRelationshipStore npcNetworkRelationshipStore;
    private final FactionRelationshipStore factionRelationshipStore;

    public DataDrivenDialogueLoader(Plugin plugin, PlayerKnowledgeStore knowledgeStore, WorldState worldState) {
        this(plugin, knowledgeStore, worldState, null, null, null, null, null, null, null, null, null);
    }

    public DataDrivenDialogueLoader(
            Plugin plugin,
            PlayerKnowledgeStore knowledgeStore,
            WorldState worldState,
            NpcKnowledgeStore npcKnowledgeStore,
            NpcRelationshipStore npcRelationshipStore,
            LoreRegistry loreRegistry,
            QuestManager questManager,
            PlayerProfileManager profileManager,
            NpcProfileStore profileStore,
            PlayerFactionRelationshipStore playerFactionRelationships,
            NpcNetworkRelationshipStore npcNetworkRelationshipStore,
            FactionRelationshipStore factionRelationshipStore) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.knowledgeStore = Objects.requireNonNull(knowledgeStore, "knowledgeStore");
        this.worldState = Objects.requireNonNull(worldState, "worldState");
        this.npcKnowledgeStore = npcKnowledgeStore;
        this.npcRelationshipStore = npcRelationshipStore;
        this.loreRegistry = loreRegistry;
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.profileStore = profileStore;
        this.playerFactionRelationships = playerFactionRelationships;
        this.npcNetworkRelationshipStore = npcNetworkRelationshipStore;
        this.factionRelationshipStore = factionRelationshipStore;
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
        if (raw.startsWith("npc_knowledge:")) {
            requireStore(npcKnowledgeStore, "npc knowledge", sourceName);
            return DialogueConditions.npcKnows(npcKnowledgeStore, value(raw, "npc_knowledge:", sourceName));
        }
        if (raw.equals("met")) {
            requireStore(npcRelationshipStore, "NPC relationships", sourceName);
            return DialogueConditions.hasMet(npcRelationshipStore);
        }
        if (raw.startsWith("faction:")) {
            if (profileStore == null) throw new IllegalStateException("Faction condition requires NPC profiles in " + sourceName);
            NpcFaction faction = NpcFaction.valueOf(value(raw, "faction:", sourceName).toUpperCase(java.util.Locale.ROOT));
            return new DialogueCondition() {
                @Override public boolean test(org.bukkit.entity.Player player) { return false; }
                @Override public boolean test(DialogueContext context) {
                    return context.npcOptional().flatMap(npc -> profileStore.get(npc.id())).map(profile -> profile.faction() == faction).orElse(false);
                }
            };
        }
        if (raw.startsWith("npc_relation:")) {
            requireStore(npcNetworkRelationshipStore, "NPC network relationships", sourceName);
            String[] parts = value(raw, "npc_relation:", sourceName).split(":", 3);
            if (parts.length != 3) throw new IllegalArgumentException("npc_relation requires npcId:relation:minimum in " + sourceName);
            return DialogueConditions.npcRelationship(npcNetworkRelationshipStore, parts[0], parts[1], parseInteger(parts[2], sourceName));
        }
        if (raw.startsWith("player_faction:")) {
            requireStore(playerFactionRelationships, "player faction relationships", sourceName);
            String[] parts = value(raw, "player_faction:", sourceName).split(":", 2);
            if (parts.length != 2) throw new IllegalArgumentException("player_faction requires faction:minimum in " + sourceName);
            return DialogueConditions.playerFactionRelationship(playerFactionRelationships, parseFaction(parts[0], sourceName), parseInteger(parts[1], sourceName));
        }
        if (raw.startsWith("faction_relation:")) {
            requireStore(factionRelationshipStore, "faction relationships", sourceName);
            String[] parts = value(raw, "faction_relation:", sourceName).split(":", 3);
            if (parts.length != 3) throw new IllegalArgumentException("faction_relation requires first:second:minimum in " + sourceName);
            return DialogueConditions.factionRelationship(factionRelationshipStore, parseFaction(parts[0], sourceName), parseFaction(parts[1], sourceName), parseInteger(parts[2], sourceName));
        }
        if (raw.startsWith("quest_active:")) {
            requireQuests(sourceName);
            String questId = value(raw, "quest_active:", sourceName);
            return player -> profileManager.getProfile(player.getUniqueId()).map(profile -> profile.hasActiveQuest(questId)).orElse(false);
        }
        if (raw.startsWith("quest_completed:")) {
            requireQuests(sourceName);
            String questId = value(raw, "quest_completed:", sourceName);
            return player -> profileManager.getProfile(player.getUniqueId()).map(profile -> profile.hasCompletedQuest(questId)).orElse(false);
        }
        if (raw.startsWith("world:")) {
            return DialogueConditions.worldFlag(worldState, value(raw, "world:", sourceName));
        }
        if (raw.startsWith("npc:")) {
            return DialogueConditions.npc(value(raw, "npc:", sourceName));
        }
        if (raw.startsWith("dimension:")) {
            return DialogueConditions.dimension(value(raw, "dimension:", sourceName));
        }
        if (raw.startsWith("item:")) {
            return DialogueConditions.hasItem(value(raw, "item:", sourceName));
        }
        throw new IllegalArgumentException("Unknown dialogue condition '" + raw + "' in " + sourceName);
    }

    private DialogueOption.DialogueAction parseAction(String raw, String sourceName) {
        if (raw == null || raw.isBlank() || raw.equals("none")) return DialogueActions.none();
        if (raw.indexOf(';') >= 0) {
            String[] actions = raw.split(";");
            List<DialogueOption.DialogueAction> parsed = new ArrayList<>();
            for (String action : actions) {
                if (!action.isBlank()) parsed.add(parseAction(action.trim(), sourceName));
            }
            return context -> parsed.forEach(action -> action.execute(context));
        }
        if (raw.equals("meet")) {
            requireStore(npcRelationshipStore, "NPC relationships", sourceName);
            return DialogueActions.meetNpc(npcRelationshipStore);
        }
        if (raw.startsWith("relationship:")) {
            requireStore(npcRelationshipStore, "NPC relationships", sourceName);
            String value = value(raw, "relationship:", sourceName);
            String[] parts = value.split(":", 2);
            if (parts.length != 2) throw new IllegalArgumentException("Relationship action requires relation:amount in " + sourceName);
            return DialogueActions.adjustRelationship(npcRelationshipStore, parts[0], parseInteger(parts[1], sourceName));
        }
        if (raw.startsWith("npc_relationship:")) {
            requireStore(npcNetworkRelationshipStore, "NPC network relationships", sourceName);
            String[] parts = value(raw, "npc_relationship:", sourceName).split(":", 3);
            if (parts.length != 3) throw new IllegalArgumentException("npc_relationship requires npcId:relation:amount in " + sourceName);
            return DialogueActions.adjustNpcRelationship(npcNetworkRelationshipStore, parts[0], parts[1], parseInteger(parts[2], sourceName));
        }
        if (raw.startsWith("player_faction_relationship:")) {
            requireStore(playerFactionRelationships, "player faction relationships", sourceName);
            String[] parts = value(raw, "player_faction_relationship:", sourceName).split(":", 2);
            if (parts.length != 2) throw new IllegalArgumentException("player_faction_relationship requires faction:amount in " + sourceName);
            return DialogueActions.adjustPlayerFactionRelationship(playerFactionRelationships, parseFaction(parts[0], sourceName), parseInteger(parts[1], sourceName));
        }
        if (raw.startsWith("faction_relationship:")) {
            requireStore(factionRelationshipStore, "faction relationships", sourceName);
            String[] parts = value(raw, "faction_relationship:", sourceName).split(":", 3);
            if (parts.length != 3) throw new IllegalArgumentException("faction_relationship requires first:second:amount in " + sourceName);
            return DialogueActions.adjustFactionRelationship(factionRelationshipStore, parseFaction(parts[0], sourceName), parseFaction(parts[1], sourceName), parseInteger(parts[2], sourceName));
        }
        if (raw.startsWith("quest:accept:")) {
            requireQuests(sourceName);
            String questId = value(raw, "quest:accept:", sourceName);
            return questAction(player -> { Quest quest = questManager.getRepository().getQuest(questId); return quest != null && questManager.acceptQuest(player, quest); });
        }
        if (raw.startsWith("quest:complete:")) {
            requireQuests(sourceName);
            String questId = value(raw, "quest:complete:", sourceName);
            return questAction(player -> questManager.completeQuest(player, questId));
        }
        if (raw.startsWith("quest:abandon:")) {
            requireQuests(sourceName);
            String questId = value(raw, "quest:abandon:", sourceName);
            return questAction(player -> questManager.abandonQuest(player, questId));
        }
        if (raw.startsWith("learn:")) {
            return DialogueActions.learn(knowledgeStore, value(raw, "learn:", sourceName));
        }
        if (raw.startsWith("learn_npc:")) {
            requireStore(npcKnowledgeStore, "NPC knowledge", sourceName);
            return DialogueActions.learnNpc(npcKnowledgeStore, value(raw, "learn_npc:", sourceName));
        }
        if (raw.startsWith("lore:")) {
            requireLore(sourceName);
            return DialogueActions.discoverLore(loreRegistry, knowledgeStore, value(raw, "lore:", sourceName));
        }
        if (raw.startsWith("world:set:")) {
            return DialogueActions.setWorldFlag(worldState, value(raw, "world:set:", sourceName));
        }
        if (raw.startsWith("world:clear:")) {
            return DialogueActions.clearWorldFlag(worldState, value(raw, "world:clear:", sourceName));
        }
        throw new IllegalArgumentException("Unknown dialogue action '" + raw + "' in " + sourceName);
    }

    private DialogueOption.DialogueAction questAction(java.util.function.Predicate<org.bukkit.entity.Player> action) {
        return new DialogueOption.DialogueAction() {
            @Override public void execute(org.bukkit.entity.Player player) { action.test(player); }
            @Override public void execute(DialogueContext context) { action.test(context.player()); }
        };
    }

    private NpcFaction parseFaction(String raw, String sourceName) {
        try {
            return NpcFaction.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown faction '" + raw + "' in " + sourceName, exception);
        }
    }

    private int parseInteger(String raw, String sourceName) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid relationship amount in " + sourceName, exception);
        }
    }

    private void requireStore(Object store, String name, String sourceName) {
        if (store == null) throw new IllegalStateException("Dialogue file " + sourceName + " requires " + name + " but the store is not configured.");
    }

    private void requireQuests(String sourceName) {
        if (questManager == null || profileManager == null) {
            throw new IllegalStateException("Dialogue file " + sourceName + " requires quest integration but it is not configured.");
        }
    }

    private void requireLore(String sourceName) {
        if (loreRegistry == null) throw new IllegalStateException("Dialogue file " + sourceName + " requires the lore registry but it is not configured.");
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
