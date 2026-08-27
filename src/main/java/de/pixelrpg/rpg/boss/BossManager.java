package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.api.events.BossDefeatedEvent;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.lang.LanguageManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class BossManager {
    private final Plugin plugin;
    private final BossAttackPatternRegistry patternRegistry;
    private final GuildAPI guildAPI;
    private final PartyAPI partyAPI;
    private final EconomyAPI economyAPI;
    private final MobScalingConfig mobScalingConfig;
    private final ItemService itemService;
    private final double barRadius;
    private final int barUpdateIntervalTicks;
    private final int phaseCheckIntervalTicks;
    private final LanguageManager lang;
    private final Map<UUID, ActiveBoss> activeBosses = new ConcurrentHashMap<>();

    public BossManager(Plugin plugin, BossAttackPatternRegistry patternRegistry, GuildAPI guildAPI,
                       PartyAPI partyAPI, EconomyAPI economyAPI, ItemService itemService,
                       MobScalingConfig mobScalingConfig, LanguageManager languageManager,
                       double barRadius, int barUpdateIntervalTicks, int phaseCheckIntervalTicks) {
        this.plugin = plugin;
        this.patternRegistry = patternRegistry;
        this.guildAPI = guildAPI;
        this.partyAPI = partyAPI;
        this.economyAPI = economyAPI;
        this.itemService = itemService;
        this.mobScalingConfig = mobScalingConfig;
        this.barRadius = Math.max(1.0D, barRadius);
        this.barUpdateIntervalTicks = Math.max(1, barUpdateIntervalTicks);
        this.phaseCheckIntervalTicks = Math.max(1, phaseCheckIntervalTicks);
        this.lang = languageManager;
        if (this.itemService == null) throw new IllegalArgumentException("itemService must not be null");
        if (this.lang == null) throw new IllegalArgumentException("languageManager must not be null");
        plugin.getServer().getPluginManager().registerEvents(new WorldBossProtectionListener(guildAPI), plugin);
    }

    public boolean isRegistered(UUID playerId) { return guildAPI.isRegistered(playerId); }

    public LivingEntity spawnWorldBoss(BossDefinition definition, Location location) {
        if (definition.getKind() != BossKind.WORLD_EVENT) throw new IllegalArgumentException("Not a world-event boss: " + definition.getId());
        return spawn(definition, location, true);
    }

    public LivingEntity spawnBiomeBoss(BossDefinition definition, Location location) {
        if (definition.getKind() != BossKind.BIOME) throw new IllegalArgumentException("Not a biome boss: " + definition.getId());
        return spawn(definition, location, false);
    }

    private LivingEntity spawn(BossDefinition definition, Location location, boolean worldBoss) {
        if (location.getWorld() == null) throw new IllegalArgumentException("Boss spawn location has no world");
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, definition.getBaseEntityType());
        entity.getPersistentDataContainer().set(RPGKeys.Boss.worldBossMarker(), PersistentDataType.BOOLEAN, worldBoss);
        attachPhaseController(entity, definition);
        return entity;
    }

    // Further boss runtime implementation remains unchanged below this point.
}
