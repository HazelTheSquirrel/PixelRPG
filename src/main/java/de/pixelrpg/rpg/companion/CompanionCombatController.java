package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Generic companion combat controller shared by normal entities and Unique Mannequin companions. */
public final class CompanionCombatController {
    private final Map<UUID, Long> nextAttackTick = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> currentTargets = new ConcurrentHashMap<>();
    private final Map<UUID, CachedMannequinStats> mannequinStatsCache = new ConcurrentHashMap<>();
    private final CompanionStatsCalculator statsCalculator = new CompanionStatsCalculator();

    public LivingEntity tick(Player owner, LivingEntity companion, CompanionDefinition definition, long gameTime) {
        CompanionDefinition.CompanionCombatDefinition combat = definition.combat();
        if (!combat.enabled()) return null;

        double maxOwnerDistance = combat.maxOwnerCombatDistance();
        if (Double.isFinite(maxOwnerDistance)
                && companion.getLocation().distanceSquared(owner.getLocation()) > maxOwnerDistance * maxOwnerDistance) {
            currentTargets.remove(companion.getUniqueId());
            return null;
        }

        LivingEntity target = resolveTarget(owner, companion, combat);
        if (target == null) {
            currentTargets.remove(companion.getUniqueId());
            return null;
        }

        double range = Math.max(1.0D, combat.attackRange());
        if (companion.getLocation().distanceSquared(target.getLocation()) > range * range) {
            moveTowards(companion, target, definition.follow().movementSpeed());
            return target;
        }

        stop(companion);
        face(companion, target);
        attack(owner, companion, target, definition, combat.attackIntervalTicks(), gameTime);
        return target;
    }

    public void clear(LivingEntity companion) {
        UUID entityId = companion.getUniqueId();
        nextAttackTick.remove(entityId);
        currentTargets.remove(entityId);
        mannequinStatsCache.remove(entityId);
    }

    private LivingEntity resolveTarget(Player owner, LivingEntity companion, CompanionDefinition.CompanionCombatDefinition combat) {
        UUID currentId = currentTargets.get(companion.getUniqueId());
        if (currentId != null) {
            Entity current = companion.getServer().getEntity(currentId);
            if (current instanceof LivingEntity living
                    && isValidTarget(owner, companion, living, combat)
                    && companion.getLocation().distanceSquared(living.getLocation()) <= combat.aggroRange() * combat.aggroRange()) {
                return living;
            }
            currentTargets.remove(companion.getUniqueId(), currentId);
        }

        double range = Math.max(combat.aggroRange(), combat.attackRange());
        LivingEntity best = null;
        double bestDistance = range * range;
        for (Entity nearby : companion.getNearbyEntities(range, range, range)) {
            if (!(nearby instanceof LivingEntity living) || !isValidTarget(owner, companion, living, combat)) continue;
            double distance = nearby.getLocation().distanceSquared(companion.getLocation());
            if (distance < bestDistance) {
                best = living;
                bestDistance = distance;
            }
        }
        if (best != null) currentTargets.put(companion.getUniqueId(), best.getUniqueId());
        return best;
    }

    private static boolean isValidTarget(Player owner, LivingEntity companion, LivingEntity target,
                                         CompanionDefinition.CompanionCombatDefinition combat) {
        if (!target.isValid() || target.isDead() || target.equals(owner) || target.equals(companion)) return false;
        if (target instanceof Player) return combat.playerTargets();
        if (target instanceof Monster) return combat.hostileTargets();
        return combat.friendlyTargets();
    }

    private void attack(Player owner, LivingEntity attacker, LivingEntity target, CompanionDefinition definition,
                        int interval, long gameTime) {
        long next = nextAttackTick.getOrDefault(attacker.getUniqueId(), 0L);
        if (gameTime < next || target.isDead() || !target.isValid()) return;

        if (attacker instanceof Mannequin) {
            attackMannequin(owner, attacker, target, definition);
        } else {
            attacker.attack(target);
            attacker.swingMainHand();
        }

        nextAttackTick.put(attacker.getUniqueId(), gameTime + Math.max(1, interval));
    }

    private void attackMannequin(Player owner, LivingEntity attacker, LivingEntity target, CompanionDefinition definition) {
        int level = Math.max(1, attacker.getPersistentDataContainer()
                .getOrDefault(RPGKeys.Companion.level(), PersistentDataType.INTEGER, 1));
        UUID entityId = attacker.getUniqueId();
        CachedMannequinStats cached = mannequinStatsCache.get(entityId);
        CompanionStats stats;
        if (cached != null && cached.level() == level && cached.definitionId().equals(definition.id())) {
            stats = cached.stats();
        } else {
            CompanionInstance instance = new CompanionInstance(
                    owner.getUniqueId(),
                    definition.id(),
                    level,
                    0L,
                    true,
                    true,
                    CompanionEquipment.empty());
            stats = statsCalculator.calculate(definition, instance, attacker);
            mannequinStatsCache.put(entityId, new CachedMannequinStats(definition.id(), level, stats));
        }

        double damage = Math.max(0.0D, stats.damage());
        if (damage <= 0.0D) return;

        double critChance = Math.clamp(stats.critChance(), 0.0D, 100.0D);
        boolean critical = critChance > 0.0D && ThreadLocalRandom.current().nextDouble(100.0D) < critChance;
        double critMultiplier = 2.0D + Math.max(0.0D, stats.critDamage());
        double finalDamage = critical ? damage * critMultiplier : damage;

        target.damage(finalDamage, attacker);
        attacker.swingMainHand();

        double lifesteal = Math.clamp(stats.lifesteal(), 0.0D, 100.0D);
        if (lifesteal > 0.0D && attacker.getHealth() > 0.0D) {
            attacker.setHealth(Math.min(attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(),
                    attacker.getHealth() + finalDamage * (lifesteal / 100.0D)));
        }

        if (critical) {
            attacker.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation().add(0.0D, 1.0D, 0.0D), 12, 0.35D, 0.35D, 0.35D, 0.05D);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.45F, 1.15F);
        }
    }

    private static void moveTowards(LivingEntity entity, Entity target, double speed) {
        Vector delta = target.getLocation().toVector().subtract(entity.getLocation().toVector());
        delta.setY(Math.max(-0.35D, Math.min(0.35D, delta.getY())));
        if (delta.lengthSquared() < 0.01D) return;
        delta.normalize().multiply(Math.max(0.05D, speed));
        entity.setVelocity(delta);
        face(entity, target);
    }

    private static void stop(LivingEntity entity) {
        Vector velocity = entity.getVelocity();
        entity.setVelocity(new Vector(velocity.getX() * 0.25D, velocity.getY(), velocity.getZ() * 0.25D));
    }

    private static void face(LivingEntity entity, Entity target) {
        Vector direction = target.getLocation().toVector().subtract(entity.getLocation().toVector());
        direction.setY(0.0D);
        if (direction.lengthSquared() < 0.0001D) return;
        entity.setRotation((float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ())), entity.getPitch());
    }

    private record CachedMannequinStats(String definitionId, int level, CompanionStats stats) {
    }
}
