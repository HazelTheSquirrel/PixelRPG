package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.CharacterStatType;
import de.pixelrpg.rpg.companion.Companion;
import de.pixelrpg.rpg.companion.CompanionDefinition;
import de.pixelrpg.rpg.companion.CompanionPassiveStats;
import de.pixelrpg.rpg.companion.CompanionService;
import de.pixelrpg.rpg.companion.CompanionStats;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.equipment.EquipmentSetService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Deterministic character-stat calculator and runtime cache. All values use PixelRPG units. */
public final class StatEngine {
    private static final double BASE_HP = 100.0D;
    private static final double BASE_CRIT_CHANCE = 0.0D;
    private static final double BASE_CRIT_DAMAGE_MULTIPLIER = 2.0D;

    private static final double MAX_HP = 200.0D;
    private static final double MAX_ARMOR = 20.0D;
    private static final double MAX_MOVEMENT_SPEED_PERCENT = 30.0D;
    private static final double MAX_REACH = 5.0D;
    private static final double MAX_CRIT_CHANCE = 100.0D;
    private static final double MAX_CRIT_DAMAGE_MULTIPLIER = 3.0D;
    private static final double MAX_LIFESTEAL = 8.0D;
    private static final double MAX_ATTACK_POWER = 15.0D;
    private static final double PIXELRPG_HP_PER_MINECRAFT_HEALTH = 5.0D;

    public record CachedStats(double maxHealth, double armor, double movementSpeedBonus, double blockReach,
                              double entityReach, double critChance, double critDamageMultiplier,
                              double lifestealBonus, double attackPower) {
        public static final CachedStats EMPTY = new CachedStats(BASE_HP, 0.0D, 0.0D, 0.0D, 0.0D,
                BASE_CRIT_CHANCE, BASE_CRIT_DAMAGE_MULTIPLIER, 0.0D, 0.0D);

        public double reach() {
            return Math.max(blockReach, entityReach);
        }
    }

    private final PlayerProfileManager profileManager;
    private final EquipmentSetService equipmentSets;
    private final Map<UUID, CachedStats> cache = new ConcurrentHashMap<>();

    public StatEngine(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
        PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
        this.equipmentSets = plugin == null ? null : new EquipmentSetService(plugin);
    }

    public CachedStats getCachedStats(UUID uuid) {
        return cache.getOrDefault(uuid, CachedStats.EMPTY);
    }

    public void recalculate(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            clear(player);
            return;
        }

        int playerLevel = Math.clamp(profile.getLevel(), 1, 99);
        double itemArmor = sum(player, playerLevel, RPGKeys.Item.armorValue());
        double itemHealth = sum(player, playerLevel, RPGKeys.Item.healthBonus());
        double itemCritChance = sum(player, playerLevel, RPGKeys.Item.critChance());
        double itemCritDamage = sum(player, playerLevel, RPGKeys.Item.critDamage());
        double itemLifesteal = sum(player, playerLevel, RPGKeys.Item.lifestealPercent());
        double itemReach = sum(player, playerLevel, RPGKeys.Item.reachBonus());
        double itemMovementSpeed = sum(player, playerLevel, RPGKeys.Item.movementSpeed());
        double itemAttackPower = sum(player, playerLevel, RPGKeys.Item.attackPower());

        Map<String, Double> setBonus = equipmentSets == null ? Map.of() : equipmentSets.bonuses(player, playerLevel);
        itemHealth += setBonus.getOrDefault("HP", 0.0D);
        itemArmor += setBonus.getOrDefault("ARMOR", 0.0D);
        itemMovementSpeed += setBonus.getOrDefault("MOVEMENT_SPEED", 0.0D);
        itemReach += setBonus.getOrDefault("REACH", 0.0D);
        itemCritChance += setBonus.getOrDefault("CRIT", 0.0D) + setBonus.getOrDefault("CRIT_CHANCE", 0.0D);
        itemCritDamage += setBonus.getOrDefault("CRIT_DAMAGE", 0.0D);
        itemLifesteal += setBonus.getOrDefault("LIFESTEAL", 0.0D);
        itemAttackPower += setBonus.getOrDefault("ATTACK_POWER", 0.0D);

