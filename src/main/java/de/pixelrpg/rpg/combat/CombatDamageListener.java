package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ThreadLocalRandom;

public final class CombatDamageListener implements Listener {
    private static final double MAX_CRIT_CHANCE = 100.0D;

    private final GuildAPI guildAPI;
    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final CombatStateService combatStateService;
    private final double bossMaxHitPercentOfMaxHp;

    public CombatDamageListener(Plugin plugin, GuildAPI guildAPI, PlayerProfileManager profileManager, StatEngine statEngine) {
        this.guildAPI = guildAPI;
        this.profileManager = profileManager;
        this.statEngine = statEngine;
        this.combatStateService = new CombatStateService(plugin, guildAPI);
        this.bossMaxHitPercentOfMaxHp = plugin.getConfig()
                .getDouble("combat.boss-max-hit-percent-of-max-hp", 0.12D);
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
        double rawDamage = CombatDamageContext.isWeaponSkill()
                ? Math.max(0.1D, event.getDamage())
                : CombatDamageCalculator.rawPlayerDamage(event.getDamage(), stats);
        double totalCritChance = Math.clamp(stats.critChance(), 0.0D, MAX_CRIT_CHANCE);
        boolean critical = ThreadLocalRandom.current().nextDouble(100.0D) < totalCritChance;
        double damage = critical ? rawDamage * stats.critDamageMultiplier() : rawDamage;

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

        double heal = CombatDamageCalculator.lifesteal(finalDamage, stats.lifestealBonus());
        if (heal > 0.0D) {
            AttributeInstance maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = maxHealth != null ? maxHealth.getValue() : attacker.getHealth();
            attacker.setHealth(Math.min(maxHp, attacker.getHealth() + heal));
        }

        Component feedback = Component.text(
                (critical ? "CRIT! -" : "-") + formatFeedback(finalDamage),
                critical ? NamedTextColor.RED : NamedTextColor.WHITE);
        if (heal > 0.0D) {
            feedback = feedback.append(Component.text("  +" + formatFeedback(heal), NamedTextColor.GREEN));
        }
        attacker.sendActionBar(feedback);

        if (critical) {
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
        player.sendActionBar(Component.text("-" + formatFeedback(finalDamage), NamedTextColor.RED));
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

    public void shutdown() {
        combatStateService.shutdown();
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

    private String formatFeedback(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05D) return Long.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private boolean isRpgEntity(LivingEntity entity) {
        var pdc = entity.getPersistentDataContainer();
        return pdc.has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)
                || pdc.has(RPGKeys.Boss.bossId(), PersistentDataType.STRING);
    }
}
