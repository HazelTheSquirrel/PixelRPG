package de.pixelrpg.rpg.story;

import java.util.List;

/** Immutable story chapter definition. */
public record StoryChapter(int order, String id, String title, List<String> dialogueLines, long expReward) {
    public StoryChapter {
        dialogueLines = dialogueLines == null ? List.of() : List.copyOf(dialogueLines);
    }
}
