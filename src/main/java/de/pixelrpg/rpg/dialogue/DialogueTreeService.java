package de.pixelrpg.rpg.dialogue;

import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Executes persistent, branching PixelRPG dialogue trees through native Minecraft dialogs. */
public final class DialogueTreeService {
    private final DialogueEngine dialogueEngine;
    private final DialogueProgressStore progressStore;
    private final Map<String, DialogueTree> trees = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<String>> navigation = new ConcurrentHashMap<>();

    public DialogueTreeService(Plugin plugin, DialogueEngine dialogueEngine) {
        this.dialogueEngine = Objects.requireNonNull(dialogueEngine, "dialogueEngine");
        this.progressStore = new DialogueProgressStore(Objects.requireNonNull(plugin, "plugin"));
        this.progressStore.load();
    }

    public void register(DialogueTree tree) {
        trees.put(tree.id(), tree);
    }

    /** Opens a dialogue tree at its top level; top-level dialogs expose only the native close action. */
    public void open(Player player, String treeId) {
        DialogueTree tree = trees.get(treeId);
        if (tree == null) {
            dialogueEngine.openUnavailable(player, "Dialog", "Dieser Dialog ist nicht verfügbar.");
            return;
        }
        navigation.remove(player.getUniqueId());
        openInternal(player, tree, tree.startNodeId(), false);
    }

    public void open(Player player, DialogueTree tree, String nodeId) {
        navigation.remove(player.getUniqueId());
        openInternal(player, tree, nodeId, false);
    }

    private void openInternal(Player player, DialogueTree tree, String nodeId, boolean pushCurrent) {
        DialogueNode node = tree.node(nodeId).orElse(null);
        if (node == null) {
            dialogueEngine.openUnavailable(player, "Dialog", "Dieser Dialogschritt ist nicht verfügbar.");
            return;
        }

        if (pushCurrent) {
            navigation.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>()).push(node.id());
        }

        String stateKey = stateKey(tree, node);
        if (node.once() && progressStore.hasSeen(player.getUniqueId(), stateKey)) {
            dialogueEngine.openUnavailable(player, "Dialog", "Diesen Dialog hast du bereits gesehen.");
            return;
        }

        progressStore.markSeen(player.getUniqueId(), stateKey);
        var actions = new ArrayList<ActionButton>();
        for (DialogueOption option : node.options()) {
            if (!option.condition().test(player)) continue;
            actions.add(dialogueEngine.actionButton(option.label(), target -> select(target, tree, node, option)));
        }

        if (actions.isEmpty()) {
            dialogueEngine.openNotice(player, node.title(), Component.text("Dialog beendet."), Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }

        if (node.id().equals(tree.startNodeId())) {
            dialogueEngine.openMultiAction(player, node.title(), node.body(), actions, Math.min(2, Math.max(1, actions.size())));
        } else {
            actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                    target -> goBack(target, tree)));
            dialogueEngine.openMultiAction(player, node.title(), node.body(), actions, Math.min(2, Math.max(1, actions.size())));
        }
    }

    private void select(Player player, DialogueTree tree, DialogueNode node, DialogueOption option) {
        if (option.completesCurrentNode()) progressStore.markCompleted(player.getUniqueId(), stateKey(tree, node));
        option.action().execute(player);
        if (option.nextNodeId() != null && !option.nextNodeId().isBlank()) {
            navigation.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>()).push(node.id());
            openInternal(player, tree, option.nextNodeId(), false);
        } else {
            player.closeDialog();
        }
    }

    private void goBack(Player player, DialogueTree tree) {
        Deque<String> stack = navigation.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            navigation.remove(player.getUniqueId());
            openInternal(player, tree, tree.startNodeId(), false);
            return;
        }
        String previous = stack.pop();
        if (stack.isEmpty()) navigation.remove(player.getUniqueId());
        openInternal(player, tree, previous, false);
    }

    public void shutdown() {
        navigation.clear();
        progressStore.shutdown();
    }

    private String stateKey(DialogueTree tree, DialogueNode node) {
        return tree.id() + ":" + node.id();
    }
}
