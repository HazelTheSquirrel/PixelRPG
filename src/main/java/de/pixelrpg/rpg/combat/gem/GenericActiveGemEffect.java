// src/main/java/de/pixelrpg/rpg/combat/gem/GenericActiveGemEffect.java
package de.pixelrpg.rpg.combat.gem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class GenericActiveGemEffect implements ActiveGemEffect {

    private final ActiveSkillGemDefinition definition;

    public GenericActiveGemEffect(ActiveSkillGemDefinition definition) {
        this.definition = definition;
    }

    @Override
    public List<LivingEntity> execute(GemCastContext ctx) {
        Player caster = ctx.caster();
        double damage = ctx.amplify(definition.flatBase() + ctx.stats().bonusDamage() * definition.baseDamageMultiplier());
        List<LivingEntity> hit = new ArrayList<>();

        switch (definition.actionType()) {
            case SINGLE_TARGET_STRIKE -> {
                LivingEntity target = rayTraceTarget(caster, 4.0);
                if (target != null) {
                    strike(caster, target, damage);
                    hit.add(target);
                }
            }
            case CONE_SWEEP, AOE_NOVA, GROUND_SLAM -> {
                for (org.bukkit.entity.Entity entity : caster.getNearbyEntities(definition.radius(), definition.radius(), definition.radius())) {
                    if (entity instanceof LivingEntity target && target != caster) {
                        strike(caster, target, damage);
                        if (definition.actionType() == ActiveSkillActionType.GROUND_SLAM) {
                            target.setVelocity(target.getVelocity().add(new Vector(0, 0.6, 0)));
                        }
                        hit.add(target);
                    }
                }
            }
            case DASH_STRIKE -> {
                Vector direction = caster.getEyeLocation().getDirection().normalize();
                caster.setVelocity(direction.multiply(1.7).setY(0.35));
                LivingEntity target = rayTraceTarget(caster, 5.0);
                if (target != null) {
                    strike(caster, target, damage);
                    hit.add(target);
                }
            }
            case PROJECTILE_VOLLEY -> {
                Vector direction = caster.getEyeLocation().getDirection();
                for (int i = 0; i < 5; i++) {
                    org.bukkit.entity.Arrow arrow = caster.launchProjectile(org.bukkit.entity.Arrow.class);
                    arrow.setShooter(caster);
                    Vector spread = direction.clone().add(new Vector(
                            (Math.random() - 0.5) * 0.25, (Math.random() - 0.5) * 0.25, (Math.random() - 0.5) * 0.25
                    )).multiply(2.5);
                    arrow.setVelocity(spread);
                    arrow.setDamage(damage * 0.4);
                }
            }
            case PIERCING_BEAM -> {
                Location eye = caster.getEyeLocation();
                Vector direction = eye.getDirection();
                Set<UUID> hitIds = new HashSet<>();
                for (double t = 0.0; t < definition.radius(); t += 0.5) {
                    Location point = eye.clone().add(direction.clone().multiply(t));
                    for (org.bukkit.entity.Entity entity : point.getWorld().getNearbyEntities(point, 1.0, 1.0, 1.0)) {
                        if (entity instanceof LivingEntity target && target != caster && hitIds.add(target.getUniqueId())) {
                            strike(caster, target, damage);
                            hit.add(target);
                        }
                    }
                }
            }
            case HEAL_SELF -> healOne(caster, damage);
            case HEAL_GROUP -> {
                healOne(caster, damage);
                for (org.bukkit.entity.Entity entity : caster.getNearbyEntities(definition.radius(), definition.radius(), definition.radius())) {
                    if (entity instanceof Player ally) {
                        healOne(ally, damage);
                    }
                }
            }
            case CLEANSE_GROUP -> {
                cleanse(caster);
                for (org.bukkit.entity.Entity entity : caster.getNearbyEntities(definition.radius(), definition.radius(), definition.radius())) {
                    if (entity instanceof Player ally) {
                        cleanse(ally);
                    }
                }
            }
            case BUFF_GROUP_STRENGTH -> {
                buff(caster, PotionEffectType.STRENGTH);
                buff(caster, PotionEffectType.RESISTANCE);
                for (org.bukkit.entity.Entity entity : caster.getNearbyEntities(definition.radius(), definition.radius(), definition.radius())) {
                    if (entity instanceof Player ally) {
                        buff(ally, PotionEffectType.STRENGTH);
                        buff(ally, PotionEffectType.RESISTANCE);
                    }
                }
            }
            case BUFF_SELF_SPEED -> {
                buff(caster, PotionEffectType.SPEED);
                buff(caster, PotionEffectType.JUMP_BOOST);
            }
            case SHIELD_SELF -> buff(caster, PotionEffectType.ABSORPTION);
        }

        if (definition.sound() != null) {
            caster.playSound(caster.getLocation(), definition.sound(), 1.0f, 1.0f);
        }
        if (definition.particle() != null) {
            caster.getWorld().spawnParticle(definition.particle(), caster.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4);
        }
        caster.sendMessage(Component.text(definition.displayName() + "!", NamedTextColor.LIGHT_PURPLE));
        return hit;
    }

    private void strike(Player caster, LivingEntity target, double damage) {
        target.damage(damage, caster);
        if (definition.applySlow()) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, definition.effectDurationTicks(), 1, true, true));
        }
        if (definition.applyRoot()) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, definition.effectDurationTicks(), 6, true, true));
        }
        if (definition.applyWeaken()) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, definition.effectDurationTicks(), 1, true, true));
        }
        if (definition.applyBurn()) {
            target.setFireTicks(Math.max(target.getFireTicks(), definition.effectDurationTicks()));
        }
        if (definition.lifesteal() && caster.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
            double max = caster.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            caster.setHealth(Math.min(max, caster.getHealth() + damage * 0.25));
        }
    }

    private LivingEntity rayTraceTarget(Player caster, double range) {
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(), caster.getEyeLocation().getDirection(), range,
                entity -> entity instanceof LivingEntity && entity != caster);
        return result != null && result.getHitEntity() instanceof LivingEntity target ? target : null;
    }

    private void healOne(Player target, double amount) {
        var attribute = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        double max = attribute.getValue();
        target.setHealth(Math.min(max, target.getHealth() + amount));
    }

    private void cleanse(Player target) {
        target.removePotionEffect(PotionEffectType.POISON);
        target.removePotionEffect(PotionEffectType.WITHER);
        target.removePotionEffect(PotionEffectType.SLOWNESS);
        target.removePotionEffect(PotionEffectType.WEAKNESS);
    }

    private void buff(Player target, PotionEffectType type) {
        target.addPotionEffect(new PotionEffect(type, definition.effectDurationTicks(), 1, true, true));
    }
}