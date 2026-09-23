package de.pixelrpg.rpg.dialogue;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Objects;

public record DialogueOption(
        Component label,
        DialogueCondition condition,
        String nextNodeId,
        DialogueAction action,
        boolean completesCurrentNode
) {
    public DialogueOption {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(action, "action");
        if (nextNodeId != null && nextNodeId.isBlank()) nextNodeId = null;
    }

    @FunctionalInterface
    public interface DialogueAction {
        void execute(Player player);

        static DialogueAction none() {
            return player -> { };
        }
    }
}
