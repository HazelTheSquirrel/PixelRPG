package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.core.Level;
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
import java.util.logging.Level as LogLevel;

public final class BossRepository {
    private final Plugin plugin;
    private final File file;
    private final Map<String, BossDefinition> definitionsById = new ConcurrentHashMap<>();

    public BossRepository(Plugin plugin) { this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "bosses.yml"); }

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
            definition.setHealthMultiplier(section.getDouble("health-multiplier", 5.0));
            definition.setDamageMultiplier(section.getDouble("damage-multiplier", 2.0));
            List<BossPhase> phases = new ArrayList<>();
            for (Map<?, ?> phaseMap : section.getMapList("phases")) {
                double threshold = toDouble(phaseMap.get("health-percent"));
                int interval = toInt(phaseMap.get("attack-interval-ticks"));
                Object announceRaw = phaseMap.get("announcement");
                String announce = announceRaw != null ? String.valueOf(announceRaw) : "";
                List<String> patterns = new ArrayList<>();
                Object patternsRaw = phaseMap.get("patterns");
                if (patternsRaw instanceof List<?> rawList) for (Object entry : rawList) patterns.add(String.valueOf(entry));
                phases.add(new BossPhase(threshold, patterns, interval, announce));
            }
            phases.sort((a, b) -> Double.compare(b.healthPercentageThreshold(), a.healthPercentageThreshold()));
            definition.setPhases(phases);
            ConfigurationSection lootSection = section.getConfigurationSection("loot");
            if (lootSection != null) {
                List<String> materials = lootSection.getStringList("materials");
                ItemRarity guaranteedRarity = parseRarity(lootSection.getString("guaranteed-rarity", "LEGENDARY"));
                definition.setLootConfig(new BossLootConfig(materials, guaranteedRarity, lootSection.getDouble("money", 500.0), lootSection.getLong("exp", 1000L)));
            }
            definitionsById.put(id, definition);
        }
    }

    private void createDefaultBosses() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("bosses.forest_tyrant.name", "Forest Tyrant");
        yaml.set("bosses.forest_tyrant.base-entity", "ZOMBIE");
        yaml.set("bosses.forest_tyrant.level", 30);
        yaml.set("bosses.forest_tyrant.health-multiplier", 6.0);
        yaml.set("bosses.forest_tyrant.damage-multiplier", 2.0);
        yaml.set("bosses.forest_tyrant.phases", List.of(
                Map.of("health-percent", 100.0, "attack-interval-ticks", 100, "patterns", List.of("SLAM"), "announcement", "The Forest Tyrant awakens!"),
                Map.of("health-percent", 66.0, "attack-interval-ticks", 80, "patterns", List.of("SLAM", "SUMMON_ADDS"), "announcement", "The Forest Tyrant summons reinforcements!"),
                Map.of("health-percent", 33.0, "attack-interval-ticks", 60, "patterns", List.of("ENRAGE_BUFF", "PROJECTILE_VOLLEY"), "announcement", "The Forest Tyrant enters its final rage!")));
        yaml.set("bosses.forest_tyrant.loot.materials", List.of("DIAMOND_SWORD", "DIAMOND_CHESTPLATE", "SHIELD"));
        yaml.set("bosses.forest_tyrant.loot.guaranteed-rarity", "LEGENDARY");
        yaml.set("bosses.forest_tyrant.loot.money", 500.0);
        yaml.set("bosses.forest_tyrant.loot.exp", 1200);

        yaml.set("bosses.frost_sovereign.name", "Frost Sovereign");
        yaml.set("bosses.frost_sovereign.base-entity", "STRAY");
        yaml.set("bosses.frost_sovereign.level", 60);
        yaml.set("bosses.frost_sovereign.health-multiplier", 8.0);
        yaml.set("bosses.frost_sovereign.damage-multiplier", 2.4);
        yaml.set("bosses.frost_sovereign.phases", List.of(
                Map.of("health-percent", 100.0, "attack-interval-ticks", 90, "patterns", List.of("PROJECTILE_VOLLEY"), "announcement", "The Frost Sovereign rises!"),
                Map.of("health-percent", 50.0, "attack-interval-ticks", 70, "patterns", List.of("PROJECTILE_VOLLEY", "SUMMON_ADDS"), "announcement", "Frost minions answer the call!"),
                Map.of("health-percent", 20.0, "attack-interval-ticks", 50, "patterns", List.of("ENRAGE_BUFF", "SLAM"), "announcement", "The Sovereign's rage freezes the battlefield!")));
        yaml.set("bosses.frost_sovereign.loot.materials", List.of("NETHERITE_SWORD", "DIAMOND_HELMET", "BOW"));
        yaml.set("bosses.frost_sovereign.loot.guaranteed-rarity", "LEGENDARY");
        yaml.set("bosses.frost_sovereign.loot.money", 800.0);
        yaml.set("bosses.frost_sovereign.loot.exp", 2000);

        yaml.set("bosses.void_reaper.name", "Void Reaper");
        yaml.set("bosses.void_reaper.base-entity", "WITHER_SKELETON");
        yaml.set("bosses.void_reaper.level", 99);
        yaml.set("bosses.void_reaper.health-multiplier", 12.0);
        yaml.set("bosses.void_reaper.damage-multiplier", 3.0);
        yaml.set("bosses.void_reaper.phases", List.of(
                Map.of("health-percent", 100.0, "attack-interval-ticks", 80, "patterns", List.of("SLAM", "SUMMON_ADDS"), "announcement", "The Void Reaper tears through reality!"),
                Map.of("health-percent", 60.0, "attack-interval-ticks", 60, "patterns", List.of("PROJECTILE_VOLLEY", "SUMMON_ADDS"), "announcement", "Reality fractures further!"),
                Map.of("health-percent", 25.0, "attack-interval-ticks", 40, "patterns", List.of("ENRAGE_BUFF", "SLAM", "PROJECTILE_VOLLEY"), "announcement", "The Void Reaper unleashes total annihilation!")));
        yaml.set("bosses.void_reaper.loot.materials", List.of("NETHERITE_SWORD", "NETHERITE_CHESTPLATE", "TRIDENT"));
        yaml.set("bosses.void_reaper.loot.guaranteed-rarity", "LEGENDARY");
        yaml.set("bosses.void_reaper.loot.money", 1500.0);
        yaml.set("bosses.void_reaper.loot.exp", 4000);

        try { yaml.save(file); }
        catch (IOException e) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create default bosses.yml", e); }
    }

    public BossDefinition get(String id) { return definitionsById.get(id); }
    public List<BossDefinition> getAll() { return new ArrayList<>(definitionsById.values()); }
    private double toDouble(Object value) { return value instanceof Number number ? number.doubleValue() : 0.0; }
    private int toInt(Object value) { return value instanceof Number number ? number.intValue() : 100; }
    private EntityType parseEntityType(String raw) { try { return EntityType.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return EntityType.ZOMBIE; } }
    private ItemRarity parseRarity(String raw) { try { return ItemRarity.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return ItemRarity.LEGENDARY; } }
}
