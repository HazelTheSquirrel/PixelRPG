package de.pixelrpg.rpg.dialogue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DialogueTree {
    private final String id;
    private final String startNodeId;
    private final Map<String, DialogueNode> nodes;

    public DialogueTree(String id, String startNodeId, Iterable<DialogueNode> nodes) {
        this.id = Objects.requireNonNull(id, "id");
        this.startNodeId = Objects.requireNonNull(startNodeId, "startNodeId");
        if (id.isBlank() || startNodeId.isBlank()) {
            throw new IllegalArgumentException("Dialogue tree id and start node must not be blank");
        }

        this.nodes = new LinkedHashMap<>();
        for (DialogueNode node : nodes) {
            if (this.nodes.put(node.id(), node) != null) {
                throw new IllegalArgumentException("Duplicate dialogue node: " + node.id());
            }
        }
        if (!this.nodes.containsKey(startNodeId)) {
            throw new IllegalArgumentException("Missing dialogue start node: " + startNodeId);
        }
    }

    public String id() {
        return id;
    }

    public String startNodeId() {
        return startNodeId;
    }

    public Optional<DialogueNode> node(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }
}
