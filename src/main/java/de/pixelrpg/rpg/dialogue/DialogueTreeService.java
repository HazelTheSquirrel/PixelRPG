package de.pixelrpg.rpg.dialogue;

import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DialogueTreeService {
    private final DialogueEngine dialogueEngine;
    private final DialogueProgressStore progressStore;
    private final Map<String, DialogueTree> trees = new ConcurrentHashMap<>();

    public DialogueTreeService(Plugin plugin, DialogueEngine dialogueEngine) {
        this.dialogueEngine = Objects.requireNonNull(dialogueEngine, "dialogueEngine");
        this.progressStore = new DialogueProgressStore(Objects.requireNonNull(plugin, "plugin"));
        this.progressStore.load();
    }

    public void register(DialogueTree tree) { trees.put(tree.id(), tree); }

    public void open(Player player, String treeId) {
        open(player, DialogueContext.forPlayer(player), treeId, () -> player.closeDialog());
    }

    public void open(Player player, DialogueContext context, String treeId) {
        open(player, context, treeId, () -> player.closeDialog());
    }

    public void open(Player player, DialogueContext context, String treeId, Runnable completion) {
        DialogueTree tree = trees.get(treeId);
        if (tree == null) {
            dialogueEngine.openUnavailable(player, "Dialog", "Dieser Dialog ist nicht verfügbar.");
            return;
        }
        open(player, context, tree, tree.startNodeId(), completion);
    }

    public void open(Player player, DialogueTree tree, String nodeId) {
        open(player, DialogueContext.forPlayer(player), tree, nodeId, () -> player.closeDialog());
    }

    public void open(Player player, DialogueContext context, DialogueTree tree, String nodeId) {
        open(player, context, tree, nodeId, () -> player.closeDialog());
    }

    public void open(Player player, DialogueContext context, DialogueTree tree, String nodeId, Runnable completion) {
        DialogueNode node = tree.node(nodeId).orElse(null);
        if (node == null) {
            dialogueEngine.openUnavailable(player, "Dialog", "Dieser Dialogschritt ist nicht verfügbar.");
            return;
        }

        String stateKey = stateKey(context, tree, node);
        if (node.once() && progressStore.hasSeen(player.getUniqueId(), stateKey)) {
            dialogueEngine.openUnavailable(player, "Dialog", "Diesen Dialog hast du bereits gesehen.");
            return;
        }

        progressStore.markSeen(player.getUniqueId(), stateKey);
        var actions = new ArrayList<ActionButton>();
        for (DialogueOption option : node.options()) {
            if (!option.condition().test(context)) continue;
            actions.add(dialogueEngine.actionButton(option.label(),
                    target -> select(target, context, tree, node, option, completion)));
        }

        if (actions.isEmpty()) {
            dialogueEngine.openNotice(player, node.title(), Component.text("Dialog beendet."),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }

        dialogueEngine.openMultiAction(player, node.title(), node.body(), actions,
                Math.min(2, Math.max(1, actions.size())));
    }

    public void shutdown() { progressStore.shutdown(); }

    private void select(Player player, DialogueContext context, DialogueTree tree,
                         DialogueNode node, DialogueOption option, Runnable completion) {
        if (option.completesCurrentNode()) progressStore.markCompleted(player.getUniqueId(), stateKey(context, tree, node));
        option.action().execute(context);
        if (option.nextNodeId() != null && !option.nextNodeId().isBlank()) {
            open(player, context, tree, option.nextNodeId(), completion);
        } else {
            completion.run();
        }
    }

    private String stateKey(DialogueContext context, DialogueTree tree, DialogueNode node) {
        String npcId = context.npcOptional().map(npc -> npc.id()).orElse("player");
        return npcId + ":" + tree.id() + ":" + node.id();
    }
}
