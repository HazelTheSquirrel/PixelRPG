package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.api.GuildAPI;
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

/** Runs the single central companion runtime tick for follow, stats, mounts and generic combat. */
public final class CompanionFollowTask implements Runnable {
    private static final double STEP_HEIGHT = 1.05D;
    private static final double JUMP_VELOCITY = 0.42D;

    private final Plugin plugin;
    private final CompanionService companionService;
    private final Map<UUID, UUID> activeEntities;
    private final CompanionRegistry registry;
    private final GuildAPI guildApi;
    private final CompanionStatsCalculator statsCalculator = new CompanionStatsCalculator();
    private final CompanionCombatController combatController = new CompanionCombatController();
    private final CompanionRuntimeRegistry runtimeRegistry = new CompanionRuntimeRegistry();
    private final CompanionMountController mountController = new CompanionMountController();
    private final MannequinCompanionController mannequinController;
    private final Map<UUID, AppliedState> appliedStates = new HashMap<>();
    private final Map<UUID, String> registeredRuntimeCompanions = new HashMap<>();

    public CompanionFollowTask(Plugin plugin, Map<UUID, UUID> activeEntities, CompanionService companionService) {
        this.plugin = plugin;
        this.companionService = companionService;
        this.activeEntities = activeEntities;
        this.registry = CompanionRegistry.load(plugin);
        this.guildApi = plugin.getServer().getServicesManager().load(GuildAPI.class);
        this.mannequinController = new MannequinCompanionController(plugin, companionService, registry);
    }

    public CompanionMountController mountController() { return mountController; }

    @Override
    public void run() {
        for (Map.Entry<UUID, UUID> entry : activeEntities.entrySet()) {
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

            UUID ownerId = owner.getUniqueId();
            String registeredCompanionId = registeredRuntimeCompanions.get(ownerId);
            if (!companionId.equals(registeredCompanionId)) {
                runtimeRegistry.register(new CompanionRuntimeRegistry.CompanionRuntime(ownerId, entity.getUniqueId(), companionId));
                registeredRuntimeCompanions.put(ownerId, companionId);
            }

            if (living instanceof Mannequin mannequin) mannequinController.tick(owner, mannequin);
            updateRuntimeState(owner, living, definition);

            if (definition.mount().enabled() && mountController.isMounted(owner, living)) {
                mountController.tick(owner, living, definition.mount());
                continue;
            }

            double ownerDistanceSquared = living.getLocation().distanceSquared(owner.getLocation());
            double maxOwnerCombatDistance = definition.combat().maxOwnerCombatDistance();
            if (definition.combat().enabled()
                    && Double.isFinite(maxOwnerCombatDistance)
                    && ownerDistanceSquared > maxOwnerCombatDistance * maxOwnerCombatDistance) {
                follow(owner, living, definition.follow());
                continue;
            }

            if (definition.combat().enabled()) {
                LivingEntity target = combatController.tick(owner, living, definition, living.getWorld().getGameTime());
                if (target != null) continue;
            }
            follow(owner, living, definition.follow());
        }
    }

    private void updateRuntimeState(Player owner, LivingEntity entity, CompanionDefinition definition) {
        int level = resolveRuntimeLevel(owner, definition, entity);
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

    private int resolveRuntimeLevel(Player owner, CompanionDefinition definition, LivingEntity entity) {
        if (definition.rarity().isUnique()) {
            return Math.max(1, Math.min(definition.progression().maxLevel(), entity.getPersistentDataContainer()
                    .getOrDefault(RPGKeys.Companion.level(), PersistentDataType.INTEGER, 1)));
        }

        int ownerLevel = guildApi == null ? 1 : guildApi.getLevel(owner.getUniqueId());
        int level = Math.max(1, Math.min(99, ownerLevel));
        entity.getPersistentDataContainer().set(RPGKeys.Companion.level(), PersistentDataType.INTEGER, level);
        return level;
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
        Location current = entity.getLocation();
        Vector delta = target.toVector().subtract(current.toVector());
        delta.setY(Math.max(-0.35D, Math.min(0.35D, delta.getY())));
        if (delta.lengthSquared() < 0.04D) {
            slow(entity);
            return;
        }

        Vector horizontal = delta.clone().setY(0.0D);
        boolean stepUp = shouldStepUp(entity, horizontal);
        boolean fallingBehind = target.getY() > current.getY() + 0.35D;
        boolean falling = entity.getVelocity().getY() < -0.08D;

        if (stepUp && entity.isOnGround()) {
            delta.setY(JUMP_VELOCITY);
        } else if (fallingBehind && entity.isOnGround() && !falling) {
            delta.setY(JUMP_VELOCITY);
        } else {
            delta.setY(Math.max(-0.35D, Math.min(0.35D, delta.getY())));
        }

        delta.setX(horizontal.getX());
        delta.setZ(horizontal.getZ());
        delta.normalize().multiply(Math.max(0.05D, speed));
        if (stepUp || fallingBehind) delta.setY(JUMP_VELOCITY);
        entity.setVelocity(delta);
        entity.setRotation((float) Math.toDegrees(Math.atan2(-delta.getX(), delta.getZ())), entity.getPitch());
    }

    private static boolean shouldStepUp(LivingEntity entity, Vector horizontal) {
        if (horizontal.lengthSquared() < 0.01D) return false;
        Vector direction = horizontal.clone().normalize();
        Location current = entity.getLocation();
        Location oneBlockAhead = current.clone().add(direction.getX() * 0.65D, 0.0D, direction.getZ() * 0.65D);
        Location feet = oneBlockAhead.clone();
        Location head = feet.clone().add(0.0D, STEP_HEIGHT, 0.0D);
        Location landing = feet.clone().add(0.0D, 1.0D, 0.0D);

        return isSolid(feet) && !isSolid(head) && !isSolid(landing);
    }

    private static boolean isSolid(Location location) {
        return location.getBlock().getType().isSolid();
    }

    private static void slow(LivingEntity entity) {
        Vector velocity = entity.getVelocity();
        entity.setVelocity(new Vector(velocity.getX() * 0.35D, velocity.getY(), velocity.getZ() * 0.35D));
    }

    private void removeRuntime(UUID ownerUuid, UUID entityUuid) {
        activeEntities.remove(ownerUuid, entityUuid);
        runtimeRegistry.unregisterOwner(ownerUuid);
        registeredRuntimeCompanions.remove(ownerUuid);
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
