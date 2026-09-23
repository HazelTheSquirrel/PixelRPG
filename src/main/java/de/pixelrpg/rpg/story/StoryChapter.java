package de.pixelrpg.rpg.story;

import java.util.List;
import java.util.Objects;

public record StoryChapter(
        int order,
        String id,
        String title,
        List<String> dialogueLines,
        long expReward
) {
    public StoryChapter {
        if (order < 0) throw new IllegalArgumentException("order must be non-negative");
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        dialogueLines = List.copyOf(Objects.requireNonNull(dialogueLines, "dialogueLines"));
        if (dialogueLines.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("dialogueLines must not contain null values");
        }
        if (expReward < 0L) throw new IllegalArgumentException("expReward must be non-negative");
    }
}
