// src/main/java/de/pixelrpg/rpg/story/StoryManager.java
package de.pixelrpg.rpg.story;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class StoryManager {

    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    private final File file;
    private final List<StoryChapter> chapters = new ArrayList<>();

    public StoryManager(Plugin plugin, PlayerProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.file = new File(plugin.getDataFolder(), "story.yml");
    }

    public void load() {
        chapters.clear();

        if (!file.exists()) {
            createDefaultStory();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rawChapters = yaml.getMapList("chapters");

        for (Map<?, ?> map : rawChapters) {
            int order = toInt(map.get("order"));
            String id = String.valueOf(map.get("id"));
            String title = String.valueOf(map.get("title"));
            long expReward = map.get("exp-reward") instanceof Number number ? number.longValue() : 0L;

            List<String> dialogueLines = new ArrayList<>();
            Object dialogueRaw = map.get("dialogue");
            if (dialogueRaw instanceof List<?> rawList) {
                for (Object entry : rawList) {
                    dialogueLines.add(String.valueOf(entry));
                }
            }

            chapters.add(new StoryChapter(order, id, title, dialogueLines, expReward));
        }

        chapters.sort(Comparator.comparingInt(StoryChapter::order));
    }

    private void createDefaultStory() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> chapterMaps = new ArrayList<>();

        chapterMaps.add(Map.of(
                "order", 0,
                "id", "prologue",
                "title", "The Summoning",
                "dialogue", List.of(
                        "You awaken in a world that is not your own.",
                        "The Guild recognizes potential in you.",
                        "Register at the Reception to begin your journey."
                ),
                "exp-reward", 100L
        ));

        yaml.set("chapters", chapterMaps);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create default story.yml", e);
        }
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    public Optional<StoryChapter> getNextChapterFor(UUID uuid) {
        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        if (profile == null) {
            return Optional.empty();
        }
        int nextOrder = profile.getStoryChapterIndex() + 1;
        return chapters.stream().filter(chapter -> chapter.order() == nextOrder).findFirst();
    }

    public boolean completeChapter(Player player, StoryChapter chapter) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return false;
        }
        if (profile.getStoryChapterIndex() + 1 != chapter.order()) {
            return false;
        }

        profile.setStoryChapterIndex(chapter.order());
        if (chapter.expReward() > 0) {
            profileManager.addExperience(player.getUniqueId(), chapter.expReward());
        }
        return true;
    }

    public List<StoryChapter> getAllChapters() {
        return List.copyOf(chapters);
    }
}