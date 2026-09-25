package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.api.events.BossDefeatedEvent;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.ItemService;
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
    private static final double WORLD_BOSS_CLEANUP_RADIUS = 256.0D;

    private final Plugin plugin;
    private final BossAttackPatternRegistry patternRegistry;
    private final GuildAPI guildAPI;
    private final PartyAPI partyAPI;
    private final EconomyAPI economyAPI;
    private final MobScalingConfig mobScalingConfig;
    private final ItemService itemService;
    private final double barRadius;
    private final double barRadiusSquared;
    private final int barUpdateIntervalTicks;
    private final int phaseCheckIntervalTicks;
    private final Map<UUID, ActiveBoss> activeBosses = new ConcurrentHashMap<>();

    public BossManager(Plugin plugin, BossAttackPatternRegistry patternRegistry, GuildAPI guildAPI,
                       PartyAPI partyAPI, EconomyAPI economyAPI, ItemService itemService,
                       MobScalingConfig mobScalingConfig,
                       double barRadius, int barUpdateIntervalTicks, int phaseCheckIntervalTicks) {
        this.plugin = plugin;
        this.patternRegistry = patternRegistry;
        this.guildAPI = guildAPI;
        this.partyAPI = partyAPI;
        this.economyAPI = economyAPI;
        this.mobScalingConfig = mobScalingConfig;
        this.itemService = itemService;
        this.barRadius = Math.max(1.0D, barRadius);
        this.barRadiusSquared = this.barRadius * this.barRadius;
        this.barUpdateIntervalTicks = Math.max(1, barUpdateIntervalTicks);
        this.phaseCheckIntervalTicks = Math.max(1, phaseCheckIntervalTicks);
        if (this.itemService == null) throw new IllegalArgumentException("itemService must not be null");
        plugin.getServer().getPluginManager().registerEvents(new BossRewardItemListener(guildAPI), plugin);
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

    public void attachPhaseController(LivingEntity entity, BossDefinition definition) {
        applyBaseStats(entity, definition);
        entity.getPersistentDataContainer().set(RPGKeys.Boss.bossId(), PersistentDataType.STRING, definition.getId());
        entity.getPersistentDataContainer().set(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER, definition.getLevel());
        entity.customName(Component.text(definition.getDisplayName(), NamedTextColor.DARK_RED));
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);
        BossBar bossBar = BossBar.bossBar(Component.text(definition.getDisplayName(), NamedTextColor.DARK_RED), 1.0f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        ActiveBoss activeBoss = new ActiveBoss(entity.getUniqueId(), definition, bossBar);
        if (definition.getKind() == BossKind.BIOME) activeBoss.setLastValidBiomeLocation(entity.getLocation());
        activeBosses.put(entity.getUniqueId(), activeBoss);
        activeBoss.setTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(activeBoss), 0L, phaseCheckIntervalTicks));
    }

    private void applyBaseStats(LivingEntity entity, BossDefinition definition) {
        MobScalingConfig.LevelBaseStats levelStats = mobScalingConfig.getBaseStats(definition.getLevel());
        double parityMultiplier = mobScalingConfig.getPlayerParityMultiplier();
        double newHp = Math.min(1024.0D, Math.max(1.0D, levelStats.hp() * parityMultiplier * definition.getHealthMultiplier()));
        double newDamage = levelStats.damage() * parityMultiplier * definition.getDamageMultiplier();
        AttributeInstance hpAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (hpAttribute != null) { hpAttribute.setBaseValue(newHp); entity.setHealth(newHp); }
        AttributeInstance dmgAttribute = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttribute != null) dmgAttribute.setBaseValue(newDamage);
        AttributeInstance scaleAttribute = entity.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) scaleAttribute.setBaseValue(scaleAttribute.getBaseValue() * definition.getScaleMultiplier());
    }

    private void tick(ActiveBoss activeBoss) {
        LivingEntity entity = (LivingEntity) Bukkit.getEntity(activeBoss.getEntityUuid());
        if (entity == null || entity.isDead() || !entity.isValid()) { cleanup(activeBoss); return; }
        if (activeBoss.getDefinition().getKind() == BossKind.BIOME) {
            if (!activeBoss.getDefinition().matchesBiome(entity.getLocation().getBlock().getBiome())) {
                Location safe = activeBoss.getLastValidBiomeLocation();
                if (safe != null && safe.getWorld() != null) entity.teleport(safe);
                else { entity.remove(); cleanup(activeBoss); return; }
            } else activeBoss.setLastValidBiomeLocation(entity.getLocation());
        }
        activeBoss.incrementBarUpdateTimer(phaseCheckIntervalTicks);
        if (activeBoss.getTicksSinceLastBarUpdate() >= barUpdateIntervalTicks) { activeBoss.resetBarUpdateTimer(); updateBossBar(activeBoss, entity); }
        if (activeBoss.getDefinition().getKind() == BossKind.WORLD_EVENT) checkPhaseTransition(activeBoss, entity);
        runAttackPatternIfDue(activeBoss, entity);
    }

    private void updateBossBar(ActiveBoss activeBoss, LivingEntity entity) {
        AttributeInstance hpAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = hpAttribute != null ? hpAttribute.getValue() : 20.0;
        activeBoss.getBossBar().progress((float) Math.max(0.0, Math.min(1.0, entity.getHealth() / maxHp)));
        Location location = entity.getLocation();
        for (Player player : location.getNearbyPlayers(barRadius)) {
            UUID uuid = player.getUniqueId();
            boolean isViewer = activeBoss.getViewers().contains(uuid);
            if (!guildAPI.isRegistered(uuid)) { if (isViewer) { player.hideBossBar(activeBoss.getBossBar()); activeBoss.getViewers().remove(uuid); } continue; }
            boolean inRange = player.getLocation().distanceSquared(location) <= barRadiusSquared;
            if (inRange && !isViewer) { player.showBossBar(activeBoss.getBossBar()); activeBoss.getViewers().add(uuid); }
            else if (!inRange && isViewer) { player.hideBossBar(activeBoss.getBossBar()); activeBoss.getViewers().remove(uuid); }
        }
        activeBoss.getViewers().removeIf(uuid -> {
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer == null || !viewer.isOnline()) return true;
            if (viewer.getWorld() != location.getWorld() || viewer.getLocation().distanceSquared(location) > barRadiusSquared) {
                viewer.hideBossBar(activeBoss.getBossBar());
                return true;
            }
            return false;
        });
    }

    private void checkPhaseTransition(ActiveBoss activeBoss, LivingEntity entity) {
        AttributeInstance hpAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = hpAttribute != null ? hpAttribute.getValue() : 20.0;
        double healthPercent = entity.getHealth() / maxHp * 100.0;
        List<BossPhase> phases = activeBoss.getDefinition().getPhases();
        if (phases.isEmpty()) return;
        int targetIndex = 0;
        for (int i = 0; i < phases.size(); i++) if (healthPercent <= phases.get(i).healthPercentageThreshold()) targetIndex = i;
        if (targetIndex == activeBoss.getCurrentPhaseIndex()) return;
        activeBoss.setCurrentPhaseIndex(targetIndex);
        BossPhase phase = phases.get(targetIndex);
        if (!phase.announcementMessage().isBlank()) for (UUID viewerUuid : activeBoss.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer != null && guildAPI.isRegistered(viewerUuid)) viewer.sendMessage(Component.text(phase.announcementMessage(), NamedTextColor.DARK_RED));
        }
    }

    private void runAttackPatternIfDue(ActiveBoss activeBoss, LivingEntity entity) {
        BossDefinition definition = activeBoss.getDefinition();
        List<String> patterns;
        int interval;
        if (definition.getKind() == BossKind.WORLD_EVENT) {
            List<BossPhase> phases = definition.getPhases();
            if (phases.isEmpty() || activeBoss.getCurrentPhaseIndex() < 0) return;
            BossPhase phase = phases.get(activeBoss.getCurrentPhaseIndex()); patterns = phase.attackPatternIds(); interval = phase.attackIntervalTicks();
        } else { patterns = activeBoss.getBiomeAttackPatternIds(); interval = definition.getAttackIntervalTicks(); }
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
        if (lootConfig == null) { callDefeatedEvent(activeBoss, Set.of()); return; }
        Set<UUID> participants = new HashSet<>(activeBoss.getDamageContribution().keySet());
        Set<UUID> recipients = activeBoss.getDefinition().getKind() == BossKind.WORLD_EVENT ? onlineRegistered(participants) : resolvePartyRecipients(participants);
        for (UUID uuid : recipients) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || !guildAPI.isRegistered(uuid)) continue;
            economyAPI.deposit(uuid, lootConfig.moneyReward());
            guildAPI.addExperience(uuid, lootConfig.expReward());
            giveGuaranteedLoot(player, lootConfig, activeBoss.getDefinition().getLevel());
            giveChanceLoot(player, lootConfig, activeBoss.getDefinition().getLevel());
            player.sendMessage(Component.text("Boss besiegt! +" + lootConfig.moneyReward() + " Gold, +" + lootConfig.expReward() + " EP", NamedTextColor.GREEN));
        }
        callDefeatedEvent(activeBoss, onlineRegistered(participants));
    }

    private Set<UUID> resolvePartyRecipients(Set<UUID> participants) {
        Set<UUID> recipients = new HashSet<>();
        for (UUID participant : participants) {
            if (!partyAPI.isInParty(participant)) { if (Bukkit.getPlayer(participant) != null) recipients.add(participant); continue; }
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
        for (UUID uuid : uuids) { Player player = Bukkit.getPlayer(uuid); if (player != null && player.isOnline() && guildAPI.isRegistered(uuid)) result.add(uuid); }
        return result;
    }

    private void giveGuaranteedLoot(Player player, BossLootConfig lootConfig, int level) { for (String reward : lootConfig.guaranteedMaterials()) giveReward(player, reward, ItemRarity.RARE, level); }

    private void giveChanceLoot(Player player, BossLootConfig lootConfig, int level) {
        Random random = ThreadLocalRandom.current();
        for (BossLootEntry entry : lootConfig.chanceDrops()) if (random.nextDouble(100.0D) < entry.chancePercent()) giveReward(player, entry.material(), entry.rarity(), level);
    }

    private void giveReward(Player player, String rewardId, ItemRarity rarity, int level) {
        if (rewardId == null || rewardId.isBlank()) return;
        String normalized = rewardId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("pixelrpg:")) {
            plugin.getLogger().warning("Ignoring custom PixelRPG boss loot '" + rewardId + "'; custom items are quest/profession rewards only.");
            return;
        }
        Material material = Material.matchMaterial(rewardId);
        if (material == null || material.isAir()) {
            plugin.getLogger().warning("Invalid boss loot reward: " + rewardId);
            return;
        }
        itemService.createVanillaReward(material, rarity, level).ifPresent(item -> giveItem(player, item));
    }

    private void giveItem(Player player, org.bukkit.inventory.ItemStack item) { player.getInventory().addItem(item).values().forEach(remainder -> player.getWorld().dropItemNaturally(player.getLocation(), remainder)); }
    private void callDefeatedEvent(ActiveBoss activeBoss, Set<UUID> participants) { Bukkit.getPluginManager().callEvent(new BossDefeatedEvent(activeBoss.getDefinition().getId(), participants)); }

    private void cleanup(ActiveBoss activeBoss) {
        if (activeBoss.getTask() != null) activeBoss.getTask().cancel();
        for (UUID viewerUuid : activeBoss.getViewers()) { Player viewer = Bukkit.getPlayer(viewerUuid); if (viewer != null) viewer.hideBossBar(activeBoss.getBossBar()); }
        if (activeBoss.getDefinition().getKind() == BossKind.WORLD_EVENT) {
            String bossId = activeBoss.getDefinition().getId();
            for (Entity entity : activeBossEntityWorldEntities(activeBoss)) {
                if (entity.getUniqueId().equals(activeBoss.getEntityUuid())) continue;
                String entityBossId = entity.getPersistentDataContainer().get(RPGKeys.Boss.bossId(), PersistentDataType.STRING);
                boolean worldBossEntity = entity.getPersistentDataContainer().getOrDefault(RPGKeys.Boss.worldBossMarker(), PersistentDataType.BOOLEAN, false);
                if (worldBossEntity && bossId.equals(entityBossId)) entity.remove();
            }
        }
        activeBosses.remove(activeBoss.getEntityUuid());
    }

    private List<Entity> activeBossEntityWorldEntities(ActiveBoss activeBoss) {
        Entity bossEntity = Bukkit.getEntity(activeBoss.getEntityUuid());
        if (bossEntity == null || bossEntity.getWorld() == null) return List.of();
        Location location = bossEntity.getLocation();
        return new ArrayList<>(location.getNearbyEntities(WORLD_BOSS_CLEANUP_RADIUS, WORLD_BOSS_CLEANUP_RADIUS, WORLD_BOSS_CLEANUP_RADIUS));
    }

    public boolean hasActiveBossOfType(String bossId) { return activeBosses.values().stream().anyMatch(active -> active.getDefinition().getId().equals(bossId)); }
    public int getActiveBossCount() { return activeBosses.size(); }
    public void shutdownAll() { for (ActiveBoss activeBoss : activeBosses.values()) cleanup(activeBoss); }
    public void shutdown() { shutdownAll(); }
}
