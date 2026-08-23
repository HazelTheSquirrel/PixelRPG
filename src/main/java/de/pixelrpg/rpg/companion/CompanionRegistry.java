package de.pixelrpg.rpg.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Loads and validates all companion definitions exactly once during plugin startup. */
public final class CompanionRegistry {
    private final Plugin plugin;
    private final Map<String, CompanionDefinition> definitions;
    private final int maxLevel;
    private final long experienceBase;
    private final long experiencePerLevel;
    private final boolean activeXpOnly;
    private final int maxNameLength;

    private CompanionRegistry(Plugin plugin, Map<String, CompanionDefinition> definitions, int maxLevel,
                              long experienceBase, long experiencePerLevel, boolean activeXpOnly, int maxNameLength) {
        this.plugin = plugin;
        this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        this.maxLevel = maxLevel;
        this.experienceBase = experienceBase;
        this.experiencePerLevel = experiencePerLevel;
        this.activeXpOnly = activeXpOnly;
        this.maxNameLength = maxNameLength;
    }

    public static CompanionRegistry load(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        JsonObject root = new JsonDataManager(plugin).load("companions.json");
        JsonObject progression = object(root, "progression");
        JsonObject xp = object(progression, "xp");
        JsonObject defaults = object(root, "defaults");
        int maxLevel = clampInt(number(progression, "maxLevel", 99), 1, 999);
        long base = Math.max(1L, numberLong(xp, "base", 100L));
        long perLevel = Math.max(0L, numberLong(xp, "perLevel", 8L));
        boolean activeXpOnly = bool(defaults, "activeXpOnly", true);
        int maxNameLength = clampInt(number(defaults, "maxNameLength", 24), 1, 64);

        Map<String, CompanionDefinition> definitions = new LinkedHashMap<>();
        JsonArray array = root.getAsJsonArray("definitions");
        if (array == null) throw new IllegalStateException("companions.json is missing definitions[]");
        for (var element : array) {
            if (!element.isJsonObject()) continue;
            CompanionDefinition definition = parse(element.getAsJsonObject());
            if (definitions.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate companion id: " + definition.id());
            }
        }
        if (definitions.isEmpty()) throw new IllegalStateException("companions.json contains no companion definitions");
        return new CompanionRegistry(plugin, definitions, maxLevel, base, perLevel, activeXpOnly, maxNameLength);
    }

    public Map<String, CompanionDefinition> definitions() { return definitions; }
    public Optional<CompanionDefinition> find(String id) { return Optional.ofNullable(definitions.get(id)); }
    public CompanionDefinition require(String id) { return Objects.requireNonNull(definitions.get(id), "Unknown companion definition: " + id); }
    public int maxLevel() { return maxLevel; }
    public long experienceBase() { return experienceBase; }
    public long experiencePerLevel() { return experiencePerLevel; }
    public boolean activeXpOnly() { return activeXpOnly; }
    public int maxNameLength() { return maxNameLength; }

