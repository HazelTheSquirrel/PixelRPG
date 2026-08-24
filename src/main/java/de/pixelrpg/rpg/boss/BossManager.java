package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.api.events.BossDefeatedEvent;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.lang.LanguageManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
    private final ItemEconomyConfig itemEconomyConfig;
    private final MobScalingConfig mobScalingConfig;
    private final double barRadius;
    private final int barUpdateIntervalTicks;
    private final int phaseCheckIntervalTicks;
    private final LanguageManager lang;
    private final Map<UUID, ActiveBoss> activeBosses = new ConcurrentHashMap<>();

    public BossManager(Plugin plugin, BossAttackPatternRegistry patternRegistry, GuildAPI guildAPI,
                       PartyAPI partyAPI, EconomyAPI economyAPI, ItemEconomyConfig itemEconomyConfig,
                       MobScalingConfig mobScalingConfig, double barRadius, int barUpdateIntervalTicks,
                       int phaseCheckIntervalTicks) {
        this.plugin = plugin;
        this.patternRegistry = patternRegistry;
        this.guildAPI = guildAPI;
        this.partyAPI = partyAPI;
        this.economyAPI = economyAPI;
        this.itemEconomyConfig = itemEconomyConfig;
        this.mobScalingConfig = mobScalingConfig;
        this.barRadius = barRadius;
        this.barUpdateIntervalTicks = Math.max(1, barUpdateIntervalTicks);
        this.phaseCheckIntervalTicks = Math.max(1, phaseCheckIntervalTicks);
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    public LivingEntity spawnWorldBoss(BossDefinition definition, Location location) {
        if (definition.getKind() != BossKind.WORLD_EVENT) {
            throw new IllegalArgumentException("Boss definition is not a world-event boss: " + definition.getId());
        }
        LivingEntity entity = spawn(definition, location, true);
        return entity;
    }

    public LivingEntity spawnBiomeBoss(BossDefinition definition, Location location) {
        if (definition.getKind() != BossKind.BIOME) {
            throw new IllegalArgumentException("Boss definition is not a biome boss: " + definition.getId());
        }
        return spawn(definition, location, false);
    }

    private LivingEntity spawn(BossDefinition definition, Location location, boolean worldBoss) {
        if (location.getWorld() == null) throw new IllegalArgumentException("Boss spawn location has no world");
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, definition.getBaseEntityType());
        entity.getPersistentDataContainer().set(RPGKeys.Boss.worldBossMarker(), PersistentDataType.BOOLEAN, worldBoss);
        attachPhaseController(entity, definition);
        return entity;
    }

    public void attachPhaseController(LivingEntity entity, BossDefinition definition) {
        applyBaseStats(entity, definition);
        entity.getPersistentDataContainer().set(RPGKeys.Boss.bossId(), PersistentDataType.STRING, definition.getId());
        entity.getPersistentDataContainer().set(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER, definition.getLevel());
        entity.customName(Component.text(definition.getDisplayName(), NamedTextColor.DARK_RED));
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);
        BossBar bossBar = BossBar.bossBar(Component.text(definition.getDisplayName(), NamedTextColor.DARK_RED), 1.0f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        ActiveBoss activeBoss = new ActiveBoss(entity.getUniqueId(), definition, bossBar);
        activeBosses.put(entity.getUniqueId(), activeBoss);
        activeBoss.setTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(activeBoss), 0L, phaseCheckIntervalTicks));
    }

    private void applyBaseStats(LivingEntity entity, BossDefinition definition) {
        MobScalingConfig.LevelBaseStats levelStats = mobScalingConfig.getBaseStats(definition.getLevel());
        double parityMultiplier = mobScalingConfig.getPlayerParityMultiplier();
        double newHp = levelStats.hp() * parityMultiplier * definition.getHealthMultiplier();
        double newDamage = levelStats.damage() * parityMultiplier * definition.getDamageMultiplier();

        AttributeInstance hpAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (hpAttribute != null) {
            hpAttribute.setBaseValue(newHp);
            entity.setHealth(newHp);
        }
        AttributeInstance dmgAttribute = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttribute != null) dmgAttribute.setBaseValue(newDamage);
        AttributeInstance scaleAttribute = entity.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) scaleAttribute.setBaseValue(scaleAttribute.getBaseValue() * definition.getScaleMultiplier());
    }

    private void tick(ActiveBoss activeBoss) {
        LivingEntity entity = (LivingEntity) Bukkit.getEntity(activeBoss.getEntityUuid());
        if (entity == null || entity.isDead() || !entity.isValid()) {
            cleanup(activeBoss);
            return;
        }

        activeBoss.incrementBarUpdateTimer(phaseCheckIntervalTicks);
        if (activeBoss.getTicksSinceLastBarUpdate() >= barUpdateIntervalTicks) {
            activeBoss.resetBarUpdateTimer();
            updateBossBar(activeBoss, entity);
        }

        if (activeBoss.getDefinition().getKind() == BossKind.WORLD_EVENT) {
            checkPhaseTransition(activeBoss, entity);
        }
        runAttackPatternIfDue(activeBoss, entity);
    }

    private void updateBossBar(ActiveBoss activeBoss, LivingEntity entity) {
        AttributeInstance hpAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = hpAttribute != null ? hpAttribute.getValue() : 20.0;
        activeBoss.getBossBar().progress((float) Math.max(0.0, Math.min(1.0, entity.getHealth() / maxHp)));
        Location location = entity.getLocation();

        for (Player player : location.getWorld().getPlayers()) {
            UUID uuid = player.getUniqueId();
            boolean isViewer = activeBoss.getViewers().contains(uuid);
            if (!guildAPI.isRegistered(uuid)) {
                if (isViewer) {
                    player.hideBossBar(activeBoss.getBossBar());
                    activeBoss.getViewers().remove(uuid);
                }
                continue;
            }

            boolean inRange = player.getLocation().distanceSquared(location) <= barRadius * barRadius;
            if (inRange && !isViewer) {
                player.showBossBar(activeBoss.getBossBar());
                activeBoss.getViewers().add(uuid);
            } else if (!inRange && isViewer) {
                player.hideBossBar(activeBoss.getBossBar());
                activeBoss.getViewers().remove(uuid);
            }
        }
    }

    private void checkPhaseTransition(ActiveBoss activeBoss, LivingEntity entity) {
        AttributeInstance hpAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = hpAttribute != null ? hpAttribute.getValue() : 20.0;
        double healthPercent = entity.getHealth() / maxHp * 100.0;
        List<BossPhase> phases = activeBoss.getDefinition().getPhases();
        if (phases.isEmpty()) return;

        int targetIndex = 0;
        for (int i = 0; i < phases.size(); i++) {
            if (healthPercent <= phases.get(i).healthPercentageThreshold()) targetIndex = i;
        }
        if (targetIndex == activeBoss.getCurrentPhaseIndex()) return;

        activeBoss.setCurrentPhaseIndex(targetIndex);
        BossPhase phase = phases.get(targetIndex);
        if (!phase.announcementMessage().isBlank()) {
            Component message = Component.text(phase.announcementMessage(), NamedTextColor.DARK_RED);
            for (UUID viewerUuid : activeBoss.getViewers()) {
                Player viewer = Bukkit.getPlayer(viewerUuid);
                if (viewer != null && guildAPI.isRegistered(viewerUuid)) viewer.sendMessage(message);
            }
        }
    }

    private void runAttackPatternIfDue(ActiveBoss activeBoss, LivingEntity entity) {
        BossDefinition definition = activeBoss.getDefinition();
        List<String> patterns;
        int interval;

        if (definition.getKind() == BossKind.WORLD_EVENT) {
            List<BossPhase> phases = definition.getPhases();
            if (phases.isEmpty() || activeBoss.getCurrentPhaseIndex() < 0) return;
            BossPhase phase = phases.get(activeBoss.getCurrentPhaseIndex());
            patterns = phase.attackPatternIds();
            interval = phase.attackIntervalTicks();
        } else {
            patterns = definition.getAttackPatternIds();
            interval = definition.getAttackIntervalTicks();
        }

        if (patterns.isEmpty()) return;
        activeBoss.incrementAttackTimer(phaseCheckIntervalTicks);
        if (activeBoss.getTicksSinceLastAttack() < interval) return;
        activeBoss.resetAttackTimer();

        List<Player> targets = new ArrayList<>();
        for (UUID viewerUuid : activeBoss.getViewers()) {
            Player player = Bukkit.getPlayer(viewerUuid);
            if (player != null && player.isOnline() && guildAPI.isRegistered(viewerUuid)) targets.add(player);
        }
        if (targets.isEmpty()) return;

        String patternId = patterns.get(ThreadLocalRandom.current().nextInt(patterns.size()));
        patternRegistry.get(patternId).ifPresent(pattern -> pattern.execute(plugin, entity, targets));
    }

    public void recordDamage(UUID bossEntityUuid, UUID playerUuid, double damage) {
        if (!guildAPI.isRegistered(playerUuid) || damage <= 0.0) return;
        ActiveBoss activeBoss = activeBosses.get(bossEntityUuid);
        if (activeBoss != null) activeBoss.recordDamage(playerUuid, damage);
    }

    public void onBossDeath(LivingEntity entity) {
        ActiveBoss activeBoss = activeBosses.get(entity.getUniqueId());
        if (activeBoss == null) return;
        cleanup(activeBoss);
        distributeRewards(activeBoss);
    }

    private void distributeRewards(ActiveBoss activeBoss) {
        BossLootConfig lootConfig = activeBoss.getDefinition().getLootConfig();
        if (lootConfig == null) {
            callDefeatedEvent(activeBoss, Set.of());
            return;
        }

        Set<UUID> participants = new HashSet<>(activeBoss.getDamageContribution().keySet());
        Set<UUID> recipients = activeBoss.getDefinition().getKind() == BossKind.WORLD_EVENT
                ? onlineRegistered(participants)
                : resolvePartyRecipients(participants);

        for (UUID uuid : recipients) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || !guildAPI.isRegistered(uuid)) continue;

            economyAPI.deposit(uuid, lootConfig.moneyReward());
            guildAPI.addExperience(uuid, lootConfig.expReward());
            giveGuaranteedLoot(player, lootConfig);
            giveChanceLoot(player, lootConfig);
            player.sendMessage(lang.get("boss.defeated-reward", "money", String.valueOf(lootConfig.moneyReward()), "exp", String.valueOf(lootConfig.expReward())));
        }

        callDefeatedEvent(activeBoss, onlineRegistered(participants));
    }

    private Set<UUID> resolvePartyRecipients(Set<UUID> participants) {
        Set<UUID> recipients = new HashSet<>();
        for (UUID participant : participants) {
            if (!partyAPI.isInParty(participant)) {
                if (Bukkit.getPlayer(participant) != null) recipients.add(participant);
                continue;
            }
            for (UUID member : partyAPI.getPartyMembers(participant)) {
                if (!partyAPI.isWithinShareRange(participant, member)) continue;
                Player player = Bukkit.getPlayer(member);
                if (player != null && player.isOnline() && guildAPI.isRegistered(member)) recipients.add(member);
            }
        }
        return recipients;
    }

    private Set<UUID> onlineRegistered(Set<UUID> uuids) {
        Set<UUID> result = new HashSet<>();
        for (UUID uuid : uuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && guildAPI.isRegistered(uuid)) result.add(uuid);
        }
        return result;
    }

    private void giveGuaranteedLoot(Player player, BossLootConfig lootConfig) {
        for (String materialName : lootConfig.guaranteedMaterials()) {
            giveMaterial(player, materialName, de.pixelrpg.rpg.item.ItemRarity.RARE);
        }
    }

    private void giveChanceLoot(Player player, BossLootConfig lootConfig) {
        Random random = ThreadLocalRandom.current();
        for (BossLootEntry entry : lootConfig.chanceDrops()) {
            if (random.nextDouble(100.0D) >= entry.chancePercent()) continue;
            giveMaterial(player, entry.material(), entry.rarity());
        }
    }

    private void giveMaterial(Player player, String materialName, de.pixelrpg.rpg.item.ItemRarity rarity) {
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            RPGItemBuilder.createItem(material, rarity, 0)
                    .ifPresent(item -> player.getInventory().addItem(item).values()
                            .forEach(remainder -> player.getWorld().dropItemNaturally(player.getLocation(), remainder)));
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Invalid boss loot material: " + materialName);
        }
    }

    private void callDefeatedEvent(ActiveBoss activeBoss, Set<UUID> participants) {
        Bukkit.getPluginManager().callEvent(new BossDefeatedEvent(activeBoss.getDefinition().getId(), participants));
    }

    private void cleanup(ActiveBoss activeBoss) {
        if (activeBoss.getTask() != null) activeBoss.getTask().cancel();
        for (UUID viewerUuid : activeBoss.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer != null) viewer.hideBossBar(activeBoss.getBossBar());
        }
        activeBosses.remove(activeBoss.getEntityUuid());
    }

    public boolean hasActiveBossOfType(String bossId) {
        return activeBosses.values().stream().anyMatch(active -> active.getDefinition().getId().equals(bossId));
    }

    public int getActiveBossCount() { return activeBosses.size(); }
    public void shutdownAll() { for (ActiveBoss activeBoss : activeBosses.values()) cleanup(activeBoss); }
    public void shutdown() { shutdownAll(); }
}
