package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.item.ItemRarity;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
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
        boolean changed = ensureWorldBossDefaults(yaml);
        changed |= ensureCustomProgressionLoot(yaml);
        if (changed) {
            try {
                yaml.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to migrate boss loot defaults", e);
            }
        }
        ConfigurationSection root = yaml.getConfigurationSection("bosses");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            BossDefinition definition = new BossDefinition(id, section.getString("name", id));
            definition.setKind(parseKind(section.getString("kind", "BIOME")));
            definition.setBaseEntityType(parseEntityType(section.getString("base-entity", "ZOMBIE")));
            List<Biome> biomes = new ArrayList<>();
            for (String raw : section.getStringList("biomes")) { Biome parsed = parseBiome(raw); if (parsed != null) biomes.add(parsed); }
            if (biomes.isEmpty()) { Biome legacyBiome = parseBiome(section.getString("biome")); if (legacyBiome != null) biomes.add(legacyBiome); }
            definition.setBiomes(biomes);
            definition.setLevel(section.getInt("level", 30));
            definition.setHealthMultiplier(section.getDouble("health-multiplier", 4.0) * 0.5D);
            definition.setDamageMultiplier(section.getDouble("damage-multiplier", 1.5) * 0.5D);
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
            if (definition.getKind() == BossKind.WORLD_EVENT && definition.getPhases().isEmpty()) {
                plugin.getLogger().warning("Ignoring world boss without phases: " + id);
                continue;
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

    private boolean ensureCustomProgressionLoot(YamlConfiguration yaml) {
        Map<String, String> progressionLoot = Map.of(
                "plunderer", "pixelrpg:weapons/iron_sword/common_3",
                "forest_witch", "pixelrpg:armor/chainmail_helmet/schattengeflecht_20",
                "ravager_chief", "pixelrpg:armor/iron_chestplate/stahlwall_30",
                "sandstone_colossus", "pixelrpg:weapons/gold_sword/uncommon_25",
                "guardian_of_depths", "pixelrpg:armor/gold_chestplate/sonnengewand_45",
                "ancient_warden", "pixelrpg:armor/diamond_chestplate/kristallwache_60",
                "nether_lord", "pixelrpg:armor/netherite_chestplate/hoellenschmiede_80",
                "end_king", "pixelrpg:weapons/netherite_sword/legendary_90"
        );
        boolean changed = false;
        for (Map.Entry<String, String> entry : progressionLoot.entrySet()) {
            String path = "bosses." + entry.getKey();
            if (!yaml.isConfigurationSection(path)) continue;
            List<String> guaranteed = new ArrayList<>(yaml.getStringList(path + ".loot.guaranteed"));
            if (guaranteed.stream().anyMatch(value -> value.equalsIgnoreCase(entry.getValue()))) continue;
            guaranteed.add(entry.getValue());
            yaml.set(path + ".loot.guaranteed", guaranteed);
            changed = true;
        }
        return changed;
    }

    private boolean ensureWorldBossDefaults(YamlConfiguration yaml) {
        boolean changed = false;
        changed |= ensureWorldBoss(yaml, "rift_colossus", "Der Risskoloss", "RAVAGER", 80, 35.0, 5.0, 1.75, 2500, 6000, "pixelrpg:boss/risskern", "NETHER_STAR", "NETHERITE_INGOT", 15.0, "LEGENDARY");
        changed |= ensureWorldBoss(yaml, "storm_lord", "Der Sturmherrscher", "EVOKER", 84, 38.0, 4.8, 1.55, 3000, 7000, "pixelrpg:boss/sturmherz", "TOTEM_OF_UNDYING", "DIAMOND_BLOCK", 18.0, "LEGENDARY");
        changed |= ensureWorldBoss(yaml, "abyss_lord", "Der Abgrundfürst", "ELDER_GUARDIAN", 88, 42.0, 4.5, 1.55, 3400, 8000, "pixelrpg:boss/abgrundkern", "HEART_OF_THE_SEA", "SPONGE", 20.0, "EPIC");
        changed |= ensureWorldBoss(yaml, "soul_devourer", "Der Seelenverschlinger", "WITHER_SKELETON", 92, 45.0, 5.4, 1.50, 3800, 9000, "pixelrpg:boss/seelenkrone", "NETHER_STAR", "NETHERITE_SCRAP", 20.0, "LEGENDARY");
        changed |= ensureWorldBoss(yaml, "end_harbinger", "Der Endbote", "ENDERMAN", 96, 48.0, 5.2, 1.55, 4200, 10000, "pixelrpg:boss/endriss", "DRAGON_BREATH", "ENDER_EYE", 25.0, "LEGENDARY");
        changed |= ensureWorldBoss(yaml, "ancient_world_warden", "Der Uralte Weltenwächter", "WARDEN", 100, 55.0, 6.0, 1.65, 5000, 12000, "pixelrpg:boss/weltenherz", "NETHER_STAR", "ECHO_SHARD", 30.0, "LEGENDARY");
        return changed;
    }

    private boolean ensureWorldBoss(YamlConfiguration yaml, String id, String name, String entity, int level,
                                    double hp, double damage, double scale, double money, long exp, String customDrop,
                                    String guaranteedMaterial, String chanceMaterial, double chancePercent, String rarity) {
        String path = "bosses." + id;
        if (yaml.isConfigurationSection(path)) return false;
        worldBoss(yaml, id, name, entity, level, hp, damage, scale, money, exp, customDrop,
                guaranteedMaterial, chanceMaterial, chancePercent, rarity);
        return true;
    }

    private void createDefaultBosses() {
        YamlConfiguration yaml = new YamlConfiguration();
        boss(yaml, "plunderer", "Der Plünderer", "PILLAGER", 12, 4.0, 1.6, 1.15, 100, List.of("PROJECTILE_VOLLEY"), List.of("PLAINS", "SUNFLOWER_PLAINS"), "pixelrpg:boss/pluenderer_siegel", 150, 300);
        boss(yaml, "bee_queen", "Die Bienenkönigin", "BEE", 16, 5.0, 1.5, 1.20, 80, List.of("SLAM"), List.of("FOREST", "FLOWER_FOREST", "BIRCH_FOREST", "OLD_GROWTH_BIRCH_FOREST"), "pixelrpg:boss/bienenkoenigin", 200, 400);
        boss(yaml, "forest_witch", "Die Hexe des Waldes", "WITCH", 22, 5.5, 1.7, 1.20, 90, List.of("PROJECTILE_VOLLEY"), List.of("DARK_FOREST"), "pixelrpg:boss/hexenkessel", 250, 500);
        boss(yaml, "creaking_heart", "Das Knarzende Herz", "CREAKING", 26, 6.0, 1.8, 1.25, 90, List.of("SLAM"), List.of("PALE_GARDEN"), "pixelrpg:boss/knarzendes_herzstueck", 300, 650);
        boss(yaml, "jungle_warden", "Der Dschungelwächter", "PANDA", 30, 7.0, 1.9, 1.30, 90, List.of("SLAM"), List.of("JUNGLE", "SPARSE_JUNGLE", "BAMBOO_JUNGLE"), "pixelrpg:boss/dschungel_amulett", 350, 800);
        boss(yaml, "swamp_witch", "Die Sumpfhexe", "WITCH", 32, 7.0, 1.9, 1.30, 85, List.of("PROJECTILE_VOLLEY"), List.of("SWAMP", "MANGROVE_SWAMP"), "pixelrpg:boss/sumpftrank", 400, 900);
        boss(yaml, "husk_king", "Der Husk-König", "HUSK", 34, 7.5, 2.0, 1.30, 90, List.of("SLAM"), List.of("DESERT"), "pixelrpg:boss/husk_siegel", 450, 1000);
        boss(yaml, "ravager_chief", "Der Ravager-Häuptling", "RAVAGER", 38, 9.0, 2.2, 1.35, 85, List.of("SLAM"), List.of("SAVANNA", "SAVANNA_PLATEAU", "WINDSWEPT_SAVANNA"), "pixelrpg:boss/ravager_trophaee", 500, 1200);
        boss(yaml, "sandstone_colossus", "Der Sandstein-Koloss", "HUSK", 40, 10.0, 2.2, 1.40, 95, List.of("PROJECTILE_VOLLEY", "SLAM"), List.of("BADLANDS", "WOODED_BADLANDS", "ERODED_BADLANDS"), "pixelrpg:boss/goldenes_fossil", 550, 1300);
        boss(yaml, "frostwolf", "Der Frostwolf", "WOLF", 42, 10.0, 2.3, 1.35, 75, List.of("SLAM"), List.of("TAIGA", "OLD_GROWTH_PINE_TAIGA", "OLD_GROWTH_SPRUCE_TAIGA", "SNOWY_TAIGA"), "pixelrpg:boss/frostwolf_fang", 600, 1400);
        boss(yaml, "stray_warrior", "Der Streuner-Krieger", "STRAY", 45, 11.0, 2.4, 1.35, 80, List.of("PROJECTILE_VOLLEY"), List.of("SNOWY_PLAINS", "ICE_SPIKES"), "pixelrpg:boss/frostpfeil_koecher", 650, 1600);
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

        worldBoss(yaml, "rift_colossus", "Der Risskoloss", "RAVAGER", 80, 35.0, 5.0, 1.75, 2500, 6000, "pixelrpg:boss/risskern", "NETHER_STAR", "NETHERITE_INGOT", 15.0, "LEGENDARY");
        worldBoss(yaml, "storm_lord", "Der Sturmherrscher", "EVOKER", 84, 38.0, 4.8, 1.55, 3000, 7000, "pixelrpg:boss/sturmherz", "TOTEM_OF_UNDYING", "DIAMOND_BLOCK", 18.0, "LEGENDARY");
        worldBoss(yaml, "abyss_lord", "Der Abgrundfürst", "ELDER_GUARDIAN", 88, 42.0, 4.5, 1.55, 3400, 8000, "pixelrpg:boss/abgrundkern", "HEART_OF_THE_SEA", "SPONGE", 20.0, "EPIC");
        worldBoss(yaml, "soul_devourer", "Der Seelenverschlinger", "WITHER_SKELETON", 92, 45.0, 5.4, 1.50, 3800, 9000, "pixelrpg:boss/seelenkrone", "NETHER_STAR", "NETHERITE_SCRAP", 20.0, "LEGENDARY");
        worldBoss(yaml, "end_harbinger", "Der Endbote", "ENDERMAN", 96, 48.0, 5.2, 1.55, 4200, 10000, "pixelrpg:boss/endriss", "DRAGON_BREATH", "ENDER_EYE", 25.0, "LEGENDARY");
        worldBoss(yaml, "ancient_world_warden", "Der Uralte Weltenwächter", "WARDEN", 100, 55.0, 6.0, 1.65, 5000, 12000, "pixelrpg:boss/weltenherz", "NETHER_STAR", "ECHO_SHARD", 30.0, "LEGENDARY");

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

    private void worldBoss(YamlConfiguration yaml, String id, String name, String entity, int level,
                           double hp, double damage, double scale, double money, long exp, String customDrop,
                           String guaranteedMaterial, String chanceMaterial, double chancePercent, String rarity) {
        String path = "bosses." + id;
        yaml.set(path + ".name", name);
        yaml.set(path + ".kind", "WORLD_EVENT");
        yaml.set(path + ".base-entity", entity);
        yaml.set(path + ".level", level);
        yaml.set(path + ".health-multiplier", hp);
        yaml.set(path + ".damage-multiplier", damage);
        yaml.set(path + ".scale-multiplier", scale);
        yaml.set(path + ".loot.guaranteed", List.of(customDrop, guaranteedMaterial));
        yaml.set(path + ".loot.chance-drops", List.of(Map.of("material", chanceMaterial, "chance-percent", chancePercent, "rarity", rarity)));
        yaml.set(path + ".loot.money", money);
        yaml.set(path + ".loot.exp", exp);
        yaml.set(path + ".phases", worldBossPhases(id));
    }

    private List<Map<String, Object>> worldBossPhases(String id) {
        return switch (id) {
            case "rift_colossus" -> List.of(phase(100.0, 120, List.of("SLAM", "PROJECTILE_VOLLEY"), "Der Risskoloss erwacht."), phase(66.0, 95, List.of("SLAM", "SUMMON_ADDS", "PROJECTILE_VOLLEY"), "Der Riss reißt weiter auf."), phase(33.0, 70, List.of("ENRAGE_BUFF", "SLAM", "SUMMON_ADDS", "PROJECTILE_VOLLEY"), "Der Risskoloss entfesselt seine letzte Kraft."));
            case "storm_lord" -> List.of(phase(100.0, 110, List.of("PROJECTILE_VOLLEY", "SLAM"), "Der Sturmherrscher ruft den Sturm."), phase(66.0, 85, List.of("PROJECTILE_VOLLEY", "SUMMON_ADDS", "SLAM"), "Der Himmel bricht über dem Schlachtfeld auf."), phase(33.0, 60, List.of("ENRAGE_BUFF", "PROJECTILE_VOLLEY", "SUMMON_ADDS", "SLAM"), "Der Sturmherrscher rastet aus."));
            case "abyss_lord" -> List.of(phase(100.0, 115, List.of("PROJECTILE_VOLLEY", "SLAM"), "Der Abgrundfürst erhebt sich."), phase(66.0, 90, List.of("PROJECTILE_VOLLEY", "SUMMON_ADDS", "SLAM"), "Der Abgrund zieht alles in die Tiefe."), phase(33.0, 65, List.of("ENRAGE_BUFF", "PROJECTILE_VOLLEY", "SUMMON_ADDS"), "Der Abgrundfürst entfesselt seine letzte Welle."));
            case "soul_devourer" -> List.of(phase(100.0, 105, List.of("SLAM", "PROJECTILE_VOLLEY"), "Der Seelenverschlinger sammelt Seelen."), phase(66.0, 80, List.of("SUMMON_ADDS", "PROJECTILE_VOLLEY", "SLAM"), "Die gefallenen Seelen kehren zurück."), phase(33.0, 55, List.of("ENRAGE_BUFF", "SUMMON_ADDS", "PROJECTILE_VOLLEY", "SLAM"), "Der Seelenverschlinger ist außer Kontrolle."));
            case "end_harbinger" -> List.of(phase(100.0, 100, List.of("PROJECTILE_VOLLEY", "SLAM"), "Der Endbote durchbricht den Schleier."), phase(66.0, 75, List.of("SUMMON_ADDS", "PROJECTILE_VOLLEY", "SLAM"), "Der Endbote öffnet weitere Risse."), phase(33.0, 50, List.of("ENRAGE_BUFF", "PROJECTILE_VOLLEY", "SUMMON_ADDS", "SLAM"), "Der Endbote beginnt zu zerfallen."));
            case "ancient_world_warden" -> List.of(phase(100.0, 120, List.of("SLAM", "PROJECTILE_VOLLEY"), "Der Uralte Weltenwächter erwacht."), phase(66.0, 90, List.of("SLAM", "SUMMON_ADDS", "PROJECTILE_VOLLEY"), "Das Schlachtfeld erzittert."), phase(33.0, 55, List.of("ENRAGE_BUFF", "SLAM", "SUMMON_ADDS", "PROJECTILE_VOLLEY"), "Der Weltenwächter entfesselt seine letzte Macht."));
            default -> List.of();
        };
    }

    private Map<String, Object> phase(double healthPercent, int interval, List<String> patterns, String announcement) {
        return Map.of("health-percent", healthPercent, "attack-interval-ticks", interval, "patterns", patterns, "announcement", announcement);
    }

    private double toDouble(Object value) { return value instanceof Number number ? number.doubleValue() : 0.0D; }
    private int toInt(Object value, int fallback) { return value instanceof Number number ? number.intValue() : fallback; }
    private String stringValue(Object value) { return stringValue(value, ""); }
    private String stringValue(Object value, String fallback) { return value == null ? fallback : String.valueOf(value); }
    private EntityType parseEntityType(String raw) { try { return EntityType.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return EntityType.ZOMBIE; } }
    private BossKind parseKind(String raw) { try { return BossKind.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return BossKind.BIOME; } }
    private Biome parseBiome(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.equals("windswept_savanna_plateau")) normalized = "windswept_savanna";
        NamespacedKey key = NamespacedKey.fromString(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        if (key == null) return null;
        Biome biome = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(key);
        if (biome == null) plugin.getLogger().warning("Unknown boss biome: " + raw);
        return biome;
    }
    private ItemRarity parseRarity(String raw) { try { return ItemRarity.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return ItemRarity.RARE; } }
}
