package de.pixelrpg.rpg.story;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** Owns story content and persistent chapter progression. */
public final class StoryManager {
    private final JavaPlugin plugin;
    private final PlayerProfileManager profiles;
    private final File file;
    private final List<StoryChapter> chapters = new ArrayList<>();

    public StoryManager(JavaPlugin plugin, PlayerProfileManager profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.file = new File(plugin.getDataFolder(), "story.yml");
    }

    public void load() {
        chapters.clear();
        if (!file.exists()) createDefaultStory();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> map : yaml.getMapList("chapters")) {
            int order = number(map.get("order"));
            String id = String.valueOf(map.get("id") == null ? "" : map.get("id"));
            String title = String.valueOf(map.get("title") == null ? "" : map.get("title"));
            long exp = map.get("exp-reward") instanceof Number n ? n.longValue() : 0L;
            List<String> lines = new ArrayList<>();
            Object raw = map.get("dialogue");
            if (raw instanceof List<?> list) list.forEach(entry -> lines.add(String.valueOf(entry)));
            if (!id.isBlank() && !title.isBlank()) chapters.add(new StoryChapter(order, id, title, lines, exp));
        }
        chapters.sort(Comparator.comparingInt(StoryChapter::order));
    }

    private void createDefaultStory() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("chapters", List.of(Map.of(
                "order", 0,
                "id", "prologue",
                "title", "The Summoning",
                "dialogue", List.of(
                        "You awaken in a world that is not your own.",
                        "The Guild recognizes potential in you.",
                        "Register at the Reception to begin your journey."
                ),
                "exp-reward", 100L
        )));
        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create default story.yml", exception);
        }
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    public Optional<StoryChapter> getNextChapterFor(UUID uuid) {
        PlayerProfile profile = profiles.getProfile(uuid).orElse(null);
        if (profile == null) return Optional.empty();
        int next = profile.getStoryChapterIndex() + 1;
        return chapters.stream().filter(chapter -> chapter.order() == next).findFirst();
    }

    public boolean completeChapter(Player player, StoryChapter chapter) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || profile.getStoryChapterIndex() + 1 != chapter.order()) return false;
        profile.setStoryChapterIndex(chapter.order());
        if (chapter.expReward() > 0) profiles.addExperience(player.getUniqueId(), chapter.expReward());
        return true;
    }

    public List<StoryChapter> getAllChapters() {
        return List.copyOf(chapters);
    }
}
