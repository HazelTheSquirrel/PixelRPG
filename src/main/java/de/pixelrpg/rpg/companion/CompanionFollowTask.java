package de.pixelrpg.rpg.companion;

import com.destroystokyo.paper.entity.Pathfinder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps active companions near their owners and delegates Mannequins to their dedicated controller. */
public final class CompanionFollowTask implements Runnable {
    private static final double FOLLOW_DISTANCE_SQUARED = 9.0D;
    private static final double FOLLOW_OFFSET = 2.0D;
    private static final double TELEPORT_RECOVERY_DISTANCE_SQUARED = 576.0D;
    private static final double FOLLOW_SPEED = 1.15D;

    private final Plugin plugin;
    private final Map<UUID, UUID> activeEntities;
    private final MannequinCompanionController mannequinController;
    private final Map<String, CompanionVisualDefinition> definitionsById = new HashMap<>();
    private final Map<UUID, AppliedCompanionState> appliedStates = new HashMap<>();
    private CompanionLevelScaling levelScaling = CompanionLevelScaling.defaults();

    public CompanionFollowTask(Plugin plugin, Map<UUID, UUID> activeEntities, CompanionService companionService) {
        this.plugin = plugin;
        this.activeEntities = activeEntities;
        this.mannequinController = new MannequinCompanionController(plugin, companionService);
        reloadConfiguration();
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, UUID> entry : activeEntities.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            Entity companion = plugin.getServer().getEntity(entry.getValue());

            if (player == null || companion == null || !companion.isValid()) {
                activeEntities.remove(entry.getKey(), entry.getValue());
                appliedStates.remove(entry.getValue());
                continue;
            }

            if (companion instanceof Mannequin mannequin) {
                mannequinController.tick(player, mannequin);
                continue;
            }

            if (companion instanceof LivingEntity living) applyConfiguredAttributes(living);

            Location playerLocation = player.getLocation();
            if (companion.getWorld() != player.getWorld()) {
                companion.teleport(playerLocation);
                continue;
            }

            double distanceSquared = companion.getLocation().distanceSquared(playerLocation);
            if (distanceSquared <= FOLLOW_DISTANCE_SQUARED) {
                companion.setVelocity(companion.getVelocity().multiply(0.35D));
                continue;
            }

            if (distanceSquared > TELEPORT_RECOVERY_DISTANCE_SQUARED) {
                companion.teleport(playerLocation.clone().add(1.0D, 0.0D, 1.0D));
                companion.setVelocity(new Vector());
                continue;
            }

            Vector direction = playerLocation.getDirection().clone();
            direction.setY(0.0D);
            if (direction.lengthSquared() < 0.001D) direction.setZ(1.0D);
            direction.normalize().multiply(FOLLOW_OFFSET);
            Location target = playerLocation.clone().subtract(direction);
            target.setY(playerLocation.getY());

            if (companion instanceof Mob mob) moveMob(mob, target);
            else moveEntity(companion, target, 0.34D);
        }
    }

    private static void moveMob(Mob mob, Location target) {
        mob.setAware(true);
        Pathfinder pathfinder = mob.getPathfinder();
        Pathfinder.PathResult path = pathfinder.findPath(target);
        if (path != null) pathfinder.moveTo(path, FOLLOW_SPEED);
        else moveEntity(mob, target, 0.34D);
    }

    private static void moveEntity(Entity entity, Location target, double speed) {
        Vector delta = target.toVector().subtract(entity.getLocation().toVector());
        delta.setY(Math.max(-0.35D, Math.min(0.35D, delta.getY())));
        if (delta.lengthSquared() < 0.04D) {
            entity.setVelocity(entity.getVelocity().multiply(0.35D));
            return;
        }
        delta.normalize().multiply(speed);
        entity.setVelocity(delta);
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.getX(), delta.getZ()));
        entity.setRotation(yaw, entity.getPitch());
    }

    private void applyConfiguredAttributes(LivingEntity entity) {
        String id = entity.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
        if (id == null || id.isBlank()) return;
        CompanionVisualDefinition definition = definitionsById.get(id);
        if (definition == null) return;
        int level = Math.max(1, entity.getPersistentDataContainer().getOrDefault(RPGKeys.Companion.level(), PersistentDataType.INTEGER, 1));
        String rarity = entity.getPersistentDataContainer().getOrDefault(RPGKeys.Companion.rarity(), PersistentDataType.STRING, definition.rarity());
        double rarityMultiplier;
        try {
            rarityMultiplier = CompanionRarity.valueOf(rarity.toUpperCase()).statMultiplier();
        } catch (IllegalArgumentException exception) {
            rarityMultiplier = 1.0D;
        }

        AppliedCompanionState state = appliedStates.get(entity.getUniqueId());
        if (state == null) {
            state = AppliedCompanionState.capture(entity, level, rarity);
            appliedStates.put(entity.getUniqueId(), state);
        } else if (state.level() != level || !state.rarity().equalsIgnoreCase(rarity)) {
            state.restore(entity);
            state = AppliedCompanionState.capture(entity, level, rarity);
            appliedStates.put(entity.getUniqueId(), state);
        }
        AttributeInstance scale = entity.getAttribute(Attribute.SCALE);
        if (scale != null && Double.isFinite(definition.scale()) && definition.scale() > 0.0D) scale.setBaseValue(definition.scale());
        double healthMultiplier = capped(rarityMultiplier * levelMultiplier(levelScaling.healthPerLevel(), level), levelScaling.healthCap());
        double damageMultiplier = capped(rarityMultiplier * levelMultiplier(levelScaling.damagePerLevel(), level), levelScaling.damageCap());
        double speedMultiplier = capped(rarityMultiplier * levelMultiplier(levelScaling.speedPerLevel(), level), levelScaling.speedCap());
        setFromBase(entity, Attribute.MAX_HEALTH, state.baseHealth(), healthMultiplier);
        setFromBase(entity, Attribute.ATTACK_DAMAGE, state.baseDamage(), damageMultiplier);
        setFromBase(entity, Attribute.MOVEMENT_SPEED, state.baseSpeed(), speedMultiplier);
    }

    private static double levelMultiplier(double perLevel, int level) { return 1.0D + Math.max(0, level - 1) * Math.max(0.0D, perLevel); }
    private static double capped(double value, double cap) { return Math.min(Math.max(0.01D, value), Math.max(0.01D, cap)); }
    private static void setFromBase(LivingEntity entity, Attribute attribute, double baseValue, double multiplier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null || !Double.isFinite(baseValue) || !Double.isFinite(multiplier) || multiplier <= 0.0D) return;
        instance.setBaseValue(Math.max(0.01D, baseValue * multiplier));
        if (attribute == Attribute.MAX_HEALTH) entity.setHealth(Math.min(entity.getHealth(), instance.getValue()));
    }

    private void reloadConfiguration() {
        loadVisualDefinitions();
        loadLevelScaling();
    }

    private void loadVisualDefinitions() {
        definitionsById.clear();
        try {
            JsonObject root = new JsonDataManager(plugin).load("companions.json");
            JsonArray definitions = root.getAsJsonArray("definitions");
            if (definitions == null) return;
            for (var element : definitions) {
                if (!element.isJsonObject()) continue;
                JsonObject json = element.getAsJsonObject();
                String id = string(json, "id", "").strip();
                if (id.isBlank()) continue;
                definitionsById.put(id, new CompanionVisualDefinition(string(json, "entityType", ""), string(json, "rarity", "COMMON").toUpperCase(), number(json, "scale", 1.0D)));
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load companion visual configuration: " + exception.getMessage());
        }
    }

    private void loadLevelScaling() {
        try {
            JsonObject root = new JsonDataManager(plugin).load("mob-scaling.json");
            JsonObject companions = root.getAsJsonObject("companions");
            JsonObject caps = companions == null ? null : companions.getAsJsonObject("caps");
            levelScaling = new CompanionLevelScaling(number(companions, "healthPerLevel", 0.008D), number(companions, "damagePerLevel", 0.006D), number(companions, "speedPerLevel", 0.0015D), number(caps, "health", 1.80D), number(caps, "damage", 1.60D), number(caps, "speed", 1.15D));
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load companion level scaling: " + exception.getMessage());
            levelScaling = CompanionLevelScaling.defaults();
        }
    }

    private static String string(JsonObject object, String key, String fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback; }
    private static double number(JsonObject object, String key, double fallback) { return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber() ? object.get(key).getAsDouble() : fallback; }

    private record CompanionVisualDefinition(String entityType, String rarity, double scale) {}
    private record AppliedCompanionState(int level, String rarity, double baseHealth, double baseDamage, double baseSpeed) {
        private static AppliedCompanionState capture(LivingEntity entity, int level, String rarity) { return new AppliedCompanionState(level, rarity, base(entity, Attribute.MAX_HEALTH), base(entity, Attribute.ATTACK_DAMAGE), base(entity, Attribute.MOVEMENT_SPEED)); }
        private void restore(LivingEntity entity) { restore(entity, Attribute.MAX_HEALTH, baseHealth); restore(entity, Attribute.ATTACK_DAMAGE, baseDamage); restore(entity, Attribute.MOVEMENT_SPEED, baseSpeed); }
        private static double base(LivingEntity entity, Attribute attribute) { AttributeInstance instance = entity.getAttribute(attribute); return instance == null ? 1.0D : instance.getBaseValue(); }
        private static void restore(LivingEntity entity, Attribute attribute, double value) { AttributeInstance instance = entity.getAttribute(attribute); if (instance != null && Double.isFinite(value) && value > 0.0D) instance.setBaseValue(value); }
    }
    private record CompanionLevelScaling(double healthPerLevel, double damagePerLevel, double speedPerLevel, double healthCap, double damageCap, double speedCap) {
        private static CompanionLevelScaling defaults() { return new CompanionLevelScaling(0.008D, 0.006D, 0.0015D, 1.80D, 1.60D, 1.15D); }
    }
}
