package de.pixelrpg.rpg.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.npc.MannequinSkinResolver;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Controls movement, presentation, equipment and combat for Mannequin companions. */
public final class MannequinCompanionController {
    private static final double FOLLOW_START = 3.0D;
    private static final double FOLLOW_STOP = 1.5D;
    private static final double TELEPORT_DISTANCE_SQUARED = 900.0D;
    private static final double NORMAL_SPEED = 0.34D;
    private static final double SPRINT_SPEED = 0.44D;
    private static final double ATTACK_RANGE = 3.5D;
    private static final int DEFAULT_ATTACK_INTERVAL = 20;

    private final Plugin plugin;
    private final CompanionService companionService;
    private final Map<UUID, Long> nextAttackTick = new ConcurrentHashMap<>();
    private final Map<UUID, String> configuredSkin = new ConcurrentHashMap<>();

    public MannequinCompanionController(Plugin plugin, CompanionService companionService) {
        this.plugin = plugin;
        this.companionService = companionService;
    }

    public void tick(Player owner, Mannequin mannequin) {
        if (!mannequin.isValid() || !owner.isOnline()) return;

        applyPresentation(mannequin);
        applyEquipment(owner, mannequin);

        if (mannequin.getWorld() != owner.getWorld()) {
            mannequin.teleport(owner.getLocation().clone().add(1.0D, 0.0D, 1.0D));
            return;
        }

        double distanceSquared = mannequin.getLocation().distanceSquared(owner.getLocation());
        if (distanceSquared > TELEPORT_DISTANCE_SQUARED) {
            mannequin.teleport(owner.getLocation().clone().add(1.0D, 0.0D, 1.0D));
            mannequin.setVelocity(new Vector());
            return;
        }

        Monster target = findTarget(mannequin, owner);
        if (target != null) {
            double attackRange = Math.max(2.0D, loadDouble(mannequin, "attackRange", ATTACK_RANGE));
            if (mannequin.getLocation().distanceSquared(target.getLocation()) > attackRange * attackRange) {
                moveTowards(mannequin, target.getLocation(), owner.isSprinting() ? SPRINT_SPEED : NORMAL_SPEED);
            } else {
                stop(mannequin);
                face(mannequin, target.getLocation());
                attack(mannequin, target);
            }
            return;
        }

        if (distanceSquared > FOLLOW_START * FOLLOW_START) {
            Vector behind = owner.getLocation().getDirection().clone();
            behind.setY(0.0D);
            if (behind.lengthSquared() < 0.001D) behind.setZ(1.0D);
            behind.normalize().multiply(-1.5D);
            Location targetLocation = owner.getLocation().clone().add(behind);
            moveTowards(mannequin, targetLocation, owner.isSprinting() ? SPRINT_SPEED : NORMAL_SPEED);
        } else if (distanceSquared < FOLLOW_STOP * FOLLOW_STOP) {
            stop(mannequin);
        }
    }

    private void applyPresentation(Mannequin mannequin) {
        AttributeInstance scale = mannequin.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(loadDouble(mannequin, "scale", 0.5D));
        mannequin.setImmovable(false);

        String skin = loadString(mannequin, "skinSource");
        if (!skin.isBlank() && !skin.equals(configuredSkin.get(mannequin.getUniqueId()))) {
            configuredSkin.put(mannequin.getUniqueId(), skin);
            MannequinSkinResolver.apply(mannequin, skin, plugin.getLogger());
        }
    }

    private void applyEquipment(Player owner, Mannequin mannequin) {
        companionService.applyEquipmentToEntity(owner.getUniqueId(), companionId(mannequin), mannequin);
    }

    private void attack(Mannequin attacker, Monster target) {
        long now = attacker.getWorld().getGameTime();
        long next = nextAttackTick.getOrDefault(attacker.getUniqueId(), 0L);
        if (now < next || target.isDead() || !target.isValid()) return;

        int interval = Math.max(1, loadInt(attacker, "attackIntervalTicks", DEFAULT_ATTACK_INTERVAL));
        double damage = calculateDamage(attacker);
        target.damage(damage, attacker);
        nextAttackTick.put(attacker.getUniqueId(), now + interval);
    }

