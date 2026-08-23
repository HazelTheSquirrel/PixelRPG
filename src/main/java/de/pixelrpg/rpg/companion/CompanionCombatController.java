package de.pixelrpg.rpg.companion;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Generic companion combat controller shared by normal entities and Mannequins. */
public final class CompanionCombatController {
    private final Map<UUID, Long> nextAttackTick = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> currentTargets = new ConcurrentHashMap<>();

    public LivingEntity tick(Player owner, LivingEntity companion, CompanionDefinition definition, long gameTime) {
        CompanionDefinition.CompanionCombatDefinition combat = definition.combat();
        if (!combat.enabled()) return null;
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
        attack(companion, target, combat.attackIntervalTicks(), gameTime);
        return target;
    }

    public void clear(LivingEntity companion) {
        nextAttackTick.remove(companion.getUniqueId());
        currentTargets.remove(companion.getUniqueId());
    }

    private LivingEntity resolveTarget(Player owner, LivingEntity companion, CompanionDefinition.CompanionCombatDefinition combat) {
        UUID currentId = currentTargets.get(companion.getUniqueId());
        if (currentId != null) {
            Entity current = companion.getServer().getEntity(currentId);
            if (current instanceof LivingEntity living && isValidTarget(owner, companion, living, combat)
                    && companion.getLocation().distanceSquared(living.getLocation()) <= combat.aggroRange() * combat.aggroRange()) return living;
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

    private static boolean isValidTarget(Player owner, LivingEntity companion, LivingEntity target, CompanionDefinition.CompanionCombatDefinition combat) {
        if (!target.isValid() || target.isDead() || target.equals(owner) || target.equals(companion)) return false;
        if (target instanceof Player) return combat.playerTargets();
        if (target instanceof Monster) return combat.hostileTargets();
        return combat.friendlyTargets();
    }

    private void attack(LivingEntity attacker, LivingEntity target, int interval, long gameTime) {
        long next = nextAttackTick.getOrDefault(attacker.getUniqueId(), 0L);
        if (gameTime < next || target.isDead() || !target.isValid()) return;
        attacker.attack(target);
        attacker.swingMainHand();
        nextAttackTick.put(attacker.getUniqueId(), gameTime + Math.max(1, interval));
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
}
