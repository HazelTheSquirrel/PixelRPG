package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.lore.LoreRegistry;

import java.util.Objects;

public final class DialogueActions {
    private DialogueActions() { }

    public static DialogueOption.DialogueAction none() {
        return DialogueOption.DialogueAction.none();
    }

    public static DialogueOption.DialogueAction learn(PlayerKnowledgeStore store, String entryId) {
        Objects.requireNonNull(store, "store");
        return new DialogueOption.DialogueAction() {
            @Override public void execute(org.bukkit.entity.Player player) {
                store.learn(player.getUniqueId(), entryId);
            }
            @Override public void execute(DialogueContext context) {
                store.learn(context.player().getUniqueId(), entryId);
            }
        };
    }

    public static DialogueOption.DialogueAction learnNpc(NpcKnowledgeStore store, String entryId) {
        Objects.requireNonNull(store, "store");
        return new DialogueOption.DialogueAction() {
            @Override public void execute(org.bukkit.entity.Player player) { }
            @Override public void execute(DialogueContext context) {
                context.npcOptional().ifPresent(npc -> store.learn(npc.id(), entryId));
            }
        };
    }

    public static DialogueOption.DialogueAction setWorldFlag(WorldState state, String flag) {
        Objects.requireNonNull(state, "state");
        return new DialogueOption.DialogueAction() {
            @Override public void execute(org.bukkit.entity.Player player) { state.set(flag); }
            @Override public void execute(DialogueContext context) { state.set(flag); }
        };
    }

    public static DialogueOption.DialogueAction clearWorldFlag(WorldState state, String flag) {
        Objects.requireNonNull(state, "state");
        return new DialogueOption.DialogueAction() {
            @Override public void execute(org.bukkit.entity.Player player) { state.clear(flag); }
            @Override public void execute(DialogueContext context) { state.clear(flag); }
        };
    }

    public static DialogueOption.DialogueAction meetNpc(NpcRelationshipStore store) {
        Objects.requireNonNull(store, "store");
        return new DialogueOption.DialogueAction() {
            @Override public void execute(org.bukkit.entity.Player player) { }
            @Override public void execute(DialogueContext context) {
                context.npcOptional().ifPresent(npc -> store.rememberMeeting(context.player().getUniqueId(), npc.id()));
            }
        };
    }

    public static DialogueOption.DialogueAction adjustRelationship(NpcRelationshipStore store, String relation, int amount) {
        Objects.requireNonNull(store, "store");
        return new DialogueOption.DialogueAction() {
            @Override public void execute(org.bukkit.entity.Player player) { }
            @Override public void execute(DialogueContext context) {
                context.npcOptional().ifPresent(npc ->
                        store.adjust(context.player().getUniqueId(), npc.id(), relation, amount));
            }
        };
    }

    public static DialogueOption.DialogueAction adjustNpcRelationship(NpcNetworkRelationshipStore store, String otherNpcId, String relation, int amount) {
        Objects.requireNonNull(store, "store");
        return new DialogueOption.DialogueAction() {
            @Override public void execute(org.bukkit.entity.Player player) { }
            @Override public void execute(DialogueContext context) {
                context.npcOptional().ifPresent(npc ->
                        store.adjust(npc.id(), otherNpcId, relation, amount));
            }
        };
    }

    public static DialogueOption.DialogueAction adjustFactionRelationship(FactionRelationshipStore store,
                                                                            de.pixelrpg.rpg.npc.NpcFaction first,
                                                                            de.pixelrpg.rpg.npc.NpcFaction second,
                                                                            int amount) {
        Objects.requireNonNull(store, "store");
        return player -> store.adjust(first, second, amount);
    }

    public static DialogueOption.DialogueAction discoverLore(LoreRegistry loreRegistry, PlayerKnowledgeStore knowledge, String loreId) {
        Objects.requireNonNull(loreRegistry, "loreRegistry");
        Objects.requireNonNull(knowledge, "knowledge");
        return new DialogueOption.DialogueAction() {
            @Override public void execute(org.bukkit.entity.Player player) {
                loreRegistry.discover(knowledge, player.getUniqueId(), loreId);
            }
            @Override public void execute(DialogueContext context) {
                loreRegistry.discover(knowledge, context.player().getUniqueId(), loreId);
            }
        };
    }
}
