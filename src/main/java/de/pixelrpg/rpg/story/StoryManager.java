package de.pixelrpg.rpg.story;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.configuration.ConfigurationSection;
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

/** Persistent, level-gated story campaign and validated lore registry. */
public final class StoryManager {
    private static final int MAX_CHAPTERS = 32;
    private static final int STORY_VERSION = 3;

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

        if (yaml.getInt("story-version", 1) < STORY_VERSION && isPreviousDefault(yaml.getMapList("chapters"))) {
            createDefaultStory();
            yaml = YamlConfiguration.loadConfiguration(file);
        }

        ConfigurationSection root = yaml.getConfigurationSection("chapters");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                if (chapters.size() >= MAX_CHAPTERS) break;
                ConfigurationSection section = root.getConfigurationSection(key);
                StoryChapter chapter = section == null ? null : parseChapter(section);
                if (chapter != null) chapters.add(chapter);
            }
        } else {
            for (Map<?, ?> map : yaml.getMapList("chapters")) {
                if (chapters.size() >= MAX_CHAPTERS) break;
                StoryChapter chapter = parseLegacyChapter(map);
                if (chapter != null) chapters.add(chapter);
            }
        }

        chapters.sort(Comparator.comparingInt(StoryChapter::order));
    }

    private StoryChapter parseChapter(ConfigurationSection section) {
        int order = section.getInt("order", -1);
        String id = clean(section.getString("id"));
        String title = clean(section.getString("title"));
        if (order < 0 || id == null || title == null) return null;

        int requiredLevel = Math.clamp(section.getInt("required-level", 1), 1, 99);
        long expReward = Math.max(0L, section.getLong("exp-reward", 0L));
        String structure = clean(section.getString("structure-trigger"));
        String npcId = clean(section.getString("npc-id"));
        List<String> quests = section.getStringList("quests").stream()
                .map(String::trim).filter(value -> !value.isBlank()).toList();
        List<String> dialogue = section.getStringList("dialogue").stream()
                .map(String::trim).filter(value -> !value.isBlank() && value.length() <= 1000).toList();

        if (structure != null && npcId == null) {
            plugin.getLogger().warning("Ignoring story chapter '" + id + "': structure-trigger requires npc-id.");
            return null;
        }
        return new StoryChapter(order, id, title, dialogue, expReward, requiredLevel,
                structure == null ? "" : structure, npcId == null ? "" : npcId, quests);
    }

    private StoryChapter parseLegacyChapter(Map<?, ?> map) {
        int order = map.get("order") instanceof Number number ? number.intValue() : -1;
        String id = clean(map.get("id"));
        String title = clean(map.get("title"));
        if (order < 0 || id == null || title == null) return null;
        long exp = map.get("exp-reward") instanceof Number number ? Math.max(0L, number.longValue()) : 0L;
        List<String> dialogue = map.get("dialogue") instanceof List<?> values
                ? values.stream().filter(value -> value != null).map(String::valueOf).map(String::trim)
                .filter(value -> !value.isBlank() && value.length() <= 1000).toList()
                : List.of();
        return new StoryChapter(order, id, title, dialogue, exp, 1, "", "", List.of());
    }

    private void createDefaultStory() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("story-version", STORY_VERSION);

        chapter(yaml, 0, "first_traces", "Die ersten Spuren", 1, "", "", List.of(),
                List.of("Die Oberfläche ist voller Ruinen, Dörfer und verlassener Wege.",
                        "Die Archive kennen keine vollständige Chronik der Welt.",
                        "Beginne mit Beobachtungen, nicht mit Gewissheiten."), 100L);

        chapter(yaml, 1, "under_the_stone", "Unter dem Stein", 11, "minecraft:ancient_city", "eryn",
                List.of("story_under_stone_expedition", "story_under_stone_echo_fragments"),
                List.of("Tief unter der Welt liegen Ancient Cities im Deep Dark.",
                        "Sculk, Echo Shards und der Warden gehören zu den sicher beobachtbaren Spuren.",
                        "Warum die Städte verlassen wurden, bleibt eine offene Frage."), 300L);

        chapter(yaml, 2, "forgotten_builders", "Die vergessenen Erbauer", 21, "minecraft:ancient_city", "the_archivist",
                List.of("story_forgotten_builders"),
                List.of("Die Architektur der Ancient Cities deutet auf eine hochentwickelte Kultur.",
                        "Welche Verbindung zwischen ihren Bauten und anderen Ruinen besteht, ist nicht bewiesen."), 450L);

        chapter(yaml, 3, "black_flame", "Die schwarze Flamme", 31, "minecraft:bastion_remnant", "vael",
                List.of("story_black_flame"),
                List.of("Bastions zeigen die organisierte Gesellschaft der Piglins.",
                        "Der Wither wird durch eine konkrete Konstruktion erschaffen.",
                        "Die Motive hinter seiner Erschaffung sind nicht vollständig überliefert."), 650L);

        chapter(yaml, 4, "end_of_the_old", "Das Ende der Alten", 41, "minecraft:ruined_portal", "mara",
                List.of("story_end_of_the_old"),
                List.of("Ruined Portals sind sichtbare Spuren einer Verbindung zwischen Dimensionen.",
                        "Die Welt bewahrt mehr Übergänge, als ihre Bewohner verstehen."), 800L);

        chapter(yaml, 5, "beyond_the_world", "Jenseits der Welt", 51, "minecraft:stronghold", "oren",
                List.of("story_beyond_the_world"),
                List.of("Strongholds sind unterirdische Ruinen mit Endportalen.",
                        "Eyes of Ender weisen den Weg zu diesen Anlagen."), 1000L);

        chapter(yaml, 6, "realm_of_endermen", "Das Reich der Endermen", 61, "minecraft:end_city", "silex",
                List.of("story_realm_of_endermen"),
                List.of("Im End stehen Endermen, Chorus-Pflanzen und End Cities nebeneinander.",
                        "Wer die Herkunft dieser Kultur erklären will, muss zwischen Fund und Theorie unterscheiden."), 1200L);

        chapter(yaml, 7, "piglin_civilization", "Die Piglin-Zivilisation", 71, "minecraft:bastion_remnant", "kael",
                List.of("story_piglin_civilization"),
                List.of("Gold ist für Piglins nicht nur Beute, sondern Teil ihrer Kultur.",
                        "Bastions bewahren Spuren von Handel, Vorräten und Macht."), 1400L);

        chapter(yaml, 8, "the_dragon", "Der Drache", 81, "minecraft:end_city", "lyra",
                List.of("story_the_dragon"),
                List.of("Der Enderdrache bewacht das Zentrum des Endes.",
                        "End Cities und Endschiffe liegen jenseits der zentralen Inseln."), 1800L);

        chapter(yaml, 9, "after_the_end", "Was hinter dem Ende bleibt", 91, "", "the_archivist",
                List.of("story_after_the_end"),
                List.of("Sculk, Nether, Strongholds und das End bilden ein Muster.",
                        "Das Muster ist real; die vollständige Erklärung ist es noch nicht."), 2500L);

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create default story.yml", exception);
        }
    }

    private void chapter(YamlConfiguration yaml, int order, String id, String title, int level,
                         String structure, String npcId, List<String> quests, List<String> dialogue, long exp) {
        String path = "chapters." + id;
        yaml.set(path + ".order", order);
        yaml.set(path + ".id", id);
        yaml.set(path + ".title", title);
        yaml.set(path + ".required-level", level);
        yaml.set(path + ".structure-trigger", structure);
        yaml.set(path + ".npc-id", npcId);
        yaml.set(path + ".quests", quests);
        yaml.set(path + ".dialogue", dialogue);
        yaml.set(path + ".exp-reward", exp);
    }

    private boolean isPreviousDefault(List<Map<?, ?>> maps) {
        if (maps.size() != 5) return false;
        List<String> ids = maps.stream().map(map -> String.valueOf(map.get("id"))).toList();
        return ids.equals(List.of("echoes_beneath", "city_without_sky", "black_flame", "door_beyond_stars", "after_the_end"));
    }

    private String clean(Object value) {
        if (value == null) return null;
        String result = String.valueOf(value).trim();
        return result.isBlank() ? null : result;
    }

    public Optional<StoryChapter> getNextChapterFor(UUID uuid) {
        if (uuid == null) return Optional.empty();
        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        if (profile == null) return Optional.empty();
        int nextOrder = profile.getStoryChapterIndex() + 1;
        return chapters.stream()
                .filter(chapter -> chapter.order() == nextOrder && profile.getLevel() >= chapter.requiredLevel())
                .findFirst();
    }

    public Optional<StoryChapter> getChapter(int order) {
        return chapters.stream().filter(chapter -> chapter.order() == order).findFirst();
    }

    public boolean isNextChapter(UUID uuid, StoryChapter chapter) {
        if (uuid == null || chapter == null) return false;
        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        return profile != null && profile.getStoryChapterIndex() + 1 == chapter.order()
                && profile.getLevel() >= chapter.requiredLevel();
    }

    public boolean completeChapter(Player player, StoryChapter chapter) {
        if (player == null || chapter == null) return false;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || profile.getStoryChapterIndex() + 1 != chapter.order()
                || profile.getLevel() < chapter.requiredLevel()) return false;
        String completionQuest = chapter.completionQuestId();
        if (!completionQuest.isBlank() && !profile.hasCompletedQuest(completionQuest)) return false;

        profile.setStoryChapterIndex(chapter.order());
        if (chapter.expReward() > 0L) profileManager.addExperience(player.getUniqueId(), chapter.expReward());
        return true;
    }

    public List<StoryChapter> getAllChapters() {
        return List.copyOf(chapters);
    }
}
