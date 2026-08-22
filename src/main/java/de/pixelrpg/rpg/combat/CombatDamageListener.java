package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.ClassBalance;
import de.pixelrpg.rpg.player.PlayerClass;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.ThreadLocalRandom;

public final class CombatDamageListener implements Listener {
    private static final double MAX_CRIT_CHANCE = 50.0D;

    private final GuildAPI guildAPI;
    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final MobScalingConfig scalingConfig;
    private final LanguageManager lang;
    private final double bossMaxHitPercentOfMaxHp;

    public CombatDamageListener(GuildAPI guildAPI, PlayerProfileManager profileManager, StatEngine statEngine, MobScalingConfig scalingConfig) {
        this.guildAPI = guildAPI;
        this.profileManager = profileManager;
        this.statEngine = statEngine;
        this.scalingConfig = scalingConfig;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
        this.bossMaxHitPercentOfMaxHp = PixelRPGPlugin.getInstance().getConfig().getDouble("combat.boss-max-hit-percent-of-max-hp", 0.12);
    }

    // Zuständig für die vollständige RPG-Schadensberechnung gegen Nicht-Spieler-Ziele.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        Player attacker = null;
        boolean isRanged = false;
        if (event.getDamager() instanceof Player player) attacker = player;
        else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) { attacker = player; isRanged = true; }
        }
        if (attacker == null) return;

        if (isRpgEntity(target) && !guildAPI.isRegistered(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (!guildAPI.isRegistered(attacker.getUniqueId())) return;
        if (target instanceof Player targetPlayer && !guildAPI.isRegistered(targetPlayer.getUniqueId())) return;
        if (target instanceof Player) return;
        PlayerProfile profile = profileManager.getProfile(attacker.getUniqueId()).orElse(null);
        if (profile == null) return;

        StatEngine.CachedStats stats = statEngine.getCachedStats(attacker.getUniqueId());
        double damage = event.getDamage() + stats.bonusDamage();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        double weaponLifesteal = stats.lifestealBonus();
        if (weapon.hasItemMeta()) {
            var pdc = weapon.getItemMeta().getPersistentDataContainer();
            Integer itemLevel = pdc.get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
            boolean levelRequirementMet = itemLevel == null || profile.getLevel() >= itemLevel;
            if (levelRequirementMet) {
                weaponLifesteal += pdc.getOrDefault(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, 0.0);
                damage += pdc.getOrDefault(RPGKeys.Item.bonusDamage(), PersistentDataType.DOUBLE, 0.0);
            }
        }

        double totalCritChance = Math.min(MAX_CRIT_CHANCE, Math.max(0.0D, stats.critChance()));
        boolean isCrit = ThreadLocalRandom.current().nextDouble(100.0) < totalCritChance;
        if (isCrit) {
            damage *= stats.critDamageMultiplier();
            attacker.sendActionBar(lang.get("combat.critical-hit"));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 1.4f);
        }

        PlayerClass playerClass = profile.getPlayerClass();
        ClassBalance classBalance = ClassBalance.of(profile);
        damage *= isRanged ? classBalance.rangedDamageMultiplier() : classBalance.meleeDamageMultiplier();
        ClassSetBonusService.SetBonus setBonus = ClassSetBonusService.computeBonus(attacker, playerClass);
        damage *= setBonus.damageMultiplier();
        boolean targetIsBoss = target.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING);
        if (targetIsBoss) {
            var maxHealthAttribute = target.getAttribute(Attribute.MAX_HEALTH);
            double bossMaxHp = maxHealthAttribute != null ? maxHealthAttribute.getValue() : target.getHealth();
            damage = Math.min(damage, bossMaxHp * bossMaxHitPercentOfMaxHp);
        }
        if (!isRanged && ClassSetBonusService.rollProc(setBonus)) {
            double procBurst = damage * 0.25;
            damage += procBurst;
            target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
            attacker.playSound(attacker.getLocation(), Sound.ITEM_TOTEM_USE, 0.7f, 1.6f);
            attacker.sendActionBar(lang.get("combat.set-bonus-proc"));
        }
        event.setDamage(damage);
        if (weaponLifesteal > 0.0) {
            var healthAttribute = attacker.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = healthAttribute != null ? healthAttribute.getValue() : attacker.getHealth();
            attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + damage * (weaponLifesteal / 100.0)));
        }
    }

    // Zuständig für den vollständigen Schutz nicht-registrierter Spieler vor RPG-Monstern.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMonsterAttackVanillaPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Monster monster)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!monster.getPersistentDataContainer().has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)) return;
        if (guildAPI.isRegistered(player.getUniqueId())) return;
        event.setCancelled(true);
        monster.setTarget(null);
    }

    // Zuständig dafür, dass RPG-Monster nicht-registrierte Spieler nicht als Ziel wählen.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMonsterTargetVanillaPlayer(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (!(event.getTarget() instanceof Player player)) return;
        if (!monster.getPersistentDataContainer().has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)) return;
        if (guildAPI.isRegistered(player.getUniqueId())) return;
        event.setCancelled(true);
        for (var nearby : monster.getNearbyEntities(16, 8, 16)) {
            if (nearby instanceof Player guildPlayer && guildAPI.isRegistered(guildPlayer.getUniqueId())) { monster.setTarget(guildPlayer); break; }
        }
    }

    private boolean isRpgEntity(LivingEntity entity) {
        var pdc = entity.getPersistentDataContainer();
        return pdc.has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)
                || pdc.has(RPGKeys.Boss.bossId(), PersistentDataType.STRING);
    }
}
