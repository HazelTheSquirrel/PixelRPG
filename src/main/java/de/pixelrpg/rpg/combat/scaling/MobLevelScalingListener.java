package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.core.RPGKeys;
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
        this.plugin = plugin;
        this.guildAPI = guildAPI;
        this.scalingConfig = scalingConfig;
    }

    public void start() {
        if (cleanupTask != null) return;
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::restoreExpiredScaling, 20L, 20L);
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        activeParticipants.clear();
    }

    // Zuständig dafür, dass die Skalierung beim tatsächlichen Zielwechsel auf einen registrierten Spieler aktiviert wird.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMonsterTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;

        if (event.getTarget() instanceof Player player && guildAPI.isRegistered(player.getUniqueId())) {
            markParticipant(monster, player);
        }
    }

    // Zuständig dafür, dass jeder registrierte RPG-Angreifer als aktiver Teilnehmer der Mob-Skalierung erfasst wird.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRpgDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player player) {
            attacker = player;
        } else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) attacker = player;
        }
        if (attacker == null || !guildAPI.isRegistered(attacker.getUniqueId())) return;

        markParticipant(monster, attacker);
    }

    private void markParticipant(Monster monster, Player player) {
        UUID mobUuid = monster.getUniqueId();
        activeParticipants.computeIfAbsent(mobUuid, ignored -> new ConcurrentHashMap<>())
                .put(player.getUniqueId(), System.currentTimeMillis());
        applyScaling(monster);
    }

    private void applyScaling(Monster monster) {
        rememberOriginalAttributes(monster);

        int participantLevel = activeParticipants.getOrDefault(monster.getUniqueId(), Map.of()).keySet().stream()
                .mapToInt(guildAPI::getLevel)
                .filter(level -> level >= Level.MIN_LEVEL)
                .max()
                .orElse(Level.MIN_LEVEL);

        int regionMin = Math.max(Level.MIN_LEVEL, resolveRegionProvider().getMinLevel(monster.getLocation()));
        int regionMax = Math.min(Level.MAX_NORMAL_LEVEL, resolveRegionProvider().getMaxLevel(monster.getLocation()));
        if (regionMin > regionMax) {
            int tmp = regionMin;
            regionMin = regionMax;
            regionMax = tmp;
        }

        MobScalingConfig.DimensionModifier dimension = scalingConfig.getDimensionModifier(monster.getWorld().getEnvironment());
        int targetLevel = Math.max(participantLevel, dimension.baseLevel()) + dimension.levelOffset();
        targetLevel = Math.max(regionMin, Math.min(targetLevel, regionMax));
        targetLevel = Math.max(Level.MIN_LEVEL, Math.min(targetLevel, Level.MAX_NORMAL_LEVEL));

        MobScalingConfig.LevelBaseStats stats = scalingConfig.getBaseStats(targetLevel);
        double maxHp = stats.hp() * dimension.hpMultiplier() * scalingConfig.getPlayerParityMultiplier();
        double damage = stats.damage() * dimension.damageMultiplier() * scalingConfig.getPlayerParityMultiplier();

        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            double oldMaxHp = Math.max(1.0, hp.getValue());
            double healthRatio = Math.max(0.0, Math.min(1.0, monster.getHealth() / oldMaxHp));
            hp.setBaseValue(maxHp);
            monster.setHealth(Math.max(0.0, Math.min(maxHp, maxHp * healthRatio)));
        }

        AttributeInstance attackDamage = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.setBaseValue(damage);

        monster.getPersistentDataContainer().set(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER, targetLevel);
    }

    private void rememberOriginalAttributes(Monster monster) {
        var pdc = monster.getPersistentDataContainer();
        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null && !pdc.has(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE)) {
            pdc.set(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE, hp.getBaseValue());
        }

        AttributeInstance attackDamage = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null && !pdc.has(RPGKeys.Combat.originalAttackDamage(), PersistentDataType.DOUBLE)) {
            pdc.set(RPGKeys.Combat.originalAttackDamage(), PersistentDataType.DOUBLE, attackDamage.getBaseValue());
        }
    }

    private void restoreExpiredScaling() {
        long now = System.currentTimeMillis();

        for (UUID mobUuid : activeParticipants.keySet()) {
            Map<UUID, Long> participants = activeParticipants.get(mobUuid);
            if (participants == null) continue;

            participants.entrySet().removeIf(entry -> now - entry.getValue() >= COMBAT_TIMEOUT_MILLIS || !guildAPI.isRegistered(entry.getKey()));
            var entity = Bukkit.getEntity(mobUuid);

            if (!(entity instanceof Monster monster)) {
                activeParticipants.remove(mobUuid);
                continue;
            }

            if (participants.isEmpty()) {
                restoreVanillaScaling(monster);
                activeParticipants.remove(mobUuid);
                continue;
            }

            applyScaling(monster);
        }
    }

    private void restoreVanillaScaling(Monster monster) {
        var pdc = monster.getPersistentDataContainer();
        if (!pdc.has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)) return;

        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        Double originalMaxHp = pdc.get(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE);
        if (hp != null && originalMaxHp != null) {
            double oldMaxHp = Math.max(1.0, hp.getValue());
            double healthRatio = Math.max(0.0, Math.min(1.0, monster.getHealth() / oldMaxHp));
            hp.setBaseValue(originalMaxHp);
            monster.setHealth(Math.max(0.0, Math.min(originalMaxHp, originalMaxHp * healthRatio)));
        }

        AttributeInstance attackDamage = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        Double originalAttackDamage = pdc.get(RPGKeys.Combat.originalAttackDamage(), PersistentDataType.DOUBLE);
        if (attackDamage != null && originalAttackDamage != null) attackDamage.setBaseValue(originalAttackDamage);

        pdc.remove(RPGKeys.Combat.mobLevel());
        pdc.remove(RPGKeys.Combat.originalMaxHealth());
        pdc.remove(RPGKeys.Combat.originalAttackDamage());
    }

    private RegionDangerProvider resolveRegionProvider() {
        RegionDangerProvider provider = Bukkit.getServicesManager().load(RegionDangerProvider.class);
        return provider != null ? provider : new DefaultRegionDangerProvider();
    }
}
