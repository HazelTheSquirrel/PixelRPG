package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.ThreadLocalRandom;

public final class CombatDamageListener implements Listener {
    private static final double MAX_CRIT_CHANCE = 100.0D;

    private final GuildAPI guildAPI;
    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final LanguageManager languageManager;
    private final double bossMaxHitPercentOfMaxHp;
    private final NamespacedKey companionCritChanceKey;
    private final NamespacedKey companionCritDamageKey;
    private final NamespacedKey companionLifestealKey;

    public CombatDamageListener(GuildAPI guildAPI, PlayerProfileManager profileManager, StatEngine statEngine, Object ignoredScalingConfig) {
        this.guildAPI = guildAPI;
        this.profileManager = profileManager;
        this.statEngine = statEngine;
        this.languageManager = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getLanguageManager();
        this.bossMaxHitPercentOfMaxHp = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getConfig()
                .getDouble("combat.boss-max-hit-percent-of-max-hp", 0.12D);
        this.companionCritChanceKey = new NamespacedKey(de.pixelrpg.rpg.PixelRPGPlugin.getInstance(), "companion_crit_chance");
        this.companionCritDamageKey = new NamespacedKey(de.pixelrpg.rpg.PixelRPGPlugin.getInstance(), "companion_crit_damage");
        this.companionLifestealKey = new NamespacedKey(de.pixelrpg.rpg.PixelRPGPlugin.getInstance(), "companion_lifesteal");
    }

    // Zuständig für die zentrale MMORPG-Schadensberechnung inklusive Vanilla-Basisschaden, Crit, Lifesteal und eigener Armor-Mitigation.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker != null) {
            handlePlayerAttack(event, attacker, target);
            return;
        }

        if (event.getDamager() instanceof Monster monster && target instanceof Player player) {
            handleMobAttack(event, monster, player);
        }
    }

    private void handlePlayerAttack(EntityDamageByEntityEvent event, Player attacker, LivingEntity target) {
        boolean attackerRegistered = guildAPI.isRegistered(attacker.getUniqueId());
        boolean targetRegistered = !(target instanceof Player targetPlayer) || guildAPI.isRegistered(targetPlayer.getUniqueId());

        if (!attackerRegistered) {
            if (isRpgEntity(target)) event.setCancelled(true);
            return;
        }
        if (!targetRegistered) {
            event.setCancelled(true);
            return;
        }

        PlayerProfile profile = profileManager.getProfile(attacker.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;

        StatEngine.CachedStats stats = statEngine.getCachedStats(attacker.getUniqueId());
        var attackerData = attacker.getPersistentDataContainer();
        double companionCritChance = attackerData.getOrDefault(companionCritChanceKey, PersistentDataType.DOUBLE, 0.0D);
        double companionCritDamage = attackerData.getOrDefault(companionCritDamageKey, PersistentDataType.DOUBLE, 0.0D);
        double companionLifesteal = attackerData.getOrDefault(companionLifestealKey, PersistentDataType.DOUBLE, 0.0D);

        double rawDamage = CombatDamageContext.isWeaponSkill()
                ? Math.max(0.1D, event.getDamage())
                : CombatDamageCalculator.rawPlayerDamage(event.getDamage(), stats);
        double totalCritChance = Math.clamp(stats.critChance() + companionCritChance, 0.0D, MAX_CRIT_CHANCE);
        boolean critical = ThreadLocalRandom.current().nextDouble(100.0D) < totalCritChance;
        double damage = CombatDamageCalculator.crit(rawDamage, critical + companionCritDamage > 0.0D && critical);
        if (critical && companionCritDamage > 0.0D) damage += rawDamage * companionCritDamage;

        double targetArmor = getArmor(target);
        double finalDamage = CombatDamageCalculator.mitigate(damage, targetArmor);

        if (target.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) {
            AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
            double bossMaxHp = maxHealth != null ? maxHealth.getValue() : target.getHealth();
            finalDamage = Math.min(finalDamage, bossMaxHp * bossMaxHitPercentOfMaxHp);
        }

        double replacementRawDamage = CombatDamageCalculator.customDamageBeforeVanillaMitigation(
                finalDamage, event.getDamage(), event.getFinalDamage());
        event.setDamage(replacementRawDamage);

        double lifestealPercent = Math.max(0.0D, stats.lifestealBonus() + companionLifesteal);
        double heal = CombatDamageCalculator.lifesteal(finalDamage, lifestealPercent);
        if (heal > 0.0D) {
            AttributeInstance maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = maxHealth != null ? maxHealth.getValue() : attacker.getHealth();
            attacker.setHealth(Math.min(maxHp, attacker.getHealth() + heal));
        }

        if (critical) {
            attacker.sendActionBar(languageManager.get("combat.critical-hit"));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0D, 1.0D, 0.0D), 12, 0.3D, 0.3D, 0.3D);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6F, 1.4F);
        }
    }

    private void handleMobAttack(EntityDamageByEntityEvent event, Monster monster, Player player) {
        if (!isRpgEntity(monster)) return;
        if (!guildAPI.isRegistered(player.getUniqueId())) {
            event.setCancelled(true);
            monster.setTarget(null);
            return;
        }

        double damage = event.getDamage();
        double playerArmor = statEngine.getCachedStats(player.getUniqueId()).armor();
        double finalDamage = CombatDamageCalculator.mitigate(damage, playerArmor);
        double replacementRawDamage = CombatDamageCalculator.customDamageBeforeVanillaMitigation(
                finalDamage, event.getDamage(), event.getFinalDamage());
        event.setDamage(replacementRawDamage);
    }

    // Zuständig für den Schutz nicht registrierter Spieler vor RPG-Monstern.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMonsterAttackVanillaPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Monster monster)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isRpgEntity(monster)) return;
        if (guildAPI.isRegistered(player.getUniqueId())) return;
        event.setCancelled(true);
        monster.setTarget(null);
    }

    // Zuständig dafür, dass RPG-Monster nicht registrierte Spieler nicht als Kampfziel behalten.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMonsterTargetVanillaPlayer(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (!(event.getTarget() instanceof Player player)) return;
        if (!isRpgEntity(monster)) return;
        if (guildAPI.isRegistered(player.getUniqueId())) return;
        event.setCancelled(true);
        monster.setTarget(null);
    }

    private Player resolvePlayerAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        return null;
    }

    private double getArmor(LivingEntity entity) {
        AttributeInstance armor = entity.getAttribute(Attribute.ARMOR);
        return armor == null ? 0.0D : Math.max(0.0D, armor.getValue());
    }

    private boolean isRpgEntity(LivingEntity entity) {
        var pdc = entity.getPersistentDataContainer();
        return pdc.has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)
                || pdc.has(RPGKeys.Boss.bossId(), PersistentDataType.STRING);
    }
}
