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

    public LivingEntity tick(Player owner, LivingEntity companion, CompanionDefinition definition, long gameTime) {
        CompanionDefinition.CompanionCombatDefinition combat = definition.combat();
        if (!combat.enabled()) return null;
        LivingEntity target = findTarget(owner, companion, combat);
        if (target == null) return null;
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

    public void clear(LivingEntity companion) { nextAttackTick.remove(companion.getUniqueId()); }

    private LivingEntity findTarget(Player owner, LivingEntity companion, CompanionDefinition.CompanionCombatDefinition combat) {
        double range = Math.max(combat.aggroRange(), combat.attackRange());
        LivingEntity best = null;
        double bestDistance = range * range;
        for (Entity nearby : companion.getNearbyEntities(range, range, range)) {
            if (!(nearby instanceof LivingEntity living) || nearby.equals(owner) || nearby.equals(companion)) continue;
            if (!living.isValid() || living.isDead()) continue;
            if (living instanceof Player && !combat.playerTargets()) continue;
            if (living instanceof Monster && !combat.hostileTargets()) continue;
            if (!(living instanceof Monster) && !(living instanceof Player) && !combat.friendlyTargets()) continue;
            double distance = nearby.getLocation().distanceSquared(companion.getLocation());
            if (distance < bestDistance) {
                best = living;
                bestDistance = distance;
            }
        }
        return best;
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