    private static CompanionDefinition parse(JsonObject json) {
        String id = string(json, "id", "").strip();
        if (id.isBlank()) throw new IllegalStateException("Companion definition has no id");
        String displayName = string(json, "displayName", string(json, "name", id));
        String description = string(json, "description", "");
        CompanionRarity rarity = enumValue(CompanionRarity.class, string(json, "rarity", "COMMON"), CompanionRarity.COMMON);
        EntityType entityType = enumValue(EntityType.class, string(json, "entityType", "WOLF"), EntityType.WOLF);
        CompanionDefinition.CompanionVisualDefinition.VisualType visualType = entityType == EntityType.MANNEQUIN
                ? CompanionDefinition.CompanionVisualDefinition.VisualType.MANNEQUIN
                : CompanionDefinition.CompanionVisualDefinition.VisualType.ENTITY;
        JsonObject visual = object(json, "visual");
        if (visual != null) visualType = enumValue(CompanionDefinition.CompanionVisualDefinition.VisualType.class,
                string(visual, "type", visualType.name()), visualType);
        double scale = number(visual, "scale", number(json, "scale", 1.0D));
        String skin = string(visual, "skinSource", string(json, "skinSource", ""));

        JsonObject stats = object(json, "stats");
        CompanionStats baseStats = new CompanionStats(
                Math.max(0.0D, number(stats, "health", 0.0D)),
                Math.max(0.0D, number(stats, "damage", 0.0D)),
                Math.max(0.0D, number(stats, "movementSpeed", 0.0D)),
                Math.max(0.0D, number(stats, "armor", 0.0D)),
                Math.max(0.0D, number(stats, "critChance", 0.0D)),
                Math.max(0.0D, number(stats, "critDamage", 0.0D)),
                Math.max(0.0D, number(stats, "lifesteal", 0.0D)),
                Math.max(0.0D, number(stats, "abilityDamage", 0.0D)),
                Math.max(0.0D, number(stats, "mana", 0.0D)));

        JsonObject follow = object(json, "follow");
        CompanionDefinition.CompanionFollowDefinition followDefinition = new CompanionDefinition.CompanionFollowDefinition(
                number(follow, "startDistance", 3.0D),
                number(follow, "stopDistance", 1.5D),
                number(follow, "teleportDistance", 30.0D),
                number(follow, "movementSpeed", 0.34D),
                bool(follow, "enabled", bool(json, "passive", true)));

        JsonObject combat = object(json, "combat");
        CompanionDefinition.CompanionCombatDefinition combatDefinition = new CompanionDefinition.CompanionCombatDefinition(
                bool(combat, "enabled", bool(json, "combat", false)),
                Math.max(1.0D, number(combat, "attackRange", number(json, "attackRange", 3.5D))),
                Math.max(1.0D, number(combat, "aggroRange", 12.0D)),
                Math.max(1.0D, number(combat, "maxOwnerCombatDistance", Double.MAX_VALUE)),
                Math.max(1, (int) Math.round(number(combat, "attackIntervalTicks", number(json, "attackIntervalTicks", 20)))),
                bool(combat, "hostileTargets", true),
                bool(combat, "playerTargets", false),
                bool(combat, "friendlyTargets", false),
                bool(combat, "protectOwner", true));

        JsonObject mount = object(json, "mount");
        CompanionDefinition.CompanionMountDefinition mountDefinition = new CompanionDefinition.CompanionMountDefinition(
                bool(mount, "enabled", false),
                enumValue(CompanionDefinition.CompanionMountDefinition.Type.class, string(mount, "type", "GROUND"), CompanionDefinition.CompanionMountDefinition.Type.GROUND),
                Math.max(0.05D, number(mount, "movementSpeed", 0.55D)),
                bool(mount, "requiresSaddle", true));

        JsonObject equipment = object(json, "equipment");
        CompanionDefinition.CompanionEquipmentDefaults equipmentDefaults = new CompanionDefinition.CompanionEquipmentDefaults(
                string(equipment, "helmet", ""), string(equipment, "chestplate", ""), string(equipment, "leggings", ""),
                string(equipment, "boots", ""), string(equipment, "mainHand", ""), string(equipment, "offHand", ""), equipment != null);

        JsonObject progression = object(json, "progression");
        CompanionDefinition.CompanionProgressionDefinition progressionDefinition = new CompanionDefinition.CompanionProgressionDefinition(
                clampInt(number(progression, "maxLevel", 99), 1, 999),
                number(progression, "healthPerLevel", 0.008D),
                number(progression, "damagePerLevel", 0.006D),
                number(progression, "speedPerLevel", 0.0015D),
                number(progression, "healthCap", 1.80D),
                number(progression, "damageCap", 1.60D),
                number(progression, "speedCap", 1.15D));

        Map<String, com.google.gson.JsonObject> abilities = objectMap(json, "abilities");
        Map<String, com.google.gson.JsonObject> passives = objectMap(json, "passives");
        JsonObject unlock = object(json, "unlock");
        CompanionDefinition.CompanionUnlockDefinition unlockDefinition = new CompanionDefinition.CompanionUnlockDefinition(
                string(unlock, "type", bool(json, "adminOnly", false) ? "ADMIN" : "DEFAULT"), unlock == null ? new JsonObject() : unlock.deepCopy());
        Map<String, Boolean> flags = Map.of(
                "renameable", bool(json, "renameable", true),
                "adminOnly", bool(json, "adminOnly", false),
                "passive", bool(json, "passive", true));

        return new CompanionDefinition(id, displayName, description, rarity,
                new CompanionDefinition.CompanionVisualDefinition(visualType, entityType, scale, skin), baseStats,
                followDefinition, combatDefinition, mountDefinition, equipmentDefaults, progressionDefinition, abilities, passives,
                unlockDefinition, flags, json.deepCopy());
    }

    private static Map<String, JsonObject> objectMap(JsonObject parent, String key) {
        JsonObject object = object(parent, key);
        if (object == null) return Map.of();
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (var entry : object.entrySet()) if (entry.getValue().isJsonObject()) result.put(entry.getKey(), entry.getValue().getAsJsonObject().deepCopy());
        return Map.copyOf(result);
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) return null;
        return parent.getAsJsonObject(key);
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsBoolean() : fallback;
    }

    private static double number(JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber()
                ? object.get(key).getAsDouble() : fallback;
    }

    private static long numberLong(JsonObject object, String key, long fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber()
                ? object.get(key).getAsLong() : fallback;
    }

    private static int clampInt(double value, int min, int max) {
        return (int) Math.max(min, Math.min(max, Math.round(value)));
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try { return Enum.valueOf(type, value.toUpperCase()); }
        catch (IllegalArgumentException exception) { return fallback; }
    }
}
