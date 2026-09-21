package de.pixelrpg.rpg.lore;

import de.pixelrpg.rpg.dialogue.KnowledgeType;

import java.util.Set;

public record LoreEntry(
        String id,
        String title,
        String category,
        String era,
        KnowledgeType sourceType,
        String summary,
        String text,
        Set<String> prerequisites
) {
    public LoreEntry {
        prerequisites = Set.copyOf(prerequisites == null ? Set.of() : prerequisites);
    }
}
