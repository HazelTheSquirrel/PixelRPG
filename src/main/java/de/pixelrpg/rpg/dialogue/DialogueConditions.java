package de.pixelrpg.rpg.dialogue;

import java.util.Objects;

public final class DialogueConditions {
    private DialogueConditions() {
    }

    public static DialogueCondition always() {
        return DialogueCondition.always();
    }

    public static DialogueCondition playerKnows(PlayerKnowledgeStore store, String entryId) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(entryId, "entryId");
        return new DialogueCondition() {
            @Override
            public boolean test(org.bukkit.entity.Player player) {
                return store.knows(player.getUniqueId(), entryId);
            }

            @Override
            public boolean test(DialogueContext context) {
                return store.knows(context.player().getUniqueId(), entryId);
            }
        };
    }

    public static DialogueCondition worldFlag(WorldState state, String flag) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(flag, "flag");
        return new DialogueCondition() {
            @Override
            public boolean test(org.bukkit.entity.Player player) {
                return state.isSet(flag);
            }

            @Override
            public boolean test(DialogueContext context) {
                return state.isSet(flag);
            }
        };
    }

    public static DialogueCondition npc(String npcId) {
        Objects.requireNonNull(npcId, "npcId");
        return new DialogueCondition() {
            @Override
            public boolean test(org.bukkit.entity.Player player) {
                return false;
            }

            @Override
            public boolean test(DialogueContext context) {
                return context.npcOptional().map(npc -> npc.id().equals(npcId)).orElse(false);
            }
        };
    }
}
