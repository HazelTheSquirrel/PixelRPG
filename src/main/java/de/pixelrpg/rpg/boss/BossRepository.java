package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
            definition.setKind(parseKind(section.getString("kind", "BIOME")));
            definition.setBaseEntityType(parseEntityType(section.getString("base-entity", "ZOMBIE")));
            definition.setBiome(parseBiome(section.getString("biome")));
            definition.setLevel(section.getInt("level", 30));
            definition.setHealthMultiplier(section.getDouble("health-multiplier", 4.0));
            definition.setDamageMultiplier(section.getDouble("damage-multiplier", 1.5));
            definition.setScaleMultiplier(section.getDouble("scale-multiplier", 1.35));
            definition.setAttackIntervalTicks(section.getInt("attack-interval-ticks", 100));
            definition.setAttackPatternIds(section.getStringList("attack-patterns"));

            List<BossPhase> phases = new ArrayList<>();
            for (Map<?, ?> phaseMap : section.getMapList("phases")) {
                double threshold = toDouble(phaseMap.get("health-percent"));
                int interval = toInt(phaseMap.get("attack-interval-ticks"), definition.getAttackIntervalTicks());
                String announce = stringValue(phaseMap.get("announcement"));
                List<String> patterns = new ArrayList<>();
                Object patternsRaw = phaseMap.get("patterns");
                if (patternsRaw instanceof List<?> rawList) for (Object entry : rawList) patterns.add(String.valueOf(entry));
                phases.add(new BossPhase(threshold, patterns, interval, announce));
            }
            phases.sort((a, b) -> Double.compare(b.healthPercentageThreshold(), a.healthPercentageThreshold()));
            definition.setPhases(phases);

            ConfigurationSection lootSection = section.getConfigurationSection("loot");
            if (lootSection != null) {
                List<BossLootEntry> chanceDrops = new ArrayList<>();
                for (Map<?, ?> entry : lootSection.getMapList("chance-drops")) {
                    String material = stringValue(entry.get("material"));
                    if (material.isBlank()) continue;
                    chanceDrops.add(new BossLootEntry(material, toDouble(entry.get("chance-percent")), parseRarity(stringValue(entry.get("rarity"), "RARE"))));
                }
                definition.setLootConfig(new BossLootConfig(lootSection.getStringList("guaranteed"), chanceDrops,
                        lootSection.getDouble("money", 0.0D), lootSection.getLong("exp", 0L)));
            }

            if (definition.getKind() == BossKind.BIOME && definition.getBiome() == null) {
                plugin.getLogger().warning("Ignoring biome boss without biome: " + id);
                continue;
            }
            if (definition.getKind() == BossKind.WORLD_EVENT) definition.setBiome(null);
            if (definition.getKind() == BossKind.BIOME && !definition.getPhases().isEmpty()) {
                plugin.getLogger().warning("Ignoring phases for biome boss: " + id);
                definition.setPhases(List.of());
            }
            definitionsById.put(id, definition);
        }
        validateBiomeUniqueness();
    }

    public BossDefinition get(String id) { return definitionsById.get(id); }
    public List<BossDefinition> getAll() { return new ArrayList<>(definitionsById.values()); }
    public List<BossDefinition> getWorldBosses() { return definitionsById.values().stream().filter(d -> d.getKind() == BossKind.WORLD_EVENT).toList(); }
    public BossDefinition getBiomeBoss(Biome biome) {
        return definitionsById.values().stream().filter(d -> d.getKind() == BossKind.BIOME && d.getBiome() == biome).findFirst().orElse(null);
    }

    private void validateBiomeUniqueness() {
        Map<Biome, String> seen = new HashMap<>();
        for (BossDefinition definition : new ArrayList<>(definitionsById.values())) {
            if (definition.getKind() != BossKind.BIOME || definition.getBiome() == null) continue;
            String previous = seen.putIfAbsent(definition.getBiome(), definition.getId());
            if (previous != null) {
                definitionsById.remove(definition.getId());
                plugin.getLogger().warning("Ignoring duplicate biome boss " + definition.getId() + " for biome " + definition.getBiome() + "; already used by " + previous + ".");
            }
        }
    }

    private void createDefaultBosses() {
        YamlConfiguration yaml = new YamlConfiguration();
        setBase(yaml, "grove_warden", "Grove Warden", "BIOME", "ZOMBIE", "PLAINS", 18, 5.0, 1.8, 1.25, 110, List.of("SLAM"));
        yaml.set("bosses.grove_warden.loot.guaranteed", List.of("IRON_INGOT"));
        yaml.set("bosses.grove_warden.loot.chance-drops", List.of(Map.of("material", "DIAMOND", "chance-percent", 8.0, "rarity", "RARE")));
        yaml.set("bosses.grove_warden.loot.money", 150.0); yaml.set("bosses.grove_warden.loot.exp", 350L);

        setBase(yaml, "dune_stalker", "Dune Stalker", "BIOME", "HUSK", "DESERT", 28, 7.0, 2.0, 1.30, 100, List.of("PROJECTILE_VOLLEY", "SLAM"));
        yaml.set("bosses.dune_stalker.loot.guaranteed", List.of("GOLD_INGOT"));
        yaml.set("bosses.dune_stalker.loot.chance-drops", List.of(Map.of("material", "DIAMOND", "chance-percent", 10.0, "rarity", "EPIC")));
        yaml.set("bosses.dune_stalker.loot.money", 300.0); yaml.set("bosses.dune_stalker.loot.exp", 650L);

        setBase(yaml, "frostfang", "Frostfang", "BIOME", "STRAY", "SNOWY_PLAINS", 42, 10.0, 2.2, 1.35, 90, List.of("PROJECTILE_VOLLEY", "SLAM"));
        yaml.set("bosses.frostfang.loot.guaranteed", List.of("DIAMOND"));
        yaml.set("bosses.frostfang.loot.chance-drops", List.of(Map.of("material", "NETHERITE_SCRAP", "chance-percent", 6.0, "rarity", "LEGENDARY")));
        yaml.set("bosses.frostfang.loot.money", 600.0); yaml.set("bosses.frostfang.loot.exp", 1200L);

        setBase(yaml, "rift_colossus", "Rift Colossus", "WORLD_EVENT", "RAVAGER", null, 80, 35.0, 5.0, 1.75, 100, List.of());
        yaml.set("bosses.rift_colossus.phases", List.of(
                Map.of("health-percent", 100.0, "attack-interval-ticks", 120, "patterns", List.of("SLAM", "PROJECTILE_VOLLEY"), "announcement", "The Rift Colossus has awakened."),
                Map.of("health-percent", 66.0, "attack-interval-ticks", 95, "patterns", List.of("SLAM", "SUMMON_ADDS", "PROJECTILE_VOLLEY"), "announcement", "The Rift tears open around the Colossus."),
                Map.of("health-percent", 33.0, "attack-interval-ticks", 70, "patterns", List.of("ENRAGE_BUFF", "SLAM", "SUMMON_ADDS", "PROJECTILE_VOLLEY"), "announcement", "The Colossus enters its final rage.")));
        yaml.set("bosses.rift_colossus.loot.guaranteed", List.of("NETHER_STAR"));
        yaml.set("bosses.rift_colossus.loot.chance-drops", List.of(
                Map.of("material", "NETHERITE_INGOT", "chance-percent", 15.0, "rarity", "LEGENDARY"),
                Map.of("material", "DIAMOND_BLOCK", "chance-percent", 25.0, "rarity", "EPIC")));
        yaml.set("bosses.rift_colossus.loot.money", 2500.0); yaml.set("bosses.rift_colossus.loot.exp", 6000L);

        try { file.getParentFile().mkdirs(); yaml.save(file); }
        catch (IOException e) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create default bosses.yml", e); }
    }

    private void setBase(YamlConfiguration yaml, String id, String name, String kind, String entity, String biome,
                         int level, double hp, double damage, double scale, int attackInterval, List<String> patterns) {
        String path = "bosses." + id;
        yaml.set(path + ".name", name); yaml.set(path + ".kind", kind); yaml.set(path + ".base-entity", entity);
        if (biome != null) yaml.set(path + ".biome", biome);
        yaml.set(path + ".level", level); yaml.set(path + ".health-multiplier", hp); yaml.set(path + ".damage-multiplier", damage);
        yaml.set(path + ".scale-multiplier", scale); yaml.set(path + ".attack-interval-ticks", attackInterval); yaml.set(path + ".attack-patterns", patterns);
    }

    private double toDouble(Object value) { return value instanceof Number number ? number.doubleValue() : 0.0D; }
    private int toInt(Object value, int fallback) { return value instanceof Number number ? number.intValue() : fallback; }
    private String stringValue(Object value) { return stringValue(value, ""); }
    private String stringValue(Object value, String fallback) { return value == null ? fallback : String.valueOf(value); }
    private EntityType parseEntityType(String raw) { try { return EntityType.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return EntityType.ZOMBIE; } }
    private BossKind parseKind(String raw) { try { return BossKind.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return BossKind.BIOME; } }
    private Biome parseBiome(String raw) { if (raw == null || raw.isBlank()) return null; try { return Biome.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException ignored) { plugin.getLogger().warning("Unknown boss biome: " + raw); return null; } }
    private ItemRarity parseRarity(String raw) { try { return ItemRarity.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return ItemRarity.RARE; } }
}
