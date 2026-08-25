package de.pixelrpg.rpg.companion;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/** Single source of truth for definition, rarity and level based companion stat calculation. */
public final class CompanionStatsCalculator {
    public CompanionStats calculate(CompanionDefinition definition, CompanionInstance instance, LivingEntity entity) {
        CompanionStats base = definition.baseStats();
        CompanionDefinition.CompanionProgressionDefinition progression = definition.progression();
        int level = Math.max(1, instance.level());
        double rarity = definition.rarity().statMultiplier();
        double healthMultiplier = cap(rarity * levelMultiplier(progression.healthPerLevel(), level), progression.healthCap());
        double damageMultiplier = cap(rarity * levelMultiplier(progression.damagePerLevel(), level), progression.damageCap());
        double speedMultiplier = cap(rarity * levelMultiplier(progression.speedPerLevel(), level), progression.speedCap());

        double entityHealth = attributeBase(entity, Attribute.MAX_HEALTH, 20.0D);
        double entityDamage = attributeBase(entity, Attribute.ATTACK_DAMAGE, 1.0D);
        double entitySpeed = attributeBase(entity, Attribute.MOVEMENT_SPEED, 0.3D);

        double health = base.health() > 0.0D ? base.health() : entityHealth;
        double damage = base.damage() > 0.0D ? base.damage() : entityDamage;
        double speed = base.movementSpeed() > 0.0D ? base.movementSpeed() : entitySpeed;

        return new CompanionStats(
                Math.max(1.0D, health * healthMultiplier),
                Math.max(0.0D, damage * damageMultiplier),
                Math.max(0.01D, speed * speedMultiplier),
                base.armor(),
                base.critChance(),
                base.critDamage(),
                base.lifesteal(),
                base.abilityDamage(),
                base.mana());
    }

    public void apply(CompanionStats stats, LivingEntity entity) {
        set(entity, Attribute.MAX_HEALTH, stats.health());
        set(entity, Attribute.ATTACK_DAMAGE, stats.damage());
        set(entity, Attribute.MOVEMENT_SPEED, stats.movementSpeed());
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) entity.setHealth(Math.min(entity.getHealth(), maxHealth.getValue()));
    }

    private static void set(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && Double.isFinite(value) && value > 0.0D) instance.setBaseValue(value);
    }

    private static double attributeBase(LivingEntity entity, Attribute attribute, double fallback) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? fallback : Math.max(0.01D, instance.getBaseValue());
    }

    private static double levelMultiplier(double perLevel, int level) {
        return 1.0D + Math.max(0, level - 1) * Math.max(0.0D, perLevel);
    }

    private static double cap(double value, double cap) {
        return Math.min(Math.max(0.01D, value), Math.max(0.01D, cap));
    }
}
