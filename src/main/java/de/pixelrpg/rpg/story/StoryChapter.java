package de.pixelrpg.rpg.story;

import java.util.List;

public record StoryChapter(
        int order,
        String id,
        String title,
        List<String> dialogueLines,
        long expReward,
        int requiredLevel,
        String structureTrigger,
        String npcId,
        List<String> questIds
) {
    public StoryChapter {
        dialogueLines = dialogueLines == null ? List.of() : List.copyOf(dialogueLines);
        questIds = questIds == null ? List.of() : List.copyOf(questIds);
        structureTrigger = structureTrigger == null ? "" : structureTrigger.trim().toLowerCase();
        npcId = npcId == null ? "" : npcId.trim();
    }

    public boolean hasStructureTrigger() {
        return !structureTrigger.isBlank() && !npcId.isBlank();
    }

    public String startQuestId() {
        return questIds.isEmpty() ? "" : questIds.getFirst();
    }

    public String completionQuestId() {
        return questIds.isEmpty() ? "" : questIds.getLast();
    }
}
