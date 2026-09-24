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

/** Persistent story progression and validated lore chapter registry. */
public final class StoryManager {
    private static final int MIN_ORDER = 0;
    private static final int MAX_CHAPTERS = 32;

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
        if (!file.exists()) createDefaultStory();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rawChapters = yaml.getMapList("chapters");
        int storyVersion = yaml.getInt("story-version", 1);
        if (storyVersion < 2 && isLegacyDefaultStory(rawChapters)) {
            createDefaultStory();
            yaml = YamlConfiguration.loadConfiguration(file);
            rawChapters = yaml.getMapList("chapters");
        }
        int previousOrder = -1;

        for (Map<?, ?> map : rawChapters) {
            if (chapters.size() >= MAX_CHAPTERS) {
                plugin.getLogger().warning("Story file contains more than " + MAX_CHAPTERS + " chapters; remaining entries are ignored.");
                break;
            }

            int order = toInt(map.get("order"));
            String id = string(map.get("id"));
            String title = string(map.get("title"));
            long expReward = map.get("exp-reward") instanceof Number number ? Math.max(0L, number.longValue()) : 0L;

            if (order < MIN_ORDER || order <= previousOrder || id == null || id.isBlank() || title == null || title.isBlank()) {
                plugin.getLogger().warning("Ignoring invalid story chapter entry: order=" + order + ", id=" + id);
                continue;
            }

            List<String> dialogueLines = new ArrayList<>();
            Object dialogueRaw = map.get("dialogue");
            if (dialogueRaw instanceof List<?> rawList) {
                for (Object entry : rawList) {
                    if (entry == null) continue;
                    String line = String.valueOf(entry).trim();
                    if (!line.isEmpty() && line.length() <= 1000) dialogueLines.add(line);
                }
            }

            chapters.add(new StoryChapter(order, id, title, List.copyOf(dialogueLines), expReward));
            previousOrder = order;
        }

        chapters.sort(Comparator.comparingInt(StoryChapter::order));
    }

    private void createDefaultStory() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> chapterMaps = new ArrayList<>();

        chapterMaps.add(chapter(0, "echoes_beneath", "Die Stimmen unter Stein",
                List.of(
                        "Die Gilde führt dich an die Archive.",
                        "Alte Ruinen, Ancient Cities und Sculk sind reale Spuren einer vergessenen Welt.",
                        "Du lernst, zwischen Beobachtung und Theorie zu unterscheiden."
                ), 150L));
        chapterMaps.add(chapter(1, "city_without_sky", "Die Stadt ohne Himmel",
                List.of(
                        "Ancient Cities liegen tief unter der Erde.",
                        "Der Warden und Sculk bewachen keine einfache Schatzkammer.",
                        "Die Verbindung zu den verschwundenen Erbauern bleibt eine offene Spur."
                ), 250L));
        chapterMaps.add(chapter(2, "black_flame", "Die schwarze Flamme",
                List.of(
                        "Piglin-Bastions erzählen von einer organisierten Nether-Zivilisation.",
                        "Der Wither ist erschaffen, nicht zufällig geboren.",
                        "Die Archive warnen davor, Theorie und gesicherte Geschichte zu vermischen."
                ), 350L));
        chapterMaps.add(chapter(3, "door_beyond_stars", "Das Tor jenseits der Sterne",
                List.of(
                        "Strongholds führen zum Endportal.",
                        "Der Enderdrache bewacht das Zentrum des Endes.",
                        "End Cities und Endschiffe bewahren Spuren einer früheren Präsenz."
                ), 500L));
        chapterMaps.add(chapter(4, "after_the_end", "Was hinter dem Ende bleibt",
                List.of(
                        "Die Welt liefert Muster, aber keine vollständige Chronik.",
                        "Sculk, Nether, alte Ruinen und das Ende bleiben Gegenstand der Untersuchung.",
                        "Die nächste Generation soll bessere Fragen und sauberere Archive hinterlassen."
                ), 750L));

        yaml.set("story-version", 2);
        yaml.set("chapters", chapterMaps);
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create default story.yml", exception);
        }
    }

    private boolean isLegacyDefaultStory(List<Map<?, ?>> rawChapters) {
        if (rawChapters.size() != 1) return false;
        Object id = rawChapters.getFirst().get("id");
        return id != null && "prologue".equalsIgnoreCase(String.valueOf(id).trim());
    }

    private Map<String, Object> chapter(int order, String id, String title, List<String> dialogue, long expReward) {
        return Map.of(
                "order", order,
                "id", id,
                "title", title,
                "dialogue", dialogue,
                "exp-reward", expReward);
    }

    public Optional<StoryChapter> getNextChapterFor(UUID uuid) {
        if (uuid == null) return Optional.empty();
        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        if (profile == null) return Optional.empty();
        int nextOrder = profile.getStoryChapterIndex() + 1;
        return chapters.stream().filter(chapter -> chapter.order() == nextOrder).findFirst();
    }

    public Optional<StoryChapter> getChapter(int order) {
        return chapters.stream().filter(chapter -> chapter.order() == order).findFirst();
    }

    public boolean isNextChapter(UUID uuid, StoryChapter chapter) {
        if (uuid == null || chapter == null) return false;
        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        return profile != null && profile.getStoryChapterIndex() + 1 == chapter.order();
    }

    public boolean completeChapter(Player player, StoryChapter chapter) {
        if (player == null || chapter == null) return false;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || profile.getStoryChapterIndex() + 1 != chapter.order()) return false;

        profile.setStoryChapterIndex(chapter.order());
        if (chapter.expReward() > 0L) profileManager.addExperience(player.getUniqueId(), chapter.expReward());
        return true;
    }

    public List<StoryChapter> getAllChapters() {
        return List.copyOf(chapters);
    }

    private String string(Object value) {
        if (value == null) return null;
        String result = String.valueOf(value).trim();
        return result.isEmpty() ? null : result;
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
    }
}