        CompanionPassiveStats companion = activeCompanionPassiveStats(player.getUniqueId());
        double maxHealth = clamp(BASE_HP + itemHealth + companion.hp(), BASE_HP, MAX_HP);
        double armor = clamp(itemArmor + companion.armor(), 0.0D, MAX_ARMOR);
        double movementSpeedBonus = clamp((itemMovementSpeed + companion.movementSpeed()) * 100.0D,
                0.0D, MAX_MOVEMENT_SPEED_PERCENT);
        double blockReach = clamp(itemReach + companion.reach(), 0.0D, MAX_REACH);
        double entityReach = clamp(itemReach + companion.reach(), 0.0D, MAX_REACH);
        double critChance = clamp(itemCritChance + companion.crit(), 0.0D, MAX_CRIT_CHANCE);
        double critDamageMultiplier = clamp(BASE_CRIT_DAMAGE_MULTIPLIER + itemCritDamage + companion.critDamage(),
                BASE_CRIT_DAMAGE_MULTIPLIER, MAX_CRIT_DAMAGE_MULTIPLIER);
        double lifestealBonus = clamp(itemLifesteal + companion.lifesteal(), 0.0D, MAX_LIFESTEAL);
        double attackPower = clamp(itemAttackPower + companion.damage() + companion.attackPower(), 0.0D, MAX_ATTACK_POWER);

        CachedStats stats = new CachedStats(maxHealth, armor, movementSpeedBonus, blockReach, entityReach,
                critChance, critDamageMultiplier, lifestealBonus, attackPower);
        cache.put(player.getUniqueId(), stats);

        double minecraftMaxHealth = maxHealth / PIXELRPG_HP_PER_MINECRAFT_HEALTH;
        applyModifier(player, Attribute.MAX_HEALTH, RPGKeys.Stats.maxHealth(), minecraftMaxHealth - 20.0D);
        applyModifier(player, Attribute.ARMOR, RPGKeys.Stats.armor(), armor);
        applyModifier(player, Attribute.MOVEMENT_SPEED, RPGKeys.Stats.movementSpeed(), movementSpeedBonus / 100.0D,
                AttributeModifier.Operation.ADD_SCALAR);
        applyModifier(player, Attribute.BLOCK_INTERACTION_RANGE, RPGKeys.Stats.blockRange(), blockReach);
        applyModifier(player, Attribute.ENTITY_INTERACTION_RANGE, RPGKeys.Stats.entityRange(), entityReach);

