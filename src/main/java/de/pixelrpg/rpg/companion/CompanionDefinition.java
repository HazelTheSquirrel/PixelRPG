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
        CompanionMountDefinition mount,
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
        Objects.requireNonNull(mount, "mount");
        Objects.requireNonNull(equipment, "equipment");
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(abilities, "abilities");
        Objects.requireNonNull(passives, "passives");
        Objects.requireNonNull(unlock, "unlock");
        Objects.requireNonNull(flags, "flags");
    }

    public boolean renameable() { return flags.getOrDefault("renameable", true); }
    public boolean adminOnly() { return flags.getOrDefault("adminOnly", false); }

    /** All companions contribute passive stats; active combat is reserved for the native tamed wolf behavior. */
    public boolean passive() { return true; }

    /** Disables the generic target-acquisition controller; the wolf uses Minecraft's tamed-wolf AI instead. */
    @Override
    public CompanionCombatDefinition combat() {
        return new CompanionCombatDefinition(false, combat.attackRange(), combat.aggroRange(), combat.maxOwnerCombatDistance(),
                combat.attackIntervalTicks(), combat.hostileTargets(), combat.playerTargets(), combat.friendlyTargets(), combat.protectOwner());
    }

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
            double maxOwnerCombatDistance,
            int attackIntervalTicks,
            boolean hostileTargets,
            boolean playerTargets,
            boolean friendlyTargets,
            boolean protectOwner
    ) {}

    public record CompanionMountDefinition(
            boolean enabled,
            Type type,
            double movementSpeed,
            boolean requiresSaddle
    ) {
        public enum Type {
            GROUND,
            UNDERWATER,
            FLYING
        }
    }

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
