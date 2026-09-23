package de.pixelrpg.rpg.companion;

import com.google.gson.JsonObject;
import org.bukkit.entity.EntityType;

import java.util.Map;
import java.util.Objects;

/** Immutable static companion definition loaded from companions.json. */
public record CompanionDefinition(
        String id, String displayName, String description, CompanionRarity rarity, EntityType entityType,
        double scale, boolean renameable, boolean adminOnly, boolean passive,
        Follow follow, Combat combat, Mount mount, Progression progression,
        EquipmentDefaults equipment, Unlock unlock, JsonObject visual
) {
    public CompanionDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(displayName); Objects.requireNonNull(rarity);
        Objects.requireNonNull(entityType); Objects.requireNonNull(follow); Objects.requireNonNull(combat);
        Objects.requireNonNull(mount); Objects.requireNonNull(progression); Objects.requireNonNull(equipment);
        Objects.requireNonNull(unlock);
    }
    public record Follow(boolean enabled, double startDistance, double stopDistance, double teleportDistance, double speed) {}
    public record Combat(boolean enabled, double attackRange, double aggroRange, double maxOwnerDistance, int intervalTicks, boolean hostileTargets, boolean playerTargets, boolean friendlyTargets) {}
    public record Mount(boolean enabled, Type type, double speed, boolean requiresSaddle) { public enum Type { GROUND, UNDERWATER, FLYING } }
    public record Progression(int maxLevel, double healthPerLevel, double damagePerLevel, double speedPerLevel, double healthCap, double damageCap, double speedCap) {}
    public record EquipmentDefaults(String helmet, String chestplate, String leggings, String boots, String mainHand, String offHand, boolean enabled) {}
    public record Unlock(String type, String bossId) {}
}
