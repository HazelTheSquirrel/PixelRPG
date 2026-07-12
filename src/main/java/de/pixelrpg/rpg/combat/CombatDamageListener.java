package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.ClassBalance;
import de.pixelrpg.rpg.player.PlayerClass;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.ThreadLocalRandom;

public final class CombatDamageListener implements Listener {

    private static final double BOSS_MAX_HIT_PERCENT_OF_MAX_HP = 0.12;

    private final GuildAPI guildAPI;
    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final MobScalingConfig scalingConfig;

    public CombatDamageListener(GuildAPI guildAPI, PlayerProfileManager profileManager,
                                 StatEngine statEngine, MobScalingConfig scalingConfig) {
        this.guildAPI = guildAPI;
        this.profileManager = profileManager;
        this.statEngine = statEngine;
        this.scalingConfig = scalingConfig;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        Player attacker = null;
        boolean isRanged = false;

        if (event.getDamager() instanceof Player player) {
            attacker = player;
        } else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                attacker = player;
                isRanged = true;
            }
        }

        if (attacker == null || !guildAPI.isRegistered(attacker.getUniqueId())) return;

        PlayerProfile profile = profileManager.getProfile(attacker.getUniqueId()).orElse(null);
        if (profile == null) return;

        StatEngine.CachedStats stats = statEngine.getCachedStats(attacker.getUniqueId());
        double damage = event.getDamage() + stats.bonusDamage();

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        double weaponCritBonus = 0.0;
        double weaponLifesteal = stats.lifestealBonus();

        if (weapon.hasItemMeta()) {
            var pdc = weapon.getItemMeta().getPersistentDataContainer();
            weaponCritBonus = pdc.getOrDefault(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, 0.0);
            weaponLifesteal += pdc.getOrDefault(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, 0.0);
        }

        double totalCritChance = Math.min(100.0, stats.critChance() + weaponCritBonus);
        boolean isCrit = ThreadLocalRandom.current().nextDouble(100.0) < totalCritChance;
        if (isCrit) {
            damage *= stats.critDamageMultiplier();
            attacker.sendActionBar(Component.text("Critical Hit!", NamedTextColor.LIGHT_PURPLE));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0,1,0),12,0.3,0.3,0.3);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT,0.6f,1.4f);
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
            double cap = bossMaxHp * BOSS_MAX_HIT_PERCENT_OF_MAX_HP;
            if (damage > cap) damage = cap;
        }

        if (!isRanged && ClassSetBonusService.rollProc(setBonus)) {
            double procBurst = damage * 0.35;
            target.damage(procBurst, attacker);
            target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0,1,0),1);
            attacker.playSound(attacker.getLocation(), Sound.ITEM_TOTEM_USE,0.7f,1.6f);
            attacker.sendActionBar(Component.text("Set Bonus Proc!", NamedTextColor.GOLD));
        }

        event.setDamage(damage);

        if (weaponLifesteal > 0.0) {
            var healthAttribute = attacker.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = healthAttribute != null ? healthAttribute.getValue() : attacker.getHealth();
            double healAmount = damage * (weaponLifesteal / 100.0);
            attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + healAmount));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMonsterAttackVanillaPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Monster monster)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!monster.getPersistentDataContainer().has(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER)) return;
        if (guildAPI.isRegistered(player.getUniqueId())) return;
        event.setDamage(scalingConfig.getVanillaPlayerDamageCap());
        monster.setTarget(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMonsterTargetVanillaPlayer(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (!(event.getTarget() instanceof Player player)) return;
        if (!monster.getPersistentDataContainer().has(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER)) return;
        if (guildAPI.isRegistered(player.getUniqueId())) return;

        event.setCancelled(true);
        for (var nearby : monster.getNearbyEntities(16,8,16)) {
            if (nearby instanceof Player guildPlayer && guildAPI.isRegistered(guildPlayer.getUniqueId())) {
                monster.setTarget(guildPlayer);
                break;
            }
        }
    }
}
