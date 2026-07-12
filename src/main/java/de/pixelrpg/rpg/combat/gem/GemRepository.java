// src/main/java/de/pixelrpg/rpg/combat/gem/GemRepository.java
package de.pixelrpg.rpg.combat.gem;

import de.pixelrpg.rpg.player.PlayerClass;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public final class GemRepository {

    private final Plugin plugin;
    private final Map<String, ActiveSkillGemDefinition> activeGems = new HashMap<>();
    private final Map<String, PassiveGemDefinition> passiveGems = new HashMap<>();

    public GemRepository(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        activeGems.clear();
        passiveGems.clear();

        File activeFile = new File(plugin.getDataFolder(), "gems/active.yml");
        if (!activeFile.exists()) {
            DefaultGemData.writeActiveDefaults(activeFile);
        }
        loadActive(activeFile);

        File passiveFile = new File(plugin.getDataFolder(), "gems/passive.yml");
        if (!passiveFile.exists()) {
            DefaultGemData.writePassiveDefaults(passiveFile);
        }
        loadPassive(passiveFile);
    }

    private void loadActive(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("gems");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                activeGems.put(id, new ActiveSkillGemDefinition(
                        id,
                        s.getString("name", id),
                        s.getString("description", ""),
                        PlayerClass.valueOf(s.getString("class", "WARRIOR").toUpperCase()),
                        s.getLong("cooldown-ms", 8000L),
                        Material.valueOf(s.getString("icon", "STICK").toUpperCase()),
                        ActiveSkillActionType.valueOf(s.getString("action", "SINGLE_TARGET_STRIKE").toUpperCase()),
                        s.getDouble("damage-multiplier", 1.0),
                        s.getDouble("flat-base", 6.0),
                        s.getDouble("radius", 4.0),
                        s.getInt("effect-duration-ticks", 60),
                        s.getBoolean("burn", false),
                        s.getBoolean("slow", false),
                        s.getBoolean("root", false),
                        s.getBoolean("weaken", false),
                        s.getBoolean("lifesteal", false),
                        parseSound(s.getString("sound", "ENTITY_PLAYER_ATTACK_STRONG")),
                        parseParticle(s.getString("particle", "CRIT"))
                ));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid active gem definition '" + id + "': " + e.getMessage());
            }
        }
    }

    private void loadPassive(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("gems");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                passiveGems.put(id, new PassiveGemDefinition(
                        id,
                        s.getString("name", id),
                        s.getString("description", ""),
                        Material.valueOf(s.getString("icon", "AMETHYST_SHARD").toUpperCase()),
                        StatModifierType.valueOf(s.getString("modifier", "BONUS_DAMAGE").toUpperCase()),
                        s.getDouble("value", 1.0)
                ));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid passive gem definition '" + id + "': " + e.getMessage());
            }
        }
    }

    private Sound parseSound(String raw) {
        try {
            return Sound.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_PLAYER_ATTACK_STRONG;
        }
    }

    private Particle parseParticle(String raw) {
        try {
            return Particle.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.CRIT;
        }
    }

    public Optional<ActiveSkillGemDefinition> getActive(String id) {
        return Optional.ofNullable(activeGems.get(id));
    }

    public Optional<PassiveGemDefinition> getPassive(String id) {
        return Optional.ofNullable(passiveGems.get(id));
    }

    public boolean isActiveGem(String id) {
        return activeGems.containsKey(id);
    }

    public boolean isPassiveGem(String id) {
        return passiveGems.containsKey(id);
    }

    public java.util.List<ActiveSkillGemDefinition> getAllActive() {
        return new java.util.ArrayList<>(activeGems.values());
    }

    public java.util.List<PassiveGemDefinition> getAllPassive() {
        return new java.util.ArrayList<>(passiveGems.values());
    }
}