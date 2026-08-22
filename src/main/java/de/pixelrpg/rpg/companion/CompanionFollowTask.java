package de.pixelrpg.rpg.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps active passive companions near their owning player without enabling combat AI. */
public final class CompanionFollowTask implements Runnable {
    private static final double FOLLOW_DISTANCE_SQUARED = 9.0D;
    private static final double FOLLOW_OFFSET = 2.0D;

    private final Plugin plugin;
    private final Map<UUID, UUID> activeEntities;
    private final Map<String, CompanionVisualDefinition> definitionsByName = new HashMap<>();
    private final Map<String, CompanionStatDefinition> statsByRarity = new HashMap<>();

    public CompanionFollowTask(Plugin plugin, Map<UUID, UUID> activeEntities) {
        this.plugin = plugin;
        this.activeEntities = activeEntities;
        loadVisualDefinitions();
        loadStatDefinitions();
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, UUID> entry : activeEntities.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            Entity companion = plugin.getServer().getEntity(entry.getValue());

            if (player == null || companion == null || !companion.isValid()) {
                activeEntities.remove(entry.getKey(), entry.getValue());
                continue;
            }

            if (companion instanceof LivingEntity living) applyConfiguredAttributes(living);

            Location playerLocation = player.getLocation();
            if (companion.getWorld() != player.getWorld()) {
                companion.teleport(playerLocation);
                continue;
            }

            if (companion.getLocation().distanceSquared(playerLocation) <= FOLLOW_DISTANCE_SQUARED) continue;

            var direction = playerLocation.getDirection();
            if (direction.lengthSquared() < 0.001D) direction.setX(0.0D).setZ(1.0D);
            direction.normalize().multiply(FOLLOW_OFFSET);

            Location target = playerLocation.clone().subtract(direction);
            target.setY(playerLocation.getY());
            companion.teleport(target);
        }
    }

    private void applyConfiguredAttributes(LivingEntity entity) {
        CompanionVisualDefinition definition = definitionsByName.get(entity.getCustomName());
        if (definition == null) {
            definition = definitionsByName.values().stream()
                    .filter(candidate -> candidate.entityType().equalsIgnoreCase(entity.getType().name()))
                    .findFirst()
                    .orElse(null);
        }
        if (definition == null) return;

        AttributeInstance scale = entity.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(definition.scale());

        CompanionStatDefinition stats = statsByRarity.get(definition.rarity());
        if (stats == null) return;

        setMultiplier(entity, Attribute.MAX_HEALTH, stats.healthMultiplier());
        setMultiplier(entity, Attribute.ATTACK_DAMAGE, stats.damageMultiplier());
        setMultiplier(entity, Attribute.MOVEMENT_SPEED, stats.speedMultiplier());
    }

    private static void setMultiplier(LivingEntity entity, Attribute attribute, double multiplier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null || !Double.isFinite(multiplier) || multiplier <= 0.0D) return;
        instance.setBaseValue(Math.max(0.01D, instance.getBaseValue() * multiplier));
        if (attribute == Attribute.MAX_HEALTH) entity.setHealth(Math.min(entity.getHealth(), instance.getValue()));
    }

    private void loadVisualDefinitions() {
        try {
            JsonObject root = new JsonDataManager(plugin).load("companions.json");
            JsonArray definitions = root.getAsJsonArray("definitions");
            if (definitions == null) return;
            for (var element : definitions) {
                if (!element.isJsonObject()) continue;
                JsonObject json = element.getAsJsonObject();
                String name = string(json, "name", "");
                if (name.isBlank()) continue;
                definitionsByName.put(name, new CompanionVisualDefinition(
                        string(json, "entityType", ""),
                        string(json, "rarity", "COMMON").toUpperCase(),
                        number(json, "scale", 1.0D)));
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load companion visual configuration: " + exception.getMessage());
        }
    }

    private void loadStatDefinitions() {
        try {
            JsonObject root = new JsonDataManager(plugin).load("companion-stats.json");
            JsonObject rarities = root.getAsJsonObject("rarities");
            if (rarities == null) return;
            for (String rarity : rarities.keySet()) {
                JsonObject json = rarities.getAsJsonObject(rarity);
                statsByRarity.put(rarity.toUpperCase(), new CompanionStatDefinition(
                        number(json, "health", 1.0D),
                        number(json, "damage", 1.0D),
                        number(json, "speed", 1.0D)));
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load companion stat configuration: " + exception.getMessage());
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }

    private static double number(JsonObject object, String key, double fallback) {
        return object.has(key) && object.get(key).isNumber() ? object.get(key).getAsDouble() : fallback;
    }

    private record CompanionVisualDefinition(String entityType, String rarity, double scale) {
    }

    private record CompanionStatDefinition(double healthMultiplier, double damageMultiplier, double speedMultiplier) {
    }
}
