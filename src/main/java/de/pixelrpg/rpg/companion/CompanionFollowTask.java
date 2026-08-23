package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Runs the single central companion runtime tick for follow, stats and generic combat. */
public final class CompanionFollowTask implements Runnable {
    private final Plugin plugin;
    private final CompanionService companionService;
    private final Map<UUID, UUID> activeEntities;
    private final CompanionRegistry registry;
    private final CompanionStatsCalculator statsCalculator = new CompanionStatsCalculator();
    private final CompanionCombatController combatController = new CompanionCombatController();
    private final CompanionRuntimeRegistry runtimeRegistry = new CompanionRuntimeRegistry();
    private final MannequinCompanionController mannequinController;
    private final Map<UUID, AppliedState> appliedStates = new HashMap<>();

    public CompanionFollowTask(Plugin plugin, Map<UUID, UUID> activeEntities, CompanionService companionService) {
        this.plugin = plugin;
        this.companionService = companionService;
        this.activeEntities = activeEntities;
        this.registry = CompanionRegistry.load(plugin);
        this.mannequinController = new MannequinCompanionController(plugin, companionService);
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, UUID> entry : Map.copyOf(activeEntities).entrySet()) {
            Player owner = plugin.getServer().getPlayer(entry.getKey());
            Entity entity = plugin.getServer().getEntity(entry.getValue());
            if (owner == null || entity == null || !entity.isValid() || !(entity instanceof LivingEntity living)) {
                removeRuntime(entry.getKey(), entry.getValue());
                continue;
            }

            String companionId = living.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
            if (companionId == null || companionId.isBlank()) {
                removeRuntime(entry.getKey(), entry.getValue());
                continue;
            }
            CompanionDefinition definition = registry.find(companionId).orElse(null);
            if (definition == null) {
                removeRuntime(entry.getKey(), entry.getValue());
                continue;
            }

            runtimeRegistry.register(new CompanionRuntimeRegistry.CompanionRuntime(owner.getUniqueId(), entity.getUniqueId(), companionId));
            if (living instanceof Mannequin mannequin) mannequinController.tick(owner, mannequin);
            updateRuntimeState(owner, living, definition);

            if (definition.combat().enabled()) {
                combatController.tick(owner, living, definition, living.getWorld().getGameTime());
                if (living.getLocation().distanceSquared(owner.getLocation()) <= definition.follow().startDistance() * definition.follow().startDistance()) continue;
            }
            follow(owner, living, definition.follow());
        }
    }

    private void updateRuntimeState(Player owner, LivingEntity entity, CompanionDefinition definition) {
        int level = Math.max(1, entity.getPersistentDataContainer().getOrDefault(RPGKeys.Companion.level(), PersistentDataType.INTEGER, 1));
        String rarity = entity.getPersistentDataContainer().getOrDefault(RPGKeys.Companion.rarity(), PersistentDataType.STRING, definition.rarity().name());
        int equipmentHash = equipmentHash(owner.getUniqueId(), definition.id());
        AppliedState current = appliedStates.get(entity.getUniqueId());
        if (current != null && current.level() == level && current.rarity().equals(rarity) && current.equipmentHash() == equipmentHash) return;

        if (current == null) current = AppliedState.capture(entity);
        else current.restore(entity);
        AppliedState base = current;
        companionService.applyEquipmentToEntity(owner.getUniqueId(), definition.id(), entity);
        CompanionInstance instance = new CompanionInstance(owner.getUniqueId(), definition.id(), level, 0L, true, true, CompanionEquipment.empty());
        CompanionStats stats = statsCalculator.calculate(definition, instance, entity);
        statsCalculator.apply(stats, entity);
        applyScale(entity, definition.visual().scale());
        appliedStates.put(entity.getUniqueId(), new AppliedState(level, rarity, equipmentHash, base.baseHealth(), base.baseDamage(), base.baseSpeed()));
    }

    private int equipmentHash(UUID ownerUuid, String companionId) {
        CompanionEquipment equipment = companionService.getEquipment(ownerUuid, companionId);
        return java.util.Objects.hash(equipment.helmet(), equipment.chestplate(), equipment.leggings(), equipment.boots(), equipment.mainHand(), equipment.offHand());
    }

    private static void applyScale(LivingEntity entity, double scale) {
        AttributeInstance attribute = entity.getAttribute(Attribute.SCALE);
        if (attribute != null && Double.isFinite(scale) && scale > 0.0D) attribute.setBaseValue(scale);
    }

    private static void follow(Player owner, LivingEntity companion, CompanionDefinition.CompanionFollowDefinition follow) {
        if (!follow.enabled()) return;
        Location playerLocation = owner.getLocation();
        if (companion.getWorld() != owner.getWorld()) {
            companion.teleport(playerLocation.clone().add(1.0D, 0.0D, 1.0D));
            companion.setVelocity(new Vector());
            return;
        }

        double distanceSquared = companion.getLocation().distanceSquared(playerLocation);
        double teleportDistance = Math.max(follow.teleportDistance(), follow.startDistance() + 1.0D);
        if (distanceSquared >= teleportDistance * teleportDistance) {
            companion.teleport(playerLocation.clone().add(1.0D, 0.0D, 1.0D));
            companion.setVelocity(new Vector());
            return;
        }
        if (distanceSquared <= follow.startDistance() * follow.startDistance()) {
            slow(companion);
            return;
        }

        Vector direction = playerLocation.getDirection().clone().setY(0.0D);
        if (direction.lengthSquared() < 0.001D) direction.setZ(1.0D);
        direction.normalize().multiply(-Math.max(1.0D, follow.stopDistance()));
        Location target = playerLocation.clone().add(direction);
        moveTowards(companion, target, follow.movementSpeed());
    }

    private static void moveTowards(LivingEntity entity, Location target, double speed) {
        Vector delta = target.toVector().subtract(entity.getLocation().toVector());
        delta.setY(Math.max(-0.35D, Math.min(0.35D, delta.getY())));
        if (delta.lengthSquared() < 0.04D) {
            slow(entity);
            return;
        }
        delta.normalize().multiply(Math.max(0.05D, speed));
        entity.setVelocity(delta);
        entity.setRotation((float) Math.toDegrees(Math.atan2(-delta.getX(), delta.getZ())), entity.getPitch());
    }

    private static void slow(LivingEntity entity) {
        Vector velocity = entity.getVelocity();
        entity.setVelocity(new Vector(velocity.getX() * 0.35D, velocity.getY(), velocity.getZ() * 0.35D));
    }

    private void removeRuntime(UUID ownerUuid, UUID entityUuid) {
        activeEntities.remove(ownerUuid, entityUuid);
        runtimeRegistry.unregisterOwner(ownerUuid);
        Entity entity = plugin.getServer().getEntity(entityUuid);
        if (entity instanceof LivingEntity living) combatController.clear(living);
        appliedStates.remove(entityUuid);
    }

    private record AppliedState(int level, String rarity, int equipmentHash, double baseHealth, double baseDamage, double baseSpeed) {
        private static AppliedState capture(LivingEntity entity) {
            return new AppliedState(1, "", 0, base(entity, Attribute.MAX_HEALTH), base(entity, Attribute.ATTACK_DAMAGE), base(entity, Attribute.MOVEMENT_SPEED));
        }

        private void restore(LivingEntity entity) {
            restore(entity, Attribute.MAX_HEALTH, baseHealth);
            restore(entity, Attribute.ATTACK_DAMAGE, baseDamage);
            restore(entity, Attribute.MOVEMENT_SPEED, baseSpeed);
        }

        private static double base(LivingEntity entity, Attribute attribute) {
            AttributeInstance instance = entity.getAttribute(attribute);
            return instance == null ? 1.0D : instance.getBaseValue();
        }

        private static void restore(LivingEntity entity, Attribute attribute, double value) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null && Double.isFinite(value) && value > 0.0D) instance.setBaseValue(value);
        }
    }
}
