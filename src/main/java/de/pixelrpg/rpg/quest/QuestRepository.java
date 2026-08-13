// src/main/java/de/pixelrpg/rpg/quest/QuestRepository.java (VOLLSTÄNDIG, ersetzt alte Datei — Standard-Quests jetzt auf Deutsch)
package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.core.Rank;
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
import java.util.logging.Level;

public final class QuestRepository {

    private final Plugin plugin;
    private final File questFolder;
    private final Map<String, Quest> questsById = new ConcurrentHashMap<>();

    public QuestRepository(Plugin plugin) {
        this.plugin = plugin;
        this.questFolder = new File(plugin.getDataFolder(), "quest");
    }

    public void load() {
        questsById.clear();
        if (!questFolder.exists()) {
            questFolder.mkdirs();
            createDefaultQuests();
        }

        for (Rank rank : Rank.values()) {
            File file = new File(questFolder, rank.name() + ".yml");
            if (!file.exists()) {
                continue;
            }
            loadFile(file, rank);
        }
    }

    private void loadFile(File file, Rank fileRank) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("quests");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            QuestType type = parseType(section.getString("type", "HUNT"));
            Rank requiredRank = section.contains("required-rank")
                    ? parseRank(section.getString("required-rank"))
                    : fileRank;
            Location escortDestination = readLocation(section, "escort-destination");
            Location reachLocation = readLocation(section, "reach-location");

            Quest quest = new Quest(
                    id, section.getString("title", id), section.getString("description", ""), type,
                    section.getString("target-key", ""), section.getInt("required-amount", 1), requiredRank, fileRank,
                    section.getDouble("reward-money", 0.0), section.getLong("reward-exp", 0L),
                    section.getInt("duration-minutes", 0), section.getStringList("reward-items"),
                    escortDestination, reachLocation, section.getDouble("reach-radius", 5.0)
            );
            questsById.put(id, quest);
        }
    }

    private Location readLocation(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) {
            return null;
        }
        return new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
    }

    private void createDefaultQuests() {
        saveDefault(Rank.F, Map.of(
                "hunt_zombies_example", questMap("Zombie-Jagd", "Dünne die Zombiehorde aus, die die Außenbezirke bedroht.",
                        "HUNT", "ZOMBIE", 10, 50.0, 150L, 0, List.of("ARROW")),
                "collect_cobblestone_example", questMap("Steinlieferung", "Sammle Bruchstein für die Befestigungsanlagen.",
                        "COLLECT", "COBBLESTONE", 32, 30.0, 100L, 0, List.of()),
                "reach_ruins_example", questMapReach("Erkunde die Ruinen", "Reise zu den alten Ruinen und melde dich zurück.", 1, 20.0, 80L, 8.0)
        ));

        saveDefault(Rank.E, Map.of(
                "hunt_skeletons_example", questMap("Skelett-Säuberung", "Die Knochenklapperer werden dreister. Dünne ihre Reihen aus.",
                        "HUNT", "SKELETON", 15, 65.0, 180L, 0, List.of()),
                "escort_merchant_example", questMap("Eskortiere den Händler", "Begleite den Händler sicher zum Kontrollpunkt.",
                        "ESCORT", "merchant_npc", 1, 80.0, 200L, 15, List.of())
        ));

        saveDefault(Rank.C, Map.of(
                "collect_blaze_rods_example", questMap("Lohenrutenlieferung", "Der Schmied benötigt Lohenruten als Brennstoff für die Verzauberung.",
                        "COLLECT", "BLAZE_ROD", 8, 90.0, 220L, 0, List.of())
        ));

        saveDefault(Rank.D, Map.of(
                "event_wither_example", questMap("Kalamität: Wither-Ausbruch", "Der Server muss den Wither besiegen, bevor er die Stadt zerstört.",
                        "GLOBAL_EVENT", "WITHER", 1, 300.0, 800L, 0, List.of())
        ));

        saveDefault(Rank.S, Map.of(
                "event_dragon_example", questMap("Kalamität: Der uralte Wyrm", "Eine legendäre Bedrohung erwacht. Der gesamte Server muss sich ihr stellen.",
                        "GLOBAL_EVENT", "ENDER_DRAGON", 1, 1000.0, 3000L, 0, List.of())
        ));

        saveDefault(Rank.B, Map.of(
                "hunt_witches_example", questMap("Hexenzirkel des verseuchten Hains", "Hexen haben den Hain verdorben. Vertreibe sie.",
                        "HUNT", "WITCH", 12, 140.0, 420L, 0, List.of()),
                "collect_ender_pearls_example", questMap("Bitte des Enderwanderers", "Sammle Enderperlen für ein Ritual der Rückkehr.",
                        "COLLECT", "ENDER_PEARL", 16, 160.0, 450L, 0, List.of())
        ));

        saveDefault(Rank.A, Map.of(
                "escort_envoy_example", questMap("Der stille Gesandte", "Eskortiere den Gesandten unbemerkt durch feindliches Gebiet.",
                        "ESCORT", "envoy_npc", 1, 260.0, 900L, 20, List.of()),
                "reach_summit_example", questMapReach("Der eisige Gipfel", "Erklimme den Gipfelmarker und hisse das Banner.",
                        1, 200.0, 700L, 6.0)
        ));
    }

    private Map<String, Object> questMap(String title, String description, String type, String targetKey,
                                          int requiredAmount, double rewardMoney, long rewardExp,
                                          int durationMinutes, List<String> rewardItems) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("type", type);
        map.put("target-key", targetKey);
        map.put("required-amount", requiredAmount);
        map.put("reward-money", rewardMoney);
        map.put("reward-exp", rewardExp);
        map.put("duration-minutes", durationMinutes);
        map.put("reward-items", rewardItems);
        return map;
    }

    private Map<String, Object> questMapReach(String title, String description, int requiredAmount,
                                               double rewardMoney, long rewardExp, double reachRadius) {
        Map<String, Object> map = questMap(title, description, "REACH_LOCATION", "", requiredAmount, rewardMoney, rewardExp, 0, List.of());
        map.put("reach-radius", reachRadius);
        return map;
    }

    private void saveDefault(Rank rank, Map<String, Map<String, Object>> quests) {
        File file = new File(questFolder, rank.name() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        for (var entry : quests.entrySet()) {
            for (var field : entry.getValue().entrySet()) {
                yaml.set("quests." + entry.getKey() + "." + field.getKey(), field.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create default quest file " + file.getName(), e);
        }
    }

    public Quest getQuest(String id) {
        return questsById.get(id);
    }

    public List<Quest> getAllQuests() {
        return new ArrayList<>(questsById.values());
    }

    public List<Quest> getQuestsByCategory(Rank category) {
        return questsById.values().stream()
                .filter(q -> q.category() == category)
                .filter(q -> q.type() != QuestType.GLOBAL_EVENT)
                .toList();
    }

    public List<Quest> getQuestsByType(QuestType type) {
        return questsById.values().stream().filter(q -> q.type() == type).toList();
    }

    private QuestType parseType(String raw) {
        try {
            return QuestType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return QuestType.HUNT;
        }
    }

    private Rank parseRank(String raw) {
        try {
            return Rank.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Rank.F;
        }
    }
}