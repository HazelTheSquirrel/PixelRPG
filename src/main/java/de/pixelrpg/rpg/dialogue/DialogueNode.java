package de.pixelrpg.rpg.dialogue;

import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;

public record DialogueNode(
        String id,
        Component title,
        List<DialogBody> body,
        List<DialogueOption> options,
        boolean once
) {
    public DialogueNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(options, "options");
        if (id.isBlank()) throw new IllegalArgumentException("Dialogue node id must not be blank");
    }
}
