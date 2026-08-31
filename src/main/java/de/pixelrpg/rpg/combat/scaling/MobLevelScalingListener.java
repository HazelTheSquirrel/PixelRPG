package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.balance.BalanceModel;
import de.pixelrpg.rpg.balance.PlayerPowerIndex;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.stats.StatEngine;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobLevelScalingListener implements Listener {
    private static final long COMBAT_TIMEOUT_MILLIS = 5_000L;
    private final Plugin plugin;
    private final GuildAPI guildAPI;
    private final MobScalingConfig scalingConfig;
    private final Map<UUID, Map<UUID, Long>> activeParticipants = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public MobLevelScalingListener(Plugin plugin, GuildAPI guildAPI, MobScalingConfig scalingConfig) {
        this.plugin = plugin; this.guildAPI = guildAPI; this.scalingConfig = scalingConfig;
    }
    public void start() { if (cleanupTask == null) cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::restoreExpiredScaling, 20L, 20L); }
    public void shutdown() { if (cleanupTask != null) { cleanupTask.cancel(); cleanupTask = null; } activeParticipants.clear(); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMonsterTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;
        if (event.getTarget() instanceof Player player && guildAPI.isRegistered(player.getUniqueId())) markParticipant(monster, player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRpgDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player player) attacker = player;
        else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) attacker = player;
        if (attacker != null && guildAPI.isRegistered(attacker.getUniqueId())) markParticipant(monster, attacker);
    }

    private void markParticipant(Monster monster, Player player) {
        activeParticipants.computeIfAbsent(monster.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(player.getUniqueId(), System.currentTimeMillis());
        applyScaling(monster);
    }

    private void applyScaling(Monster monster) {
        rememberOriginalAttributes(monster);
        Map<UUID, Long> participants = activeParticipants.getOrDefault(monster.getUniqueId(), Map.of());
        int level = BalanceModel.clampLevel(participants.keySet().stream().mapToInt(guildAPI::getLevel).max().orElse(1));
        ArrayList<StatEngine.CachedStats> stats = new ArrayList<>();
        if (plugin instanceof PixelRPGPlugin pixelRPG && pixelRPG.getStatEngine() != null) {
            for (UUID uuid : participants.keySet()) { if (findPlayer(uuid) != null) stats.add(pixelRPG.getStatEngine().getCachedStats(uuid)); }
        }
        double ppi = PlayerPowerIndex.groupPower(stats);
        MobScalingConfig.LevelBaseStats base = scalingConfig.getBaseStats(level);
        double levelPower = BalanceModel.levelPower(level);
        double hp = base.hp() * (1.0D + 1.80D * levelPower) * (0.85D + 0.30D * ppi) * scalingConfig.getPlayerParityMultiplier();
        double damage = base.damage() * (1.0D + 1.20D * levelPower) * (0.90D + 0.20D * ppi) * scalingConfig.getPlayerParityMultiplier();

        AttributeInstance health = monster.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            double old = Math.max(1.0D, health.getValue());
            double ratio = Math.clamp(monster.getHealth() / old, 0.0D, 1.0D);
            health.setBaseValue(Math.max(1.0D, hp)); monster.setHealth(Math.clamp(hp * ratio, 0.0D, hp));
        }
        AttributeInstance attack = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(Math.max(0.0D, damage));
        monster.getPersistentDataContainer().set(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER, level);
    }

    private Player findPlayer(UUID uuid) { Player player = Bukkit.getPlayer(uuid); return player != null && player.isOnline() ? player : null; }

    private void rememberOriginalAttributes(Monster monster) {
        var pdc = monster.getPersistentDataContainer();
        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null && !pdc.has(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE)) pdc.set(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE, hp.getBaseValue());
        AttributeInstance attack = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null && !pdc.has(RPGKeys.Combat.originalAttackDamage(), PersistentDataType.DOUBLE)) pdc.set(RPGKeys.Combat.originalAttackDamage(), PersistentDataType.DOUBLE, attack.getBaseValue());
    }

    private void restoreExpiredScaling() {
        long now = System.currentTimeMillis();
        for (UUID uuid : activeParticipants.keySet()) {
            Map<UUID, Long> participants = activeParticipants.get(uuid);
            if (participants == null) continue;
            participants.entrySet().removeIf(e -> now - e.getValue() >= COMBAT_TIMEOUT_MILLIS || !guildAPI.isRegistered(e.getKey()));
            var entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof Monster monster)) { activeParticipants.remove(uuid); continue; }
            if (participants.isEmpty()) { restoreVanillaScaling(monster); activeParticipants.remove(uuid); }
            else applyScaling(monster);
        }
    }

    private void restoreVanillaScaling(Monster monster) {
        var pdc = monster.getPersistentDataContainer();
        if (!pdc.has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)) return;
        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        Double originalHp = pdc.get(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE);
        if (hp != null && originalHp != null) {
            double old = Math.max(1.0D, hp.getValue()); double ratio = Math.clamp(monster.getHealth() / old, 0.0D, 1.0D);
            hp.setBaseValue(originalHp); monster.setHealth(Math.clamp(originalHp * ratio, 0.0D, originalHp));
        }
        AttributeInstance attack = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        Double originalAttack = pdc.get(RPGKeys.Combat.originalAttackDamage(), PersistentDataType.DOUBLE);
        if (attack != null && originalAttack != null) attack.setBaseValue(originalAttack);
        pdc.remove(RPGKeys.Combat.mobLevel()); pdc.remove(RPGKeys.Combat.originalMaxHealth()); pdc.remove(RPGKeys.Combat.originalAttackDamage());
    }
}
