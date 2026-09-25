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
        changed |= migrateCustomLoot(yaml);
        changed |= migrateProgressionLevels(yaml);
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
                    if (material.isBlank() || material.trim().toLowerCase(java.util.Locale.ROOT).startsWith("pixelrpg:")) continue;
                    chanceDrops.add(new BossLootEntry(material, toDouble(entry.get("chance-percent")), parseRarity(stringValue(entry.get("rarity"), "RARE"))));
                }
                definition.setLootConfig(new BossLootConfig(lootSection.getStringList("guaranteed").stream()
                        .filter(value -> !value.trim().toLowerCase(java.util.Locale.ROOT).startsWith("pixelrpg:")).toList(), chanceDrops,
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

    private boolean migrateCustomLoot(YamlConfiguration yaml) {
        boolean changed = false;
        ConfigurationSection bosses = yaml.getConfigurationSection("bosses");
        if (bosses == null) return false;

        for (String id : bosses.getKeys(false)) {
            String path = "bosses." + id + ".loot";
            if (!yaml.isConfigurationSection(path)) continue;

            List<String> guaranteed = yaml.getStringList(path + ".guaranteed");
            List<String> filteredGuaranteed = guaranteed.stream()
                    .filter(value -> !value.trim().toLowerCase(java.util.Locale.ROOT).startsWith("pixelrpg:"))
                    .toList();
            if (!filteredGuaranteed.equals(guaranteed)) {
                yaml.set(path + ".guaranteed", filteredGuaranteed);
                changed = true;
            }

            List<?> chanceDrops = yaml.getList(path + ".chance-drops", List.of());
            List<?> filteredChanceDrops = chanceDrops.stream()
                    .filter(value -> !(value instanceof Map<?, ?> map
                            && String.valueOf(map.containsKey("material") ? map.get("material") : "").trim()
                            .toLowerCase(java.util.Locale.ROOT).startsWith("pixelrpg:")))
                    .toList();
            if (filteredChanceDrops.size() != chanceDrops.size()) {
                yaml.set(path + ".chance-drops", filteredChanceDrops);
                changed = true;
            }
        }
        return changed;
    }

    private boolean migrateProgressionLevels(YamlConfiguration yaml) {
        Map<String, Integer> biomeLevels = Map.ofEntries(
                Map.entry("plunderer", 1), Map.entry("bee_queen", 4), Map.entry("forest_witch", 6),
                Map.entry("creaking_heart", 9), Map.entry("jungle_warden", 11), Map.entry("swamp_witch", 14),
                Map.entry("husk_king", 16), Map.entry("ravager_chief", 19), Map.entry("sandstone_colossus", 21),
                Map.entry("frostwolf", 24), Map.entry("stray_warrior", 26), Map.entry("mountain_goat", 29),
                Map.entry("wild_goat", 31), Map.entry("blossom_warden", 34), Map.entry("drowned_captain", 36),
                Map.entry("river_warden", 39), Map.entry("guardian_of_depths", 41), Map.entry("mycelium_king", 44),
                Map.entry("cave_hunter", 46), Map.entry("ancient_warden", 49), Map.entry("nether_lord", 51),
                Map.entry("crimson_beast", 54), Map.entry("enderman_lord", 56), Map.entry("soul_lord", 58),
                Map.entry("magma_colossus", 59), Map.entry("end_king", 60)
        );
        Map<String, Integer> worldLevels = Map.of(
                "rift_colossus", 40, "storm_lord", 44, "abyss_lord", 48,
                "soul_devourer", 52, "end_harbinger", 56, "ancient_world_warden", 60
        );
        boolean changed = false;
        for (Map.Entry<String, Integer> entry : biomeLevels.entrySet()) {
            changed |= migrateBossScaling(yaml, entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<String, Integer> entry : worldLevels.entrySet()) {
            changed |= migrateBossScaling(yaml, entry.getKey(), entry.getValue(), true);
        }
        return changed;
    }

    private boolean migrateBossScaling(YamlConfiguration yaml, String id, int level, boolean worldBoss) {
        String root = "bosses." + id;
        if (!yaml.isConfigurationSection(root)) return false;

        boolean changed = false;
        if (yaml.getInt(root + ".level") != level) {
            yaml.set(root + ".level", level);
            changed = true;
        }

        double healthMultiplier = worldBoss
                ? 2.8D + Math.max(0, level - 40) * (1.2D / 20.0D)
                : 2.0D + Math.max(0, level - 1) * (2.0D / 59.0D);
        double damageMultiplier = worldBoss
                ? 2.2D + Math.max(0, level - 40) * (0.5D / 20.0D)
                : 1.4D + Math.max(0, level - 1) * (1.1D / 59.0D);

        if (Math.abs(yaml.getDouble(root + ".health-multiplier") - healthMultiplier) > 0.001D) {
            yaml.set(root + ".health-multiplier", healthMultiplier);
            changed = true;
        }
        if (Math.abs(yaml.getDouble(root + ".damage-multiplier") - damageMultiplier) > 0.001D) {
            yaml.set(root + ".damage-multiplier", damageMultiplier);
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
        boss(yaml, "plunderer", "Der Plünderer", "PILLAGER", 1, 2.000, 1.400, 1.15, 100, List.of("PROJECTILE_VOLLEY"), List.of("PLAINS", "SUNFLOWER_PLAINS"), "pixelrpg:boss/pluenderer_siegel", 150, 300);
        boss(yaml, "bee_queen", "Die Bienenkönigin", "BEE", 4, 2.102, 1.456, 1.20, 80, List.of("SLAM"), List.of("FOREST", "FLOWER_FOREST", "BIRCH_FOREST", "OLD_GROWTH_BIRCH_FOREST"), "pixelrpg:boss/bienenkoenigin", 200, 400);
        boss(yaml, "forest_witch", "Die Hexe des Waldes", "WITCH", 6, 2.169, 1.493, 1.20, 90, List.of("PROJECTILE_VOLLEY"), List.of("DARK_FOREST"), "pixelrpg:boss/hexenkessel", 250, 500);
        boss(yaml, "creaking_heart", "Das Knarzende Herz", "CREAKING", 9, 2.271, 1.549, 1.25, 90, List.of("SLAM"), List.of("PALE_GARDEN"), "pixelrpg:boss/knarzendes_herzstueck", 300, 650);
        boss(yaml, "jungle_warden", "Der Dschungelwächter", "PANDA", 11, 2.339, 1.586, 1.30, 90, List.of("SLAM"), List.of("JUNGLE", "SPARSE_JUNGLE", "BAMBOO_JUNGLE"), "pixelrpg:boss/dschungel_amulett", 350, 800);
        boss(yaml, "swamp_witch", "Die Sumpfhexe", "WITCH", 14, 2.441, 1.642, 1.30, 85, List.of("PROJECTILE_VOLLEY"), List.of("SWAMP", "MANGROVE_SWAMP"), "pixelrpg:boss/sumpftrank", 400, 900);
        boss(yaml, "husk_king", "Der Husk-König", "HUSK", 16, 2.508, 1.680, 1.30, 90, List.of("SLAM"), List.of("DESERT"), "pixelrpg:boss/husk_siegel", 450, 1000);
        boss(yaml, "ravager_chief", "Der Ravager-Häuptling", "RAVAGER", 19, 2.610, 1.736, 1.35, 85, List.of("SLAM"), List.of("SAVANNA", "SAVANNA_PLATEAU", "WINDSWEPT_SAVANNA"), "pixelrpg:boss/ravager_trophaee", 500, 1200);
        boss(yaml, "sandstone_colossus", "Der Sandstein-Koloss", "HUSK", 21, 2.678, 1.773, 1.40, 95, List.of("PROJECTILE_VOLLEY", "SLAM"), List.of("BADLANDS", "WOODED_BADLANDS", "ERODED_BADLANDS"), "pixelrpg:boss/goldenes_fossil", 550, 1300);
        boss(yaml, "frostwolf", "Der Frostwolf", "WOLF", 24, 2.780, 1.829, 1.35, 75, List.of("SLAM"), List.of("TAIGA", "OLD_GROWTH_PINE_TAIGA", "OLD_GROWTH_SPRUCE_TAIGA", "SNOWY_TAIGA"), "pixelrpg:boss/frostwolf_fang", 600, 1400);
        boss(yaml, "stray_warrior", "Der Streuner-Krieger", "STRAY", 26, 2.847, 1.866, 1.35, 80, List.of("PROJECTILE_VOLLEY"), List.of("SNOWY_PLAINS", "ICE_SPIKES"), "pixelrpg:boss/frostpfeil_koecher", 650, 1600);
        boss(yaml, "mountain_goat", "Der Bergbock", "GOAT", 29, 2.949, 1.922, 1.40, 75, List.of("SLAM"), List.of("JAGGED_PEAKS", "FROZEN_PEAKS", "STONY_PEAKS"), "pixelrpg:boss/horn_des_berges", 700, 1800);
        boss(yaml, "wild_goat", "Der wilde Bergbock", "GOAT", 31, 3.017, 1.959, 1.35, 75, List.of("SLAM"), List.of("MEADOW"), "pixelrpg:boss/wildhorn", 650, 1500);
        boss(yaml, "blossom_warden", "Der Blütenwächter", "BEE", 34, 3.119, 2.015, 1.35, 80, List.of("SLAM"), List.of("CHERRY_GROVE"), "pixelrpg:boss/bluetenhonig", 800, 2000);
        boss(yaml, "drowned_captain", "Der Ertrunkene Kapitän", "DROWNED", 36, 3.186, 2.053, 1.35, 80, List.of("PROJECTILE_VOLLEY", "SLAM"), List.of("BEACH", "SNOWY_BEACH", "STONY_SHORE"), "pixelrpg:boss/kapitaens_nautilus", 850, 2200);
        boss(yaml, "river_warden", "Der Flusswächter", "DROWNED", 39, 3.288, 2.108, 1.35, 80, List.of("PROJECTILE_VOLLEY"), List.of("RIVER", "FROZEN_RIVER"), "pixelrpg:boss/flusskiesel", 850, 2100);
        boss(yaml, "guardian_of_depths", "Der Tiefenwächter", "GUARDIAN", 41, 3.356, 2.146, 1.40, 90, List.of("PROJECTILE_VOLLEY"), List.of("OCEAN", "DEEP_OCEAN", "COLD_OCEAN", "DEEP_COLD_OCEAN", "LUKEWARM_OCEAN", "DEEP_LUKEWARM_OCEAN", "WARM_OCEAN", "FROZEN_OCEAN", "DEEP_FROZEN_OCEAN"), "pixelrpg:boss/auge_der_tiefe", 1000, 2600);
        boss(yaml, "mycelium_king", "Der Myzelkönig", "MOOSHROOM", 44, 3.458, 2.202, 1.35, 85, List.of("SLAM"), List.of("MUSHROOM_FIELDS"), "pixelrpg:boss/myzelkern", 900, 2400);
        boss(yaml, "cave_hunter", "Der Höhlenjäger", "SPIDER", 46, 3.525, 2.239, 1.30, 75, List.of("SLAM"), List.of("DRIPSTONE_CAVES", "LUSH_CAVES"), "pixelrpg:boss/spinnenauge_des_jaegers", 750, 1900);
        boss(yaml, "ancient_warden", "Der Uralte Wächter", "WARDEN", 49, 3.627, 2.295, 1.45, 100, List.of("SLAM"), List.of("DEEP_DARK"), "pixelrpg:boss/echoherz", 1200, 3500);
        boss(yaml, "nether_lord", "Der Netherfürst", "PIGLIN_BRUTE", 51, 3.695, 2.332, 1.45, 80, List.of("SLAM", "PROJECTILE_VOLLEY"), List.of("NETHER_WASTES"), "pixelrpg:boss/netherkern", 1300, 3800);
        boss(yaml, "crimson_beast", "Die Karmesinbestie", "HOGLIN", 54, 3.797, 2.388, 1.40, 75, List.of("SLAM"), List.of("CRIMSON_FOREST"), "pixelrpg:boss/karmesinherz", 1200, 3300);
        boss(yaml, "enderman_lord", "Der Endermanfürst", "ENDERMAN", 56, 3.864, 2.425, 1.45, 80, List.of("SLAM", "PROJECTILE_VOLLEY"), List.of("WARPED_FOREST"), "pixelrpg:boss/gebundene_enderperle", 1400, 4000);
        boss(yaml, "soul_lord", "Der Seelenfürst", "WITHER_SKELETON", 58, 3.932, 2.463, 1.45, 80, List.of("PROJECTILE_VOLLEY", "SLAM"), List.of("SOUL_SAND_VALLEY"), "pixelrpg:boss/seelenfragment", 1400, 3900);
        boss(yaml, "magma_colossus", "Der Magmakoloss", "MAGMA_CUBE", 59, 3.966, 2.481, 1.45, 70, List.of("SLAM"), List.of("BASALT_DELTAS"), "pixelrpg:boss/magmaherz", 1350, 3700);
        boss(yaml, "end_king", "Der Endkönig", "SHULKER", 60, 4.000, 2.500, 1.50, 85, List.of("PROJECTILE_VOLLEY", "SLAM"), List.of("THE_END", "END_HIGHLANDS", "END_MIDLANDS", "SMALL_END_ISLANDS", "END_BARRENS"), "pixelrpg:boss/shulkerkern", 1800, 5000);

        worldBoss(yaml, "rift_colossus", "Der Risskoloss", "RAVAGER", 40, 2.800, 2.200, 1.75, 2500, 6000, "pixelrpg:boss/risskern", "NETHER_STAR", "NETHERITE_INGOT", 15.0, "LEGENDARY");
        worldBoss(yaml, "storm_lord", "Der Sturmherrscher", "EVOKER", 44, 3.040, 2.300, 1.55, 3000, 7000, "pixelrpg:boss/sturmherz", "TOTEM_OF_UNDYING", "DIAMOND_BLOCK", 18.0, "LEGENDARY");
        worldBoss(yaml, "abyss_lord", "Der Abgrundfürst", "ELDER_GUARDIAN", 48, 3.280, 2.400, 1.55, 3400, 8000, "pixelrpg:boss/abgrundkern", "HEART_OF_THE_SEA", "SPONGE", 20.0, "EPIC");
        worldBoss(yaml, "soul_devourer", "Der Seelenverschlinger", "WITHER_SKELETON", 52, 3.520, 2.500, 1.50, 3800, 9000, "pixelrpg:boss/seelenkrone", "NETHER_STAR", "NETHERITE_SCRAP", 20.0, "LEGENDARY");
        worldBoss(yaml, "end_harbinger", "Der Endbote", "ENDERMAN", 56, 3.760, 2.600, 1.55, 4200, 10000, "pixelrpg:boss/endriss", "DRAGON_BREATH", "ENDER_EYE", 25.0, "LEGENDARY");
        worldBoss(yaml, "ancient_world_warden", "Der Uralte Weltenwächter", "WARDEN", 60, 4.000, 2.700, 1.65, 5000, 12000, "pixelrpg:boss/weltenherz", "NETHER_STAR", "ECHO_SHARD", 30.0, "LEGENDARY");

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
        yaml.set(path + ".loot.guaranteed", List.of());
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
        yaml.set(path + ".loot.guaranteed", List.of(guaranteedMaterial));
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
