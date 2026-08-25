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

            List<Biome> biomes = new ArrayList<>();
            for (String raw : section.getStringList("biomes")) {
                Biome parsed = parseBiome(raw);
                if (parsed != null) biomes.add(parsed);
            }
            if (biomes.isEmpty()) {
                Biome legacyBiome = parseBiome(section.getString("biome"));
                if (legacyBiome != null) biomes.add(legacyBiome);
            }
            definition.setBiomes(biomes);

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

            if (definition.getKind() == BossKind.BIOME && definition.getBiomes().isEmpty()) {
                plugin.getLogger().warning("Ignoring biome boss without biome group: " + id);
                continue;
            }
            if (definition.getKind() == BossKind.WORLD_EVENT) definition.setBiomes(List.of());
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
    public BossDefinition getBiomeBoss(Biome biome) { return definitionsById.values().stream().filter(d -> d.getKind() == BossKind.BIOME && d.matchesBiome(biome)).findFirst().orElse(null); }

    private void validateBiomeUniqueness() {
        Map<Biome, String> seen = new HashMap<>();
        for (BossDefinition definition : new ArrayList<>(definitionsById.values())) {
            if (definition.getKind() != BossKind.BIOME) continue;
            for (Biome biome : definition.getBiomes()) {
                String previous = seen.putIfAbsent(biome, definition.getId());
                if (previous != null) {
                    definitionsById.remove(definition.getId());
                    plugin.getLogger().warning("Ignoring duplicate biome boss " + definition.getId() + " for biome " + biome + "; already used by " + previous + ".");
                    break;
                }
            }
        }
    }

    private void createDefaultBosses() {
        YamlConfiguration yaml = new YamlConfiguration();

        boss(yaml, "plunderer", "Der Plünderer", "PILLAGER", 12, 4.0, 1.6, 1.15, 100, List.of("PROJECTILE_VOLLEY"), List.of("PLAINS", "SUNFLOWER_PLAINS", "WINDSWEPT_SAVANNA"), "pixelrpg:boss/pluenderer_siegel", 150, 300);
        boss(yaml, "bee_queen", "Die Bienenkönigin", "BEE", 16, 5.0, 1.5, 1.20, 80, List.of("SLAM"), List.of("FOREST", "FLOWER_FOREST", "BIRCH_FOREST", "OLD_GROWTH_BIRCH_FOREST"), "pixelrpg:boss/bienenkoenigin", 200, 400);
        boss(yaml, "forest_witch", "Die Hexe des Waldes", "WITCH", 22, 5.5, 1.7, 1.20, 90, List.of("PROJECTILE_VOLLEY"), List.of("DARK_FOREST"), "pixelrpg:boss/hexenkessel", 250, 500);
        boss(yaml, "creaking_heart", "Das Knarzende Herz", "CREAKING", 26, 6.0, 1.8, 1.25, 90, List.of("SLAM"), List.of("PALE_GARDEN"), "pixelrpg:boss/knarzendes_herzstueck", 300, 650);
        boss(yaml, "jungle_warden", "Der Dschungelwächter", "PANDA", 30, 7.0, 1.9, 1.30, 90, List.of("SLAM"), List.of("JUNGLE", "SPARSE_JUNGLE", "BAMBOO_JUNGLE"), "pixelrpg:boss/dschungel_amulett", 350, 800);
        boss(yaml, "swamp_witch", "Die Sumpfhexe", "WITCH", 32, 7.0, 1.9, 1.30, 85, List.of("PROJECTILE_VOLLEY"), List.of("SWAMP", "MANGROVE_SWAMP"), "pixelrpg:boss/sumpftrank", 400, 900);
        boss(yaml, "husk_king", "Der Husk-König", "HUSK", 34, 7.5, 2.0, 1.30, 90, List.of("SLAM"), List.of("DESERT"), "pixelrpg:boss/husk_siegel", 450, 1000);
        boss(yaml, "ravager_chief", "Der Ravager-Häuptling", "RAVAGER", 38, 9.0, 2.2, 1.35, 85, List.of("SLAM"), List.of("SAVANNA", "SAVANNA_PLATEAU", "WINDSWEPT_SAVANNA_PLATEAU"), "pixelrpg:boss/ravager_trophaee", 500, 1200);
        boss(yaml, "sandstone_colossus", "Der Sandstein-Koloss", "HUSK", 40, 10.0, 2.2, 1.40, 95, List.of("PROJECTILE_VOLLEY", "SLAM"), List.of("BADLANDS", "WOODED_BADLANDS", "ERODED_BADLANDS"), "pixelrpg:boss/goldenes_fossil", 550, 1300);
        boss(yaml, "frostwolf", "Der Frostwolf", "WOLF", 42, 10.0, 2.3, 1.35, 75, List.of("SLAM"), List.of("TAIGA", "OLD_GROWTH_PINE_TAIGA", "OLD_GROWTH_SPRUCE_TAIGA", "SNOWY_TAIGA"), "pixelrpg:boss/frostwolf_fang", 600, 1400);
        boss(yaml, "stray_warrior", "Der Streuner-Krieger", "STRAY", 45, 11.0, 2.4, 1.35, 80, List.of("PROJECTILE_VOLLEY"), List.of("SNOWY_PLAINS", "ICE_SPIKES", "SNOWY_TAIGA"), "pixelrpg:boss/frostpfeil_koecher", 650, 1600);
        boss(yaml, "mountain_goat", "Der Bergbock", "GOAT", 48, 12.0, 2.5, 1.40, 75, List.of("SLAM"), List.of("JAGGED_PEAKS", "FROZEN_PEAKS", "STONY_PEAKS"), "pixelrpg:boss/horn_des_berges", 700, 1800);
        boss(yaml, "wild_goat", "Der wilde Bergbock", "GOAT", 44, 10.0, 2.3, 1.35, 75, List.of("SLAM"), List.of("MEADOW"), "pixelrpg:boss/wildhorn", 650, 1500);
        boss(yaml, "blossom_warden", "Der Blütenwächter", "BEE", 50, 12.0, 2.4, 1.35, 80, List.of("SLAM"), List.of("CHERRY_GROVE"), "pixelrpg:boss/bluetenhonig", 800, 2000);
        boss(yaml, "drowned_captain", "Der Ertrunkene Kapitän", "DROWNED", 52, 13.0, 2.5, 1.35, 80, List.of("PROJECTILE_VOLLEY", "SLAM"), List.of("BEACH", "SNOWY_BEACH", "STONY_SHORE"), "pixelrpg:boss/kapitaens_nautilus", 850, 2200);
        boss(yaml, "river_warden", "Der Flusswächter", "DROWNED", 50, 12.0, 2.4, 1.35, 80, List.of("PROJECTILE_VOLLEY"), List.of("RIVER", "FROZEN_RIVER"), "pixelrpg:boss/flusskiesel", 850, 2100);
        boss(yaml, "guardian_of_depths", "Der Tiefenwächter", "GUARDIAN", 58, 15.0, 2.6, 1.40, 90, List.of("PROJECTILE_VOLLEY"), List.of("OCEAN", "DEEP_OCEAN", "COLD_OCEAN", "DEEP_COLD_OCEAN", "LUKEWARM_OCEAN", "DEEP_LUKEWARM_OCEAN", "WARM_OCEAN", "FROZEN_OCEAN", "DEEP_FROZEN_OCEAN"), "pixelrpg:boss/auge_der_tiefe", 1000, 2600);
        boss(yaml, "mycelium_king", "Der Myzelkönig", "MOOSHROOM", 55, 14.0, 2.4, 1.35, 85, List.of("SLAM"), List.of("MUSHROOM_FIELDS"), "pixelrpg:boss/myzelkern", 900, 2400);
        boss(yaml, "cave_hunter", "Der Höhlenjäger", "SPIDER", 46, 11.0, 2.2, 1.30, 75, List.of("SLAM"), List.of("DRIPSTONE_CAVES", "LUSH_CAVES"), "pixelrpg:boss/spinnenauge_des_jaegers", 750, 1900);
        boss(yaml, "ancient_warden", "Der Uralte Wächter", "WARDEN", 65, 20.0, 3.0, 1.45, 100, List.of("SLAM"), List.of("DEEP_DARK"), "pixelrpg:boss/echoherz", 1200, 3500);
        boss(yaml, "nether_lord", "Der Netherfürst", "PIGLIN_BRUTE", 68, 18.0, 3.0, 1.45, 80, List.of("SLAM", "PROJECTILE_VOLLEY"), List.of("NETHER_WASTES"), "pixelrpg:boss/netherkern", 1300, 3800);
        boss(yaml, "crimson_beast", "Die Karmesinbestie", "HOGLIN", 62, 17.0, 2.8, 1.40, 75, List.of("SLAM"), List.of("CRIMSON_FOREST"), "pixelrpg:boss/karmesinherz", 1200, 3300);
        boss(yaml, "enderman_lord", "Der Endermanfürst", "ENDERMAN", 72, 20.0, 3.0, 1.45, 80, List.of("SLAM", "PROJECTILE_VOLLEY"), List.of("WARPED_FOREST"), "pixelrpg:boss/gebundene_enderperle", 1400, 4000);
        boss(yaml, "soul_lord", "Der Seelenfürst", "WITHER_SKELETON", 70, 19.0, 3.1, 1.45, 80, List.of("PROJECTILE_VOLLEY", "SLAM"), List.of("SOUL_SAND_VALLEY"), "pixelrpg:boss/seelenfragment", 1400, 3900);
        boss(yaml, "magma_colossus", "Der Magmakoloss", "MAGMA_CUBE", 66, 18.0, 2.8, 1.45, 70, List.of("SLAM"), List.of("BASALT_DELTAS"), "pixelrpg:boss/magmaherz", 1350, 3700);
        boss(yaml, "end_king", "Der Endkönig", "SHULKER", 78, 22.0, 3.2, 1.50, 85, List.of("PROJECTILE_VOLLEY", "SLAM"), List.of("THE_END", "END_HIGHLANDS", "END_MIDLANDS", "SMALL_END_ISLANDS", "END_BARRENS"), "pixelrpg:boss/shulkerkern", 1800, 5000);

        try { file.getParentFile().mkdirs(); yaml.save(file); }
        catch (IOException e) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create default bosses.yml", e); }
    }

    private void boss(YamlConfiguration yaml, String id, String name, String entity, int level, double hp, double damage,
                      double scale, int interval, List<String> patterns, List<String> biomes, String customDrop,
                      double money, long exp) {
        String path = "bosses." + id;
        yaml.set(path + ".name", name);
        yaml.set(path + ".kind", "BIOME");
        yaml.set(path + ".base-entity", entity);
        yaml.set(path + ".biomes", biomes);
        yaml.set(path + ".level", level);
        yaml.set(path + ".health-multiplier", hp);
        yaml.set(path + ".damage-multiplier", damage);
        yaml.set(path + ".scale-multiplier", scale);
        yaml.set(path + ".attack-interval-ticks", interval);
        yaml.set(path + ".attack-patterns", patterns);
        yaml.set(path + ".loot.guaranteed", List.of(customDrop));
        yaml.set(path + ".loot.money", money);
        yaml.set(path + ".loot.exp", exp);
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
