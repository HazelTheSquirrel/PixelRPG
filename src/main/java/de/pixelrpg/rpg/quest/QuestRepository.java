package de.pixelrpg.rpg.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestRepository {
    private final Plugin plugin;
    private final File questFolder;
    private final Map<String, Quest> questsById = new ConcurrentHashMap<>();

    private int maxActiveQuests = 5;
    private int unlockEarlyLevels = 5;

    public QuestRepository(Plugin plugin) {
        this.plugin = plugin;
        this.questFolder = new File(plugin.getDataFolder(), "quest");
    }

    public void load() {
        questsById.clear();
        maxActiveQuests = 5;
        unlockEarlyLevels = 5;
        try {
            loadJsonDefinitions();
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load quests.json; continuing with legacy YAML quests: " + exception.getMessage());
        }
        loadLegacyYaml();
    }

    /** Loads all user-editable quest rules and definitions from data/quests.json. */
    private void loadJsonDefinitions() {
        JsonObject root = new JsonDataManager(plugin).load("quests.json");
        JsonObject rules = object(root, "rules");
        if (rules != null) {
            maxActiveQuests = Math.max(1, number(rules, "maxActive", maxActiveQuests));
            unlockEarlyLevels = Math.max(0, number(rules, "unlockEarlyLevels", unlockEarlyLevels));
        }

        JsonArray definitions = root.getAsJsonArray("definitions");
        if (definitions == null) return;
        for (var element : definitions) {
            if (!element.isJsonObject()) continue;
            JsonObject json = element.getAsJsonObject();
            String id = string(json, "id", "").strip();
            if (id.isEmpty()) {
                plugin.getLogger().warning("Ignoring quest definition without an id.");
                continue;
            }
            QuestType type = parseType(string(json, "type", "HUNT"));
            int recommendedLevel = clampLevel(number(json, "recommendedLevel", number(json, "requiredLevel", 1)));
            int categoryLevel = clampLevel(number(json, "categoryLevel", Math.max(1, recommendedLevel - 5)));
            int requiredAmount = Math.max(1, number(json, "requiredAmount", 1));
            JsonObject reward = object(json, "reward");
            Quest quest = new Quest(id, string(json, "title", id), string(json, "description", ""), type,
                    string(json, "targetKey", ""), requiredAmount, recommendedLevel, categoryLevel,
                    numberDouble(reward, "money", 0.0), numberLong(reward, "experience", 0L), number(reward, "durationMinutes", 0),
                    stringList(reward, "items"), readJsonLocation(json, "escortDestination"), readJsonLocation(json, "reachLocation"), numberDouble(json, "reachRadius", 5.0));
            questsById.put(id, quest);
        }
    }

    private void loadLegacyYaml() {
        if (!questFolder.exists()) {
            questFolder.mkdirs();
            createDefaultQuests();
        }
        File[] files = questFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;
        for (File file : files) loadFile(file, parseCategoryLevel(file.getName()));
    }

    private void loadFile(File file, int fileCategoryLevel) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("quests");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            QuestType type = parseType(section.getString("type", "HUNT"));
            int requiredLevel = readLevelRequirement(section, fileCategoryLevel);
            int categoryLevel = clampLevel(section.getInt("category-level", fileCategoryLevel));
            Quest quest = new Quest(id, section.getString("title", id), section.getString("description", ""), type, section.getString("target-key", ""), section.getInt("required-amount", 1), requiredLevel, categoryLevel, section.getDouble("reward-money", 0.0), section.getLong("reward-exp", 0L), section.getInt("duration-minutes", 0), section.getStringList("reward-items"), readLocation(section, "escort-destination"), readLocation(section, "reach-location"), section.getDouble("reach-radius", 5.0));
            questsById.put(id, quest);
        }
    }

    private int readLevelRequirement(ConfigurationSection section, int fallback) {
        if (section.contains("required-level")) return clampLevel(section.getInt("required-level", fallback));
        String oldRank = section.getString("required-rank");
        return oldRank == null ? clampLevel(fallback) : oldRankToLevel(oldRank);
    }

    private Location readLocation(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) return null;
        World world = Bukkit.getWorld(section.getString("world", ""));
        if (world == null) return null;
        return new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
    }

    private Location readJsonLocation(JsonObject parent, String key) {
        JsonObject location = object(parent, key);
        if (location == null) return null;
        World world = Bukkit.getWorld(string(location, "world", ""));
        if (world == null) return null;
        return new Location(world, numberDouble(location, "x", 0.0), numberDouble(location, "y", 0.0), numberDouble(location, "z", 0.0));
    }

    private static JsonObject object(JsonObject parent, String key) { return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null; }
    private static String string(JsonObject object, String key, String fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback; }
    private static int number(JsonObject object, String key, int fallback) { return object != null && object.has(key) && object.get(key).isNumber() ? object.get(key).getAsInt() : fallback; }
    private static long numberLong(JsonObject object, String key, long fallback) { return object != null && object.has(key) && object.get(key).isNumber() ? object.get(key).getAsLong() : fallback; }
    private static double numberDouble(JsonObject object, String key, double fallback) { return object != null && object.has(key) && object.get(key).isNumber() ? object.get(key).getAsDouble() : fallback; }
    private static List<String> stringList(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (var element : object.getAsJsonArray(key)) if (element.isJsonPrimitive()) result.add(element.getAsString());
        return List.copyOf(result);
    }

    private void createDefaultQuests() {
        saveDefault(1, Map.of("hunt_zombies_example", questMap("Zombie-Jagd", "Dünne die Zombiehorde aus, die die Außenbezirke bedroht.", "HUNT", "ZOMBIE", 10, 50.0, 150L, 0, List.of(), 1), "collect_cobblestone_example", questMap("Steinlieferung", "Sammle Bruchstein für die Befestigungsanlagen.", "COLLECT", "COBBLESTONE", 32, 30.0, 100L, 0, List.of(), 1), "reach_ruins_example", questMapReach("Erkunde die Ruinen", "Reise zu den alten Ruinen und melde dich zurück.", 1, 20.0, 80L, 8.0, 1)));
        saveDefault(11, Map.of("hunt_skeletons_example", questMap("Skelett-Säuberung", "Die Knochenklapperer werden dreister. Dünne ihre Reihen aus.", "HUNT", "SKELETON", 15, 65.0, 180L, 0, List.of(), 11), "escort_merchant_example", questMap("Eskortiere den Händler", "Begleite den Händler sicher zum Kontrollpunkt.", "ESCORT", "merchant_npc", 1, 80.0, 200L, 15, List.of(), 11)));
        saveDefault(21, Map.of("collect_blaze_rods_example", questMap("Lohenrutenlieferung", "Der Schmied benötigt Lohenruten als Brennstoff für die Verzauberung.", "COLLECT", "BLAZE_ROD", 8, 90.0, 220L, 0, List.of(), 21)));
        saveDefault(31, Map.of("event_wither_example", questMap("Kalamität: Wither-Ausbruch", "Der Server muss den Wither besiegen, bevor er die Stadt zerstört.", "GLOBAL_EVENT", "WITHER", 1, 300.0, 800L, 0, List.of(), 31)));
        saveDefault(41, Map.of("hunt_witches_example", questMap("Hexenzirkel des verseuchten Hains", "Hexen haben den Hain verdorben. Vertreibe sie.", "HUNT", "WITCH", 12, 140.0, 420L, 0, List.of(), 41), "collect_ender_pearls_example", questMap("Bitte des Enderwanderers", "Sammle Enderperlen für ein Ritual der Rückkehr.", "COLLECT", "ENDER_PEARL", 16, 160.0, 450L, 0, List.of(), 41)));
        saveDefault(51, Map.of("escort_envoy_example", questMap("Der stille Gesandte", "Eskortiere den Gesandten unbemerkt durch feindliches Gebiet.", "ESCORT", "envoy_npc", 1, 260.0, 900L, 20, List.of(), 51), "reach_summit_example", questMapReach("Der eisige Gipfel", "Erklimme den Gipfelmarker und hisse das Banner.", 1, 200.0, 700L, 6.0, 51)));
        saveDefault(61, Map.of("event_dragon_example", questMap("Kalamität: Der uralte Wyrm", "Eine legendäre Bedrohung erwacht. Der gesamte Server muss sich ihr stellen.", "GLOBAL_EVENT", "ENDER_DRAGON", 1, 1000.0, 3000L, 0, List.of(), 61)));
    }

    private Map<String, Object> questMap(String title, String description, String type, String targetKey, int requiredAmount, double rewardMoney, long rewardExp, int durationMinutes, List<String> rewardItems, int level) {
        Map<String, Object> map = new java.util.HashMap<>(); map.put("title", title); map.put("description", description); map.put("type", type); map.put("target-key", targetKey); map.put("required-amount", requiredAmount); map.put("required-level", level); map.put("category-level", level); map.put("reward-money", rewardMoney); map.put("reward-exp", rewardExp); map.put("duration-minutes", durationMinutes); map.put("reward-items", rewardItems); return map;
    }
    private Map<String, Object> questMapReach(String title, String description, int requiredAmount, double rewardMoney, long rewardExp, double reachRadius, int level) { Map<String, Object> map = questMap(title, description, "REACH_LOCATION", "", requiredAmount, rewardMoney, rewardExp, 0, List.of(), level); map.put("reach-radius", reachRadius); return map; }
    private void saveDefault(int level, Map<String, Map<String, Object>> quests) { File file = new File(questFolder, "level-" + level + ".yml"); YamlConfiguration yaml = new YamlConfiguration(); for (var entry : quests.entrySet()) for (var field : entry.getValue().entrySet()) yaml.set("quests." + entry.getKey() + "." + field.getKey(), field.getValue()); try { yaml.save(file); } catch (IOException e) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create default quest file " + file.getName(), e); } }

    public Quest getQuest(String id) { return questsById.get(id); }
    public List<Quest> getAllQuests() { return new ArrayList<>(questsById.values()); }
    public List<Quest> getQuestsByCategory(int playerLevel) { int category = categoryForLevel(playerLevel); return questsById.values().stream().filter(q -> q.categoryLevel() == category).filter(q -> q.type() != QuestType.GLOBAL_EVENT).toList(); }
    public List<Quest> getQuestsByCategoryLevel(int categoryLevel) { int category = clampLevel(categoryLevel); return questsById.values().stream().filter(q -> q.categoryLevel() == category).filter(q -> q.type() != QuestType.GLOBAL_EVENT).toList(); }
    public int categoryForLevel(int playerLevel) { int level = clampLevel(playerLevel); int best = 1; for (Quest quest : questsById.values()) if (quest.categoryLevel() <= level && quest.categoryLevel() > best) best = quest.categoryLevel(); return best; }
    public List<Quest> getQuestsByType(QuestType type) { return questsById.values().stream().filter(q -> q.type() == type).toList(); }
    public int maxActiveQuests() { return maxActiveQuests; }
    public int unlockEarlyLevels() { return unlockEarlyLevels; }
    private int parseCategoryLevel(String fileName) { String base = fileName.substring(0, fileName.length() - 4); if (base.startsWith("level-")) { try { return clampLevel(Integer.parseInt(base.substring(6))); } catch (NumberFormatException ignored) { return Level.MIN_LEVEL; } } return oldRankToLevel(base); }
    private int oldRankToLevel(String raw) { return switch (raw.trim().toUpperCase()) { case "F" -> 1; case "E" -> 11; case "D" -> 21; case "C" -> 31; case "B" -> 41; case "A" -> 51; case "S" -> 61; default -> Level.MIN_LEVEL; }; }
    private int clampLevel(int level) { return Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, level)); }
    private QuestType parseType(String raw) { try { return QuestType.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return QuestType.HUNT; } }
}
