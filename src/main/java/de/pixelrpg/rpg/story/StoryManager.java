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
    private static final int MAX_CHAPTERS = 64;
    private static final int STORY_VERSION = 5;

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
        if (!file.exists()) installBundledCampaign(false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        if (yaml.getInt("story-version", 1) < STORY_VERSION && isPreviousDefault(yaml)) {
            installBundledCampaign(true);
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

    private void installBundledCampaign(boolean replace) {
        try {
            plugin.saveResource("data/story_campaign.yml", replace);
            File bundled = new File(plugin.getDataFolder(), "data/story_campaign.yml");
            if (!bundled.exists()) throw new IOException("Bundled story_campaign.yml was not written.");
            java.nio.file.Files.copy(bundled.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to install story campaign.", exception);
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

    private boolean isPreviousDefault(YamlConfiguration yaml) {
        List<String> known = List.of("first_traces","under_the_stone","forgotten_builders","black_flame","end_of_the_old","beyond_the_world","realm_of_endermen","piglin_civilization","the_dragon","after_the_end");
        ConfigurationSection root = yaml.getConfigurationSection("chapters");
        if (root == null) return false;
        List<String> ids = root.getKeys(false).stream().toList();
        return ids.size() == known.size() && ids.containsAll(known);
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
