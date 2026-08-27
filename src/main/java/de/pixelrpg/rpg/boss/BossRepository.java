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
        if (!file.exists()) plugin.saveResource("bosses.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        definitionsById.clear();
        ConfigurationSection section = config.getConfigurationSection("bosses");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection boss = section.getConfigurationSection(id);
            if (boss == null) continue;
            BossDefinition definition = new BossDefinition(id);
            definition.setDisplayName(stringValue(boss.get("display-name"), id));
            definition.setBaseEntityType(parseEntityType(stringValue(boss.get("entity"), "ZOMBIE")));
            definition.setKind(parseKind(stringValue(boss.get("kind"), "BIOME")));
            definition.setLevel(boss.getInt("level", 1));
            definition.setHealthMultiplier(toDouble(boss.get("health-multiplier")));
            definition.setDamageMultiplier(toDouble(boss.get("damage-multiplier")));
            definition.setBiomes(parseBiomes(boss.getStringList("biomes")));
            definition.setLoot(parseLoot(boss.getConfigurationSection("loot")));
            definition.setPhases(parsePhases(boss.getConfigurationSection("phases")));
            definitionsById.put(id.toLowerCase(java.util.Locale.ROOT), definition);
        }
    }

    public BossDefinition get(String id) {
        if (id == null) return null;
        return definitionsById.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public List<BossDefinition> all() {
        return List.copyOf(definitionsById.values());
    }

    private List<Biome> parseBiomes(List<String> raw) {
        List<Biome> biomes = new ArrayList<>();
        for (String value : raw) {
            Biome biome = parseBiome(value);
            if (biome != null) biomes.add(biome);
        }
        return biomes;
    }

    private List<BossLootConfig> parseLoot(ConfigurationSection section) {
        if (section == null) return List.of();
        List<BossLootConfig> result = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection loot = section.getConfigurationSection(key);
            if (loot == null) continue;
            List<String> guaranteed = loot.getStringList("guaranteed-materials");
            List<BossLootEntry> chance = new ArrayList<>();
            ConfigurationSection chanceSection = loot.getConfigurationSection("chance-drops");
            if (chanceSection != null) {
                for (String entryKey : chanceSection.getKeys(false)) {
                    ConfigurationSection entry = chanceSection.getConfigurationSection(entryKey);
                    if (entry == null) continue;
                    chance.add(new BossLootEntry(
                            stringValue(entry.get("material"), ""),
                            toDouble(entry.get("chance-percent")),
                            parseRarity(stringValue(entry.get("rarity"), "RARE"))));
                }
            }
            result.add(new BossLootConfig(guaranteed, chance));
        }
        return result;
    }

    private List<Map<String, Object>> parsePhases(ConfigurationSection section) {
        if (section == null) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection phase = section.getConfigurationSection(key);
            if (phase == null) continue;
            Map<String, Object> values = new HashMap<>();
            values.put("health-percent", phase.getDouble("health-percent", 100.0D));
            values.put("attack-interval-ticks", phase.getInt("attack-interval-ticks", 100));
            values.put("patterns", phase.getStringList("patterns"));
            values.put("announcement", phase.getString("announcement", ""));
            result.add(values);
        }
        return result;
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
        NamespacedKey key = NamespacedKey.fromString(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        if (key == null) return null;
        Biome biome = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(key);
        if (biome == null) plugin.getLogger().warning("Unknown boss biome: " + raw);
        return biome;
    }
    private ItemRarity parseRarity(String raw) { try { return ItemRarity.valueOf(raw.trim().toUpperCase()); } catch (IllegalArgumentException | NullPointerException e) { return ItemRarity.RARE; } }
}