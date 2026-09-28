package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.npc.ProfessionNpcRank;
import de.pixelrpg.rpg.profession.Profession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent Season-1 city progression with hidden next-level requirements and cooldowns. */
public final class CityProgressionService {
    private final PixelRPGPlugin plugin;
    private final GuildManager guilds;
    private final File file;
    private final Map<UUID, Map<Material, Integer>> delivered = new HashMap<>();

    public CityProgressionService(PixelRPGPlugin plugin, GuildManager guilds) {
        this.plugin = plugin;
        this.guilds = guilds;
        this.file = new File(plugin.getDataFolder(), "kingdom-city-progress.yml");
        load();
    }

    public synchronized CityView view(UUID guildId) {
        Guild guild = guilds.getGuildById(guildId).orElse(null);
        if (guild == null) return null;
        int level = guild.cityLevel();
        if (level >= Guild.MAX_CITY_LEVEL) {
            return new CityView(level, Guild.cityName(level), level, Guild.cityName(level),
                    guild.cityUpgradeCooldownUntil(), 0L, 0L, 0L, Map.of(), Map.of(), Map.of(), Map.of(), false);
        }
        int next = level + 1;
        Requirements requirements = requirements(next);
        Map<Material,Integer> current = delivered.getOrDefault(guildId, Map.of());
        Map<Material,Integer> progress = new LinkedHashMap<>();
        requirements.materials().forEach((m,a) -> progress.put(m, Math.min(a, current.getOrDefault(m, 0))));
        long remainingGold = Math.max(0L, requirements.goldMinorUnits() - guild.treasuryMinorUnits());
        Map<String,Integer> objectiveProgress=new LinkedHashMap<>(); requirements.objectives().forEach((key,required)->objectiveProgress.put(key,objectiveProgress(guild,key)));
        return new CityView(level, Guild.cityName(level), next, Guild.cityName(next), guild.cityUpgradeCooldownUntil(), requirements.cooldownMillis(),
                requirements.goldMinorUnits(), remainingGold, requirements.materials(), progress, requirements.objectives(), objectiveProgress, canUpgrade(guild));
    }