        AttributeInstance healthInstance = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthInstance != null) {
            double minecraftMax = Math.max(1.0D, healthInstance.getValue());
            player.setHealthScaled(true);
            player.setHealthScale(Math.min(40.0D, minecraftMax));
            if (player.getHealth() > minecraftMax) player.setHealth(minecraftMax);
        }
    }

    public void clear(Player player) {
        cache.remove(player.getUniqueId());
        removeModifier(player, Attribute.MAX_HEALTH, RPGKeys.Stats.maxHealth());
        removeModifier(player, Attribute.ARMOR, RPGKeys.Stats.armor());
        removeModifier(player, Attribute.MOVEMENT_SPEED, RPGKeys.Stats.movementSpeed());
        removeModifier(player, Attribute.BLOCK_INTERACTION_RANGE, RPGKeys.Stats.blockRange());
        removeModifier(player, Attribute.ENTITY_INTERACTION_RANGE, RPGKeys.Stats.entityRange());
        player.setHealthScaled(false);
    }

    public double getCharacterStat(UUID uuid, CharacterStatType type) {
        CachedStats stats = getCachedStats(uuid);
        return switch (type) {
            case HP -> stats.maxHealth();
            case ARMOR -> stats.armor();
            case MOVEMENT_SPEED -> stats.movementSpeedBonus();
            case REACH -> stats.reach();
            case CRIT -> stats.critChance();
            case CRIT_DAMAGE -> stats.critDamageMultiplier();
            case LIFESTEAL -> stats.lifestealBonus();
            case ATTACK_POWER -> stats.attackPower();
        };
    }

    private CompanionPassiveStats activeCompanionPassiveStats(UUID playerId) {
        PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
        if (plugin == null) return CompanionPassiveStats.EMPTY;
        CompanionService service = plugin.getCompanionService();
        if (service == null) return CompanionPassiveStats.EMPTY;
        UUID activeEntityId = service.getActiveEntity(playerId);
        if (activeEntityId == null) return CompanionPassiveStats.EMPTY;
        Entity activeEntity = Bukkit.getEntity(activeEntityId);
        if (activeEntity == null || activeEntity.isDead()) return CompanionPassiveStats.EMPTY;
        Companion active = service.getActive(playerId);
        if (active == null) return CompanionPassiveStats.EMPTY;
        CompanionDefinition definition = service.definition(active.id());
        if (!definition.passive()) return CompanionPassiveStats.EMPTY;
        return fromMainBranchCompanionStats(definition);
    }

    /** Mirrors the existing rarity-based companion bonus model without introducing additional stat types. */
    private CompanionPassiveStats fromMainBranchCompanionStats(CompanionDefinition definition) {
        CompanionStats configured = definition.baseStats();
        double tier = switch (definition.rarity()) {
            case COMMON -> 1.0D;
            case UNCOMMON -> 2.0D;
            case RARE -> 3.5D;
            case EPIC -> 5.0D;
            case LEGENDARY -> 7.5D;
            case UNIQUE -> 0.0D;
        };
        return new CompanionPassiveStats(
                configured.health() > 0.0D ? configured.health() : tier * 2.0D,
                configured.armor() > 0.0D ? configured.armor() : tier,
                configured.movementSpeed(),
                0.0D,
                0.0D,
                configured.critChance() > 0.0D ? configured.critChance() : tier * 0.5D,
                configured.critDamage() > 0.0D ? configured.critDamage() : tier * 0.02D,
                configured.lifesteal() > 0.0D ? configured.lifesteal() : tier * 0.25D,
                configured.damage());
    }

    private double sum(Player player, int playerLevel, org.bukkit.NamespacedKey key) {
        double total = 0.0D;
        for (ItemStack item : equippedItems(player)) {
            if (!isUsable(item, playerLevel)) continue;
            total += item.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(key, PersistentDataType.DOUBLE, 0.0D);
        }
        return total;
    }

    private boolean isUsable(ItemStack item, int playerLevel) {
        if (item == null || !item.hasItemMeta()) return false;
        Integer requiredLevel = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        return requiredLevel == null || playerLevel >= requiredLevel;
    }

    private ItemStack[] equippedItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armor = inventory.getArmorContents();
        ItemStack[] equipped = new ItemStack[armor.length + 2];
        System.arraycopy(armor, 0, equipped, 0, armor.length);
        equipped[armor.length] = inventory.getItemInMainHand();
        equipped[armor.length + 1] = inventory.getItemInOffHand();
        return equipped;
    }

    private void applyModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key, double value) {
        applyModifier(player, attribute, key, value, AttributeModifier.Operation.ADD_NUMBER);
    }

    private void applyModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key, double value,
                               AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        removeModifier(player, attribute, key);
        if (value != 0.0D) instance.addModifier(new AttributeModifier(key, value, operation));
    }

    private void removeModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(key);
        if (existing != null) instance.removeModifier(existing);
    }

    private static double clamp(double value, double min, double max) {
        return Math.clamp(value, min, max);
    }
}
