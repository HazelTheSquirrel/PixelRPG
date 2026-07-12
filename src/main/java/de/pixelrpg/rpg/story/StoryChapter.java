// src/main/java/de/pixelrpg/rpg/story/StoryChapter.java
package de.pixelrpg.rpg.story;

import java.util.List;

public record StoryChapter(int order, String id, String title, List<String> dialogueLines, long expReward) {
}