    private double calculateDamage(Mannequin mannequin) {
        AttributeInstance attack = mannequin.getAttribute(Attribute.ATTACK_DAMAGE);
        double damage = attack == null ? 1.0D : Math.max(1.0D, attack.getValue());
        ItemStack weapon = mannequin.getEquipment() == null ? null : mannequin.getEquipment().getItemInMainHand();
        if (weapon == null || weapon.getType().isAir()) return damage;
        if (damage > 1.0D) return damage;
        return switch (weapon.getType()) {
            case WOODEN_SWORD -> 4.0D;
            case STONE_SWORD -> 5.0D;
            case IRON_SWORD -> 6.0D;
            case GOLDEN_SWORD -> 4.0D;
            case DIAMOND_SWORD -> 7.0D;
            case NETHERITE_SWORD -> 8.0D;
            default -> damage;
        };
    }

    private Monster findTarget(Mannequin mannequin, Player owner) {
        double range = Math.max(ATTACK_RANGE, loadDouble(mannequin, "attackRange", ATTACK_RANGE));
        Monster best = null;
        double bestDistance = range * range;
        for (Entity nearby : mannequin.getNearbyEntities(range + 4.0D, range + 2.0D, range + 4.0D)) {
            if (!(nearby instanceof Monster monster) || !monster.isValid() || monster.isDead()) continue;
            if (nearby.getUniqueId().equals(owner.getUniqueId())) continue;
            double distance = nearby.getLocation().distanceSquared(mannequin.getLocation());
            if (distance < bestDistance) {
                best = monster;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void moveTowards(LivingEntity entity, Location target, double speed) {
        Vector delta = target.toVector().subtract(entity.getLocation().toVector());
        delta.setY(Math.max(-0.35D, Math.min(0.35D, delta.getY())));
        if (delta.lengthSquared() < 0.01D) return;
        delta.normalize().multiply(speed);
        entity.setVelocity(delta);
        face(entity, target);
    }

    private static void stop(LivingEntity entity) {
        Vector velocity = entity.getVelocity().multiply(0.25D);
        entity.setVelocity(new Vector(velocity.getX(), entity.getVelocity().getY(), velocity.getZ()));
    }

    private static void face(LivingEntity entity, Location target) {
        Location current = entity.getLocation();
        Vector direction = target.toVector().subtract(current.toVector());
        if (direction.lengthSquared() < 0.0001D) return;
        direction.setY(0.0D);
        if (direction.lengthSquared() < 0.0001D) return;
        current.setDirection(direction);
        entity.setRotation(current.getYaw(), entity.getPitch());
    }

    private String companionId(Mannequin mannequin) {
        return mannequin.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
    }

    private String loadString(Mannequin mannequin, String key) {
        String id = companionId(mannequin);
        if (id == null) return "";
        try {
            JsonObject root = new JsonDataManager(plugin).load("companions.json");
            JsonArray definitions = root.getAsJsonArray("definitions");
            if (definitions == null) return "";
            for (var element : definitions) {
                if (!element.isJsonObject()) continue;
                JsonObject json = element.getAsJsonObject();
                if (!id.equals(string(json, "id", ""))) continue;
                return string(json, key, "");
            }
        } catch (RuntimeException ignored) {
        }
        return "";
    }

    private double loadDouble(Mannequin mannequin, String key, double fallback) {
        String id = companionId(mannequin);
        if (id == null) return fallback;
        try {
            JsonObject root = new JsonDataManager(plugin).load("companions.json");
            JsonArray definitions = root.getAsJsonArray("definitions");
            if (definitions == null) return fallback;
            for (var element : definitions) {
                if (!element.isJsonObject()) continue;
                JsonObject json = element.getAsJsonObject();
                if (id.equals(string(json, "id", "")) && json.has(key) && json.get(key).isJsonPrimitive()) return json.get(key).getAsDouble();
            }
        } catch (RuntimeException ignored) {
        }
        return fallback;
    }

    private int loadInt(Mannequin mannequin, String key, int fallback) {
        return (int) Math.round(loadDouble(mannequin, key, fallback));
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }
}
