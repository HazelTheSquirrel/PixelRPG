package de.pixelrpg.rpg.dialogue;

import java.util.Objects;

public final class DialogueActions {
    private DialogueActions() {
    }

    public static DialogueOption.DialogueAction none() {
        return DialogueOption.DialogueAction.none();
    }

    public static DialogueOption.DialogueAction learn(PlayerKnowledgeStore store, String entryId) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(entryId, "entryId");
        return new DialogueOption.DialogueAction() {
            @Override
            public void execute(org.bukkit.entity.Player player) {
                store.learn(player.getUniqueId(), entryId);
            }

            @Override
            public void execute(DialogueContext context) {
                store.learn(context.player().getUniqueId(), entryId);
            }
        };
    }

    public static DialogueOption.DialogueAction setWorldFlag(WorldState state, String flag) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(flag, "flag");
        return new DialogueOption.DialogueAction() {
            @Override
            public void execute(org.bukkit.entity.Player player) {
                state.set(flag);
            }

            @Override
            public void execute(DialogueContext context) {
                state.set(flag);
            }
        };
    }

    public static DialogueOption.DialogueAction clearWorldFlag(WorldState state, String flag) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(flag, "flag");
        return new DialogueOption.DialogueAction() {
            @Override
            public void execute(org.bukkit.entity.Player player) {
                state.clear(flag);
            }

            @Override
            public void execute(DialogueContext context) {
                state.clear(flag);
            }
        };
    }
}
