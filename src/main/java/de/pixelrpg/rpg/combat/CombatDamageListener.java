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
    private static final double MAX_CRIT_CHANCE=100.0D;
    private final GuildAPI guild; private final PlayerProfileManager profiles; private final StatEngine stats; private final CombatStateService combat; private final double bossMaxHitPercent;
    public CombatDamageListener(Plugin plugin,GuildAPI guild,PlayerProfileManager profiles,StatEngine stats,double bossMaxHitPercent){this.guild=guild;this.profiles=profiles;this.stats=stats;this.combat=new CombatStateService(plugin,guild);this.bossMaxHitPercent=bossMaxHitPercent;}
    // Zuständig für die zentrale MMORPG-Schadensberechnung inklusive Crit, Lifesteal und Armor-Mitigation.
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onCombatDamage(EntityDamageByEntityEvent event){
        if(!(event.getEntity() instanceof LivingEntity target))return;
        Player attacker=resolve(event.getDamager());
        if(attacker!=null){handlePlayerAttack(event,attacker,target);return;}
        if(event.getDamager() instanceof Monster monster&&target instanceof Player player)handleMobAttack(event,monster,player);
    }
    private void handlePlayerAttack(EntityDamageByEntityEvent event,Player attacker,LivingEntity target){
        boolean attackerRegistered=guild.isRegistered(attacker.getUniqueId());
        boolean targetRegistered=!(target instanceof Player p)||guild.isRegistered(p.getUniqueId());
        if(!attackerRegistered){if(isRpgEntity(target))event.setCancelled(true);return;}
        if(!targetRegistered){event.setCancelled(true);return;}
        PlayerProfile profile=profiles.getProfile(attacker.getUniqueId()).orElse(null);if(profile==null||!profile.isRegistered())return;
        StatEngine.CachedStats cached=stats.getCachedStats(attacker.getUniqueId());
        double raw=CombatDamageContext.isWeaponSkill()?Math.max(0.1D,event.getDamage()):CombatDamageCalculator.rawPlayerDamage(event.getDamage(),cached);
        boolean critical=ThreadLocalRandom.current().nextDouble(100.0D)<Math.clamp(cached.critChance(),0.0D,MAX_CRIT_CHANCE);
        double damage=critical?raw*cached.critDamageMultiplier():raw;
        double armor=getArmor(target);double finalDamage=CombatDamageCalculator.mitigate(damage,armor);
        if(target.getPersistentDataContainer().has(RPGKeys.Boss.bossId(),PersistentDataType.STRING)){AttributeInstance max=target.getAttribute(Attribute.MAX_HEALTH);double hp=max!=null?max.getValue():target.getHealth();finalDamage=Math.min(finalDamage,hp*bossMaxHitPercent);}
        event.setDamage(CombatDamageCalculator.customDamageBeforeVanillaMitigation(finalDamage,event.getDamage(),event.getFinalDamage()));
        double heal=CombatDamageCalculator.lifesteal(finalDamage,cached.lifestealBonus());if(heal>0){AttributeInstance max=attacker.getAttribute(Attribute.MAX_HEALTH);double hp=max!=null?max.getValue():attacker.getHealth();attacker.setHealth(Math.min(hp,attacker.getHealth()+heal));}
        Component feedback=Component.text((critical?"CRIT! -":"-")+format(finalDamage),critical?NamedTextColor.RED:NamedTextColor.WHITE);if(heal>0)feedback=feedback.append(Component.text("  +"+format(heal),NamedTextColor.GREEN));attacker.sendActionBar(feedback);
        if(critical){target.getWorld().spawnParticle(Particle.CRIT,target.getLocation().add(0,1,0),12,.3,.3,.3);attacker.playSound(attacker.getLocation(),Sound.ENTITY_PLAYER_ATTACK_CRIT,.6f,1.4f);}
    }
    private void handleMobAttack(EntityDamageByEntityEvent event,Monster monster,Player player){if(!isRpgEntity(monster))return;if(!guild.isRegistered(player.getUniqueId())){event.setCancelled(true);monster.setTarget(null);return;}double finalDamage=CombatDamageCalculator.mitigate(event.getDamage(),stats.getCachedStats(player.getUniqueId()).armor());event.setDamage(CombatDamageCalculator.customDamageBeforeVanillaMitigation(finalDamage,event.getDamage(),event.getFinalDamage()));player.sendActionBar(Component.text("-"+format(finalDamage),NamedTextColor.RED));}
    // Zuständig für den Schutz nicht registrierter Spieler vor RPG-Monstern.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onMonsterAttackVanillaPlayer(EntityDamageByEntityEvent event){if(!(event.getDamager() instanceof Monster monster)||!(event.getEntity() instanceof Player player)||!isRpgEntity(monster)||guild.isRegistered(player.getUniqueId()))return;event.setCancelled(true);monster.setTarget(null);}
    // Zuständig dafür, dass RPG-Monster nicht registrierte Spieler nicht als Kampfziel behalten.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onMonsterTargetVanillaPlayer(EntityTargetLivingEntityEvent event){if(!(event.getEntity() instanceof Monster monster)||!(event.getTarget() instanceof Player player)||!isRpgEntity(monster)||guild.isRegistered(player.getUniqueId()))return;event.setCancelled(true);monster.setTarget(null);}
    public void shutdown(){combat.shutdown();}
    private Player resolve(org.bukkit.entity.Entity damager){if(damager instanceof Player p)return p;if(damager instanceof Projectile projectile){ProjectileSource source=projectile.getShooter();if(source instanceof Player p)return p;}return null;}
    private double getArmor(LivingEntity entity){AttributeInstance armor=entity.getAttribute(Attribute.ARMOR);return armor==null?0:Math.max(0,armor.getValue());}
    private String format(double value){return Math.abs(value-Math.rint(value))<.05?Long.toString(Math.round(value)):String.format(java.util.Locale.ROOT,"%.1f",value);}
    private boolean isRpgEntity(LivingEntity entity){var pdc=entity.getPersistentDataContainer();return pdc.has(RPGKeys.Combat.mobLevel(),PersistentDataType.INTEGER)||pdc.has(RPGKeys.Boss.bossId(),PersistentDataType.STRING);}
}
