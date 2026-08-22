package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BossRepository {
    private final Plugin plugin;
    private final File file;
    private final Map<String, BossDefinition> definitionsById = new ConcurrentHashMap<>();

    public BossRepository(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bosses.yml");
    }

    public void load() {
        definitionsById.clear();
        if (!file.exists()) createDefaultBosses();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("bosses");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            BossDefinition definition = new BossDefinition(id, section.getString("name", id));
            definition.setBaseEntityType(parseEntityType(section.getString("base-entity", "ZOMBIE")));
            definition.setLevel(section.getInt("level", 30));
            definition.setHealthMultiplier(section.getDouble("health-multiplier", 4.0));
            definition.setDamageMultiplier(section.getDouble("damage-multiplier", 1.5));
            definition.setScaleMultiplier(section.getDouble("scale-multiplier", 1.35));

            List<BossPhase> phases = new ArrayList<>();
            for (Map<?, ?> phaseMap : section.getMapList("phases")) {
                double threshold = toDouble(phaseMap.get("health-percent"));
                int interval = toInt(phaseMap.get("attack-interval-ticks"));
                Object announceRaw = phaseMap.get("announcement");
                String announce = announceRaw != null ? String.valueOf(announceRaw) : "";
                List<String> patterns = new ArrayList<>();
                Object patternsRaw = phaseMap.get("patterns");
                if (patternsRaw instanceof List<?> rawList) {
                    for (Object entry : rawList) patterns.add(String.valueOf(entry));
                }
                phases.add(new BossPhase(threshold, patterns, interval, announce));
            }
            phases.sort((a, b) -> Double.compare(b.healthPercentageThreshold(), a.healthPercentageThreshold()));
            definition.setPhases(phases);

            ConfigurationSection lootSection = section.getConfigurationSection("loot");
            if (lootSection != null) {
                definition.setLootConfig(new BossLootConfig(
                        lootSection.getStringList("materials"),
                        parseRarity(lootSection.getString("guaranteed-rarity", "LEGENDARY")),
                        lootSection.getDouble("money", 500.0),
                        lootSection.getLong("exp", 1000L)));
            }
            definitionsById.put(id, definition);
        }
    }

    public BossDefinition get(String id) {
        return definitionsById.get(id);
    }

    public List<BossDefinition> getAll() {
        return new ArrayList<>(definitionsById.values());
    }

    public BossDefinition getForEntityType(EntityType entityType) {
        return definitionsById.values().stream()
                .filter(definition -> definition.getBaseEntityType() == entityType)
                .findFirst()
                .orElse(null);
    }

    private void createDefaultBosses() {
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("bosses.forest_tyrant.name", "Forest Tyrant");
        yaml.set("bosses.forest_tyrant.base-entity", "ZOMBIE");
        yaml.set("bosses.forest_tyrant.level", 30);
        yaml.set("bosses.forest_tyrant.health-multiplier", 4.0);
        yaml.set("bosses.forest_tyrant.damage-multiplier", 1.5);
        yaml.set("bosses.forest_tyrant.scale-multiplier", 1.35);
        yaml.set("bosses.forest_tyrant.phases", List.of(
                Map.of("health-percent", 100.0, "attack-interval-ticks", 140, "patterns", List.of("SLAM"), "announcement", ""),
                Map.of("health-percent", 66.0, "attack-interval-ticks", 120, "patterns", List.of("SLAM", "SUMMON_ADDS"), "announcement", ""),
                Map.of("health-percent", 33.0, "attack-interval-ticks", 100, "patterns", List.of("ENRAGE_BUFF", "PROJECTILE_VOLLEY"), "announcement", "")));
        yaml.set("bosses.forest_tyrant.loot.materials", List.of("DIAMOND_SWORD", "DIAMOND_CHESTPLATE", "SHIELD"));
        yaml.set("bosses.forest_tyrant.loot.guaranteed-rarity", "LEGENDARY");
        yaml.set("bosses.forest_tyrant.loot.money", 500.0);
        yaml.set("bosses.forest_tyrant.loot.exp", 1200);

        yaml.set("bosses.frost_sovereign.name", "Frost Sovereign");
        yaml.set("bosses.frost_sovereign.base-entity", "STRAY");
        yaml.set("bosses.frost_sovereign.level", 60);
        yaml.set("bosses.frost_sovereign.health-multiplier", 5.0);
        yaml.set("bosses.frost_sovereign.damage-multiplier", 1.7);
        yaml.set("bosses.frost_sovereign.scale-multiplier", 1.40);
        yaml.set("bosses.frost_sovereign.phases", List.of(
                Map.of("health-percent", 100.0, "attack-interval-ticks", 130, "patterns", List.of("PROJECTILE_VOLLEY"), "announcement", ""),
                Map.of("health-percent", 50.0, "attack-interval-ticks", 110, "patterns", List.of("PROJECTILE_VOLLEY", "SUMMON_ADDS"), "announcement", ""),
                Map.of("health-percent", 20.0, "attack-interval-ticks", 90, "patterns", List.of("ENRAGE_BUFF", "SLAM"), "announcement", "")));
        yaml.set("bosses.frost_sovereign.loot.materials", List.of("NETHERITE_SWORD", "DIAMOND_HELMET", "BOW"));
        yaml.set("bosses.frost_sovereign.loot.guaranteed-rarity", "LEGENDARY");
        yaml.set("bosses.frost_sovereign.loot.money", 800.0);
        yaml.set("bosses.frost_sovereign.loot.exp", 2000);

        yaml.set("bosses.void_reaper.name", "Void Reaper");
        yaml.set("bosses.void_reaper.base-entity", "WITHER_SKELETON");
        yaml.set("bosses.void_reaper.level", 99);
        yaml.set("bosses.void_reaper.health-multiplier", 6.0);
        yaml.set("bosses.void_reaper.damage-multiplier", 2.0);
        yaml.set("bosses.void_reaper.scale-multiplier", 1.45);
        yaml.set("bosses.void_reaper.phases", List.of(
                Map.of("health-percent", 100.0, "attack-interval-ticks", 120, "patterns", List.of("SLAM", "SUMMON_ADDS"), "announcement", ""),
                Map.of("health-percent", 60.0, "attack-interval-ticks", 100, "patterns", List.of("PROJECTILE_VOLLEY", "SUMMON_ADDS"), "announcement", ""),
                Map.of("health-percent", 25.0, "attack-interval-ticks", 80, "patterns", List.of("ENRAGE_BUFF", "SLAM", "PROJECTILE_VOLLEY"), "announcement", "")));
        yaml.set("bosses.void_reaper.loot.materials", List.of("NETHERITE_SWORD", "NETHERITE_CHESTPLATE", "TRIDENT"));
        yaml.set("bosses.void_reaper.loot.guaranteed-rarity", "LEGENDARY");
        yaml.set("bosses.void_reaper.loot.money", 1500.0);
        yaml.set("bosses.void_reaper.loot.exp", 4000);

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create default bosses.yml", e);
        }
    }

    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 100;
    }

    private EntityType parseEntityType(String raw) {
        try {
            return EntityType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return EntityType.ZOMBIE;
        }
    }

    private ItemRarity parseRarity(String raw) {
        try {
            return ItemRarity.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ItemRarity.LEGENDARY;
        }
    }
}
