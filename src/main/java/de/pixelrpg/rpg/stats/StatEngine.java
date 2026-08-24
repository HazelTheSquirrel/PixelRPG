package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.companion.CompanionPassiveStats;
import de.pixelrpg.rpg.companion.CompanionService;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Deterministic character-stat calculator and runtime cache. */
public final class StatEngine {
    private static final double BASE_HEALTH = 20.0D;
    private static final double BASE_CRIT_CHANCE = 0.0D;
    private static final double BASE_CRIT_DAMAGE_MULTIPLIER = 2.0D;
    private static final double MAX_CRIT_CHANCE = 100.0D;
    private static final double MAX_CRIT_DAMAGE_MULTIPLIER = 10.0D;

    public record CachedStats(double maxHealth, double armor, double movementSpeedBonus, double blockReach,
                              double entityReach, double bonusDamage, double critChance,
                              double critDamageMultiplier, double lifestealBonus, double attackPower) {
        public static final CachedStats EMPTY = new CachedStats(
                BASE_HEALTH, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                BASE_CRIT_CHANCE, BASE_CRIT_DAMAGE_MULTIPLIER, 0.0D, 0.0D);

        public double reach() {
            return Math.max(blockReach, entityReach);
        }
    }

    private final PlayerProfileManager profileManager;
    private final Map<UUID, CachedStats> cache = new ConcurrentHashMap<>();
    private volatile CompanionService companionService;

    public StatEngine(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    public void attachCompanionService(CompanionService companionService) {
        this.companionService = companionService;
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
        double itemDamage = sum(player, playerLevel, RPGKeys.Item.bonusDamage());
        double itemLifesteal = sum(player, playerLevel, RPGKeys.Item.lifestealPercent());
        double itemReach = sum(player, playerLevel, RPGKeys.Item.reachBonus());
        double itemMovementSpeed = sum(player, playerLevel, RPGKeys.Item.movementSpeed());

        CompanionPassiveStats companion = companionService == null
                ? CompanionPassiveStats.EMPTY
                : companionService.getActivePassiveStats(player.getUniqueId());

        double maxHealth = Math.max(BASE_HEALTH, BASE_HEALTH + itemHealth + companion.hp());
        double armor = Math.max(0.0D, itemArmor + companion.armor());
        double movementSpeedBonus = itemMovementSpeed + companion.movementSpeed();
        double blockReach = itemReach + companion.reach();
        double entityReach = itemReach + companion.reach();
        double bonusDamage = Math.max(0.0D, itemDamage + companion.damage());
        double critChance = Math.clamp(itemCritChance + companion.crit(), 0.0D, MAX_CRIT_CHANCE);
        double critDamageMultiplier = Math.clamp(
                BASE_CRIT_DAMAGE_MULTIPLIER + itemCritDamage + companion.critDamage(),
                1.0D, MAX_CRIT_DAMAGE_MULTIPLIER);
        double lifestealBonus = Math.max(0.0D, itemLifesteal + companion.lifesteal());
        double attackPower = Math.max(0.0D, itemDamage + companion.attackPower());

        CachedStats stats = new CachedStats(maxHealth, armor, movementSpeedBonus, blockReach, entityReach,
                bonusDamage, critChance, critDamageMultiplier, lifestealBonus, attackPower);
        cache.put(player.getUniqueId(), stats);

        applyModifier(player, Attribute.MAX_HEALTH, RPGKeys.Stats.maxHealth(), maxHealth - BASE_HEALTH);
        applyModifier(player, Attribute.ARMOR, RPGKeys.Stats.armor(), armor);
        applyModifier(player, Attribute.MOVEMENT_SPEED, RPGKeys.Stats.movementSpeed(), movementSpeedBonus);
        applyModifier(player, Attribute.BLOCK_INTERACTION_RANGE, RPGKeys.Stats.blockRange(), blockReach);
        applyModifier(player, Attribute.ENTITY_INTERACTION_RANGE, RPGKeys.Stats.entityRange(), entityReach);

        AttributeInstance healthInstance = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthInstance != null) {
            double max = Math.max(1.0D, healthInstance.getValue());
            player.setHealthScaled(true);
            player.setHealthScale(Math.min(40.0D, max));
            if (player.getHealth() > max) player.setHealth(max);
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
            case DAMAGE -> stats.bonusDamage();
            case CRIT -> stats.critChance();
            case CRIT_DAMAGE -> stats.critDamageMultiplier();
            case LIFESTEAL -> stats.lifestealBonus();
            case ATTACK_POWER -> stats.attackPower();
        };
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
        Integer itemLevel = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
        return itemLevel == null || playerLevel >= itemLevel;
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
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        removeModifier(player, attribute, key);
        if (value != 0.0D) {
            instance.addModifier(new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private void removeModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(key);
        if (existing != null) instance.removeModifier(existing);
    }
}