    public synchronized boolean contribute(Player player, Material material, int amount) {
        if (player == null || material == null || amount <= 0) return false;
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null || guild.cityLevel() >= Guild.MAX_CITY_LEVEL) return false;
        int required = requirements(guild.cityLevel() + 1).materials().getOrDefault(material, 0);
        if (required <= 0) return false;
        Map<Material,Integer> map = delivered.computeIfAbsent(guild.id(), ignored -> new HashMap<>());
        int already = map.getOrDefault(material, 0);
        int accepted = Math.min(amount, required - already);
        if (accepted <= 0 || !player.getInventory().contains(material, accepted)) return false;
        player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(material, accepted));
        map.put(material, already + accepted);
        save();
        return true;
    }

    public synchronized Result upgrade(Player player) {
        if (player == null) return Result.FAILURE;
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null || !guild.canManageTerritory(player.getUniqueId())) return Result.NOT_AUTHORIZED;
        if (guild.cityLevel() >= Guild.MAX_CITY_LEVEL) return Result.MAX_LEVEL;
        if (!canUpgrade(guild)) return Result.REQUIREMENTS_OR_COOLDOWN;
        Requirements req = requirements(guild.cityLevel() + 1);
        if (!guilds.chargeTreasury(guild.id(), req.goldMinorUnits())) return Result.INSUFFICIENT_GOLD;
        guilds.advanceCityLevel(guild.id(), guild.cityLevel() + 1, System.currentTimeMillis() + req.cooldownMillis());
        delivered.remove(guild.id());
        save();
        player.sendMessage(Component.text("Königreich auf Stadtlevel " + guild.cityLevel() + " – " + Guild.cityName(guild.cityLevel()) + " aufgestiegen.", NamedTextColor.GREEN));
        return Result.SUCCESS;
    }

    public synchronized boolean canUpgrade(Guild guild) {
        if (guild == null || guild.cityLevel() >= Guild.MAX_CITY_LEVEL) return false;
        if (System.currentTimeMillis() < guild.cityUpgradeCooldownUntil()) return false;
        Requirements req = requirements(guild.cityLevel() + 1);
        if (guild.treasuryMinorUnits() < req.goldMinorUnits()) return false;
        Map<Material,Integer> current = delivered.getOrDefault(guild.id(), Map.of());
        return req.materials().entrySet().stream().allMatch(e -> current.getOrDefault(e.getKey(), 0) >= e.getValue()) && req.objectives().entrySet().stream().allMatch(e -> objectiveProgress(guild,e.getKey()) >= e.getValue());
    }

    public Requirements requirements(int targetLevel) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("kingdom.city-levels." + targetLevel);
        if (root == null) throw new IllegalStateException("Missing Season-1 city requirement for level " + targetLevel);
        long gold = Math.max(0L, Math.round(root.getDouble("gold", 0.0D) * 100.0D));
        long cooldown = Math.max(0L, root.getLong("cooldown-hours", 24L)) * 3_600_000L;
        Map<Material,Integer> materials = new LinkedHashMap<>();
        ConfigurationSection section = root.getConfigurationSection("materials");
        if (section != null) for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null || material.isAir()) throw new IllegalStateException("Unknown city requirement material: " + key);
            int amount = section.getInt(key, 0);
            if (amount > 0) materials.put(material, amount);
        }
        Map<String,Integer> objectives=new LinkedHashMap<>(); ConfigurationSection objectiveSection=root.getConfigurationSection("objectives"); if(objectiveSection!=null) for(String key:objectiveSection.getKeys(false)){int amount=objectiveSection.getInt(key,0);if(amount>0)objectives.put(key,amount);}
        return new Requirements(gold, cooldown, Map.copyOf(materials), Map.copyOf(objectives));
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("guilds");
        if (root == null) return;
        for (String guildText : root.getKeys(false)) try {
            UUID guildId = UUID.fromString(guildText);
            ConfigurationSection section = root.getConfigurationSection(guildText + ".materials");
            if (section == null) continue;
            Map<Material,Integer> map = new HashMap<>();
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material != null) map.put(material, Math.max(0, section.getInt(key)));
            }
            delivered.put(guildId, map);
        } catch (IllegalArgumentException ignored) { }
    }

    private synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (var entry : delivered.entrySet()) for (var material : entry.getValue().entrySet()) yaml.set("guilds." + entry.getKey() + ".materials." + material.getKey().name(), material.getValue());
        try {
            Path target=file.toPath(), temp=target.resolveSibling(file.getName()+".tmp");
            yaml.save(temp.toFile());
            try { Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING); }
        } catch(IOException e) { plugin.getLogger().warning("Could not save kingdom city progress: "+e.getMessage()); }
    }

    private int objectiveProgress(Guild guild,String key) {
        if(key.equalsIgnoreCase("members")) return guild.memberCount();
        String[] parts=key.split(":"); if(parts.length==3&&parts[0].equalsIgnoreCase("profession-npc")) {
            try { Profession profession=Profession.valueOf(parts[1].toUpperCase(java.util.Locale.ROOT)); ProfessionNpcRank rank=ProfessionNpcRank.valueOf(parts[2].toUpperCase(java.util.Locale.ROOT)); return (int)plugin.getNpcManager().getAll().stream().filter(n->guild.id().equals(n.kingdomId())&&n.profession()==profession&&n.professionNpcRank().ordinal()>=rank.ordinal()).count(); }
            catch(IllegalArgumentException ignored){ return 0; }
        }
        return 0;
    }

    public record Requirements(long goldMinorUnits, long cooldownMillis, Map<Material,Integer> materials, Map<String,Integer> objectives) { }
    public record CityView(int level,String levelName,int nextLevel,String nextLevelName,long cooldownUntil,long cooldownMillis,long requiredGoldMinorUnits,long remainingGoldMinorUnits,Map<Material,Integer> requiredMaterials,Map<Material,Integer> deliveredMaterials,Map<String,Integer> requiredObjectives,Map<String,Integer> objectiveProgress,boolean upgradeReady) { }
    public enum Result { SUCCESS, FAILURE, NOT_AUTHORIZED, MAX_LEVEL, REQUIREMENTS_OR_COOLDOWN, INSUFFICIENT_GOLD }
}
