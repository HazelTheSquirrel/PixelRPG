package de.pixelrpg.rpg.dialogue;

import org.bukkit.Material;
import org.bukkit.World;

import java.util.Objects;

public final class DialogueConditions {
    private DialogueConditions() { }

    public static DialogueCondition always() {
        return DialogueCondition.always();
    }

    public static DialogueCondition playerKnows(PlayerKnowledgeStore store, String entryId) {
        Objects.requireNonNull(store, "store");
        return context -> store.knows(context.player().getUniqueId(), entryId);
    }

    public static DialogueCondition npcKnows(NpcKnowledgeStore store, String entryId) {
        Objects.requireNonNull(store, "store");
        return context -> context.npcOptional().map(npc -> store.knows(npc.id(), entryId)).orElse(false);
    }

    public static DialogueCondition hasMet(NpcRelationshipStore store) {
        Objects.requireNonNull(store, "store");
        return context -> context.npcOptional().map(npc ->
                store.hasMet(context.player().getUniqueId(), npc.id())).orElse(false);
    }

    public static DialogueCondition worldFlag(WorldState state, String flag) {
        Objects.requireNonNull(state, "state");
        return context -> state.isSet(flag);
    }

    public static DialogueCondition npc(String npcId) {
        return context -> context.npcOptional().map(npc -> npc.id().equals(npcId)).orElse(false);
    }

    public static DialogueCondition dimension(String dimension) {
        String normalized = Objects.requireNonNull(dimension, "dimension").trim().toLowerCase();
        return context -> switch (normalized) {
            case "overworld" -> context.world().getEnvironment() == World.Environment.NORMAL;
            case "nether" -> context.world().getEnvironment() == World.Environment.NETHER;
            case "end" -> context.world().getEnvironment() == World.Environment.THE_END;
            default -> false;
        };
    }

    public static DialogueCondition hasItem(String materialName) {
        Material material = Material.matchMaterial(Objects.requireNonNull(materialName, "materialName"));
        if (material == null || material.isAir()) throw new IllegalArgumentException("Unknown material: " + materialName);
        return context -> context.player().getInventory().contains(material);
    }
}
