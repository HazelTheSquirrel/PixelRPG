package de.pixelrpg.rpg.companion;

import com.google.gson.JsonObject;
import org.bukkit.entity.EntityType;

import java.util.Map;
import java.util.Objects;

/** Immutable, data-driven definition loaded once from companions.json. */
public record CompanionDefinition(
        String id,
        String displayName,
        String description,
        CompanionRarity rarity,
        CompanionVisualDefinition visual,
        CompanionStats baseStats,
        CompanionFollowDefinition follow,
        CompanionCombatDefinition combat,
        CompanionEquipmentDefaults equipment,
        CompanionProgressionDefinition progression,
        Map<String, JsonObject> abilities,
        Map<String, JsonObject> passives,
        CompanionUnlockDefinition unlock,
        Map<String, Boolean> flags,
        JsonObject raw
) {
    public CompanionDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(visual, "visual");
        Objects.requireNonNull(baseStats, "baseStats");
        Objects.requireNonNull(follow, "follow");
        Objects.requireNonNull(combat, "combat");
        Objects.requireNonNull(equipment, "equipment");
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(abilities, "abilities");
        Objects.requireNonNull(passives, "passives");
        Objects.requireNonNull(unlock, "unlock");
        Objects.requireNonNull(flags, "flags");
    }

    public boolean renameable() { return flags.getOrDefault("renameable", true); }
    public boolean adminOnly() { return flags.getOrDefault("adminOnly", false); }
    public boolean passive() { return flags.getOrDefault("passive", true); }

    public record CompanionVisualDefinition(
            VisualType type,
            EntityType entityType,
            double scale,
            String skinSource
    ) {
        public enum VisualType { ENTITY, MANNEQUIN }
    }

    public record CompanionFollowDefinition(
            double startDistance,
            double stopDistance,
            double teleportDistance,
            double movementSpeed,
            boolean enabled
    ) {}

    public record CompanionCombatDefinition(
            boolean enabled,
            double attackRange,
            double aggroRange,
            int attackIntervalTicks,
            boolean hostileTargets,
            boolean playerTargets,
            boolean friendlyTargets,
            boolean protectOwner
    ) {}

    public record CompanionEquipmentDefaults(
            String helmet,
            String chestplate,
            String leggings,
            String boots,
            String mainHand,
            String offHand,
            boolean enabled
    ) {}

    public record CompanionProgressionDefinition(
            int maxLevel,
            double healthPerLevel,
            double damagePerLevel,
            double speedPerLevel,
            double healthCap,
            double damageCap,
            double speedCap
    ) {}

    public record CompanionUnlockDefinition(String type, JsonObject data) {}
}
