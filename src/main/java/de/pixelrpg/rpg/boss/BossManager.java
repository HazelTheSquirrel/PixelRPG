package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.events.BossDefeatedEvent;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ClassSetItemFactory;
import de.pixelrpg.rpg.item.ClassSetSlot;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerClass;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
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
    private final EconomyAPI economyAPI;
    private final ItemEconomyConfig itemEconomyConfig;
    private final double barRadius;
    private final int phaseCheckIntervalTicks;
    private final LanguageManager lang;
    private final double classSetDropChance;
    private final Map<UUID, ActiveBoss> activeBosses = new ConcurrentHashMap<>();

    public BossManager(Plugin plugin, BossAttackPatternRegistry patternRegistry, GuildAPI guildAPI,
                       EconomyAPI economyAPI, ItemEconomyConfig itemEconomyConfig,
                       double barRadius, int barUpdateIntervalTicks, int phaseCheckIntervalTicks) {
        this.plugin = plugin;
        this.patternRegistry = patternRegistry;
        this.guildAPI = guildAPI;
        this.economyAPI = economyAPI;
        this.itemEconomyConfig = itemEconomyConfig;
        this.barRadius = barRadius;
        this.phaseCheckIntervalTicks = phaseCheckIntervalTicks;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
        this.classSetDropChance = PixelRPGPlugin.getInstance().getConfig().getDouble("bosses.class-set-drop-chance", 0.08);
    }

    public LivingEntity spawnWorldBoss(BossDefinition definition, Location location) {
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, definition.getBaseEntityType());
        entity.getPersistentDataContainer().set(RPGKeys.Boss.worldBossMarker(), PersistentDataType.BOOLEAN, true);
        announceSpawn(definition, location);
        attachPhaseController(entity, definition);
        return entity;
    }

    public void attachPhaseController(LivingEntity entity, BossDefinition definition) {
        applyBaseStats(entity, definition);
        entity.getPersistentDataContainer().set(RPGKeys.Boss.bossId(), PersistentDataType.STRING, definition.getId());
        entity.getPersistentDataContainer().set(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER, definition.getLevel());
        entity.customName(Component.text(definition.getDisplayName(), NamedTextColor.DARK_RED));
        entity.setCustomNameVisible(true);
        BossBar bossBar = BossBar.bossBar(Component.text(definition.getDisplayName(), NamedTextColor.DARK_RED), 1.0f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        ActiveBoss activeBoss = new ActiveBoss(entity.getUniqueId(), definition, bossBar);
        activeBosses.put(entity.getUniqueId(), activeBoss);
        activeBoss.setTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(activeBoss), 0L, phaseCheckIntervalTicks));
    }

    private void applyBaseStats(LivingEntity entity, BossDefinition definition) {
        AttributeInstance hpAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (hpAttribute != null) {
            double newHp = hpAttribute.getBaseValue() * definition.getHealthMultiplier();
            hpAttribute.setBaseValue(newHp);
            entity.setHealth(newHp);
        }
        AttributeInstance dmgAttribute = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttribute != null) dmgAttribute.setBaseValue(dmgAttribute.getBaseValue() * definition.getDamageMultiplier());
    }

    private void announceSpawn(BossDefinition definition, Location location) {
        Component announcement = lang.get("boss.world-boss-appeared", "name", definition.getDisplayName());
        Component title = Component.text(definition.getDisplayName(), NamedTextColor.DARK_RED);
        Component subtitle = lang.get("boss.world-boss-awakened");
        for (Player player : location.getWorld().getPlayers()) {
            if (!guildAPI.isRegistered(player.getUniqueId())) continue;
            player.sendMessage(announcement);
            player.showTitle(Title.title(title, subtitle, Title.Times.times(
                    Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(500))));
        }
    }

    private void tick(ActiveBoss activeBoss) {
        LivingEntity entity = (LivingEntity) Bukkit.getEntity(activeBoss.getEntityUuid());
        if (entity == null || entity.isDead() || !entity.isValid()) {
            cleanup(activeBoss);
            return;
        }
        updateBossBar(activeBoss, entity);
        checkPhaseTransition(activeBoss, entity);
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
        var phases = activeBoss.getDefinition().getPhases();
        if (phases.isEmpty()) return;
        int targetIndex = 0;
        for (int i = 0; i < phases.size(); i++) {
            if (healthPercent <= phases.get(i).healthPercentageThreshold()) targetIndex = i;
        }
        if (targetIndex != activeBoss.getCurrentPhaseIndex()) {
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
    }

    private void runAttackPatternIfDue(ActiveBoss activeBoss, LivingEntity entity) {
        var phases = activeBoss.getDefinition().getPhases();
        if (phases.isEmpty() || activeBoss.getCurrentPhaseIndex() < 0) return;
        BossPhase phase = phases.get(activeBoss.getCurrentPhaseIndex());
        activeBoss.incrementAttackTimer(phaseCheckIntervalTicks);
        if (activeBoss.getTicksSinceLastAttack() < phase.attackIntervalTicks()) return;
        activeBoss.resetAttackTimer();
        if (phase.attackPatternIds().isEmpty()) return;

        List<Player> targets = new ArrayList<>();
        for (UUID viewerUuid : activeBoss.getViewers()) {
            Player player = Bukkit.getPlayer(viewerUuid);
            if (player != null && player.isOnline() && guildAPI.isRegistered(viewerUuid)) targets.add(player);
        }
        if (targets.isEmpty()) return;

        String patternId = phase.attackPatternIds().get(ThreadLocalRandom.current().nextInt(phase.attackPatternIds().size()));
        patternRegistry.get(patternId).ifPresent(pattern -> pattern.execute(plugin, entity, targets));
    }

    public void onBossDeath(LivingEntity entity) {
        ActiveBoss activeBoss = activeBosses.get(entity.getUniqueId());
        if (activeBoss == null) return;
        cleanup(activeBoss);
        distributeRewards(activeBoss);
    }

    private void distributeRewards(ActiveBoss activeBoss) {
        BossLootConfig lootConfig = activeBoss.getDefinition().getLootConfig();
        Random random = ThreadLocalRandom.current();
        Set<UUID> participants = new HashSet<>();
        for (UUID viewerUuid : activeBoss.getViewers()) {
            Player player = Bukkit.getPlayer(viewerUuid);
            if (player == null || !player.isOnline() || !guildAPI.isRegistered(viewerUuid)) continue;
            participants.add(viewerUuid);
            rollClassSetDrop(player, viewerUuid, activeBoss.getDefinition().getLevel(), random);
            if (lootConfig == null) continue;
            economyAPI.deposit(viewerUuid, lootConfig.moneyReward());
            guildAPI.addExperience(viewerUuid, lootConfig.expReward());
            player.sendMessage(lang.get("boss.defeated-reward", "money", String.valueOf(lootConfig.moneyReward()), "exp", String.valueOf(lootConfig.expReward())));
            if (!lootConfig.materialPool().isEmpty()) {
                String materialName = lootConfig.materialPool().get(random.nextInt(lootConfig.materialPool().size()));
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    RPGItemBuilder.createUnidentified(material, lootConfig.guaranteedRarity(), activeBoss.getDefinition().getLevel())
                            .ifPresent(item -> player.getInventory().addItem(item).values()
                                    .forEach(remainder -> player.getWorld().dropItemNaturally(player.getLocation(), remainder)));
                } catch (IllegalArgumentException ignored) { }
            }
        }
        Bukkit.getPluginManager().callEvent(new BossDefeatedEvent(activeBoss.getDefinition().getId(), participants));
    }

    private void rollClassSetDrop(Player player, UUID uuid, int bossLevel, Random random) {
        if (classSetDropChance <= 0.0 || random.nextDouble() >= classSetDropChance) return;
        PlayerClass playerClass = guildAPI.getPlayerClass(uuid);
        if (playerClass == PlayerClass.NONE) return;
        ClassSetSlot[] slots = ClassSetSlot.values();
        ClassSetSlot slot = slots[random.nextInt(slots.length)];
        ItemStack setItem = ClassSetItemFactory.create(playerClass, slot, bossLevel);
        player.getInventory().addItem(setItem).values()
                .forEach(remainder -> player.getWorld().dropItemNaturally(player.getLocation(), remainder));
        lang.send(player, "boss.class-set-drop", "class",
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(playerClass.displayName()),
                "slot", slot.name());
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

    public int getActiveBossCount() {
        return activeBosses.size();
    }

    public void shutdownAll() {
        for (ActiveBoss activeBoss : activeBosses.values()) cleanup(activeBoss);
    }

    public void shutdown() {
        shutdownAll();
    }
}
