package de.pixelrpg.rpg.stats;

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

public final class StatEngine {
    private static final double BASE_CRIT_CHANCE = 0.0D;
    private static final double BASE_CRIT_DAMAGE_MULTIPLIER = 2.0D;
    private static final double MAX_CRIT_CHANCE = 100.0D;
    private static final double MAX_CRIT_DAMAGE_MULTIPLIER = 10.0D;

    public record CachedStats(double maxHealth, double armor, double movementSpeedBonus, double blockReach,
                              double entityReach, double bonusDamage, double critChance,
                              double critDamageMultiplier, double lifestealBonus,
                              double strength, double agility, double stamina, double intellect,
                              double attackPower, double spellPower) {
        public static final CachedStats EMPTY = new CachedStats(
                20.0, 0.0, 0.0, 0.0, 0.0, 0.0, BASE_CRIT_CHANCE, BASE_CRIT_DAMAGE_MULTIPLIER,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    private final PlayerProfileManager profileManager;
    private final Map<UUID, CachedStats> cache = new ConcurrentHashMap<>();

    public StatEngine(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
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

        int playerLevel = profile.getLevel();
        double itemArmor = getEquippedItemArmor(player, playerLevel);
        double itemHealth = getEquippedItemHealth(player, playerLevel);
        double itemCritChance = getEquippedItemCritChance(player, playerLevel);
        double itemDamage = getEquippedItemDamage(player, playerLevel);
        double itemLifesteal = getEquippedItemLifesteal(player, playerLevel);

        double maxHealth = 20.0D + itemHealth;
        double armor = itemArmor;
        double movementSpeedBonus = 0.0D;
        double blockReach = 0.0D;
        double entityReach = 0.0D;
        double bonusDamage = itemDamage;
        double critChance = itemCritChance;
        double critDamageMultiplier = BASE_CRIT_DAMAGE_MULTIPLIER;
        double lifestealBonus = itemLifesteal;
        double attackPower = itemDamage;

        CachedStats stats = new CachedStats(maxHealth, armor, movementSpeedBonus, blockReach, entityReach,
                bonusDamage, Math.min(MAX_CRIT_CHANCE, Math.max(0.0D, critChance)),
                Math.min(MAX_CRIT_DAMAGE_MULTIPLIER, Math.max(1.0D, critDamageMultiplier)),
                Math.max(0.0D, lifestealBonus), 0.0D, 0.0D, 0.0D, 0.0D, attackPower, 0.0D);
        cache.put(player.getUniqueId(), stats);

        applyModifier(player, Attribute.MAX_HEALTH, RPGKeys.Stats.maxHealth(), maxHealth - 20.0D);
        applyModifier(player, Attribute.ARMOR, RPGKeys.Stats.armor(), armor);
        applyModifier(player, Attribute.MOVEMENT_SPEED, RPGKeys.Stats.movementSpeed(), movementSpeedBonus);
        applyModifier(player, Attribute.BLOCK_INTERACTION_RANGE, RPGKeys.Stats.blockRange(), blockReach);
        applyModifier(player, Attribute.ENTITY_INTERACTION_RANGE, RPGKeys.Stats.entityRange(), entityReach);

        AttributeInstance healthInstance = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthInstance != null) {
            double max = healthInstance.getValue();
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

    private double getEquippedItemArmor(Player player, int playerLevel) {
        double total = 0.0D;
        for (ItemStack item : equippedItems(player)) {
            if (!isUsable(item, playerLevel)) continue;
            total += item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, 0.0D);
        }
        return total;
    }

    private double getEquippedItemHealth(Player player, int playerLevel) {
        double total = 0.0D;
        for (ItemStack item : equippedItems(player)) {
            if (!isUsable(item, playerLevel)) continue;
            total += item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, 0.0D);
        }
        return total;
    }

    private double getEquippedItemCritChance(Player player, int playerLevel) {
        double total = 0.0D;
        for (ItemStack item : equippedItems(player)) {
            if (!isUsable(item, playerLevel)) continue;
            total += item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, 0.0D);
        }
        return total;
    }

    private double getEquippedItemDamage(Player player, int playerLevel) {
        double total = 0.0D;
        for (ItemStack item : equippedItems(player)) {
            if (!isUsable(item, playerLevel)) continue;
            total += item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Item.bonusDamage(), PersistentDataType.DOUBLE, 0.0D);
        }
        return total;
    }

    private double getEquippedItemLifesteal(Player player, int playerLevel) {
        double total = 0.0D;
        for (ItemStack item : equippedItems(player)) {
            if (!isUsable(item, playerLevel)) continue;
            total += item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, 0.0D);
        }
        return total;
    }

    private boolean isUsable(ItemStack item, int playerLevel) {
        if (item == null || !item.hasItemMeta()) return false;
        Integer itemLevel = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
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
        if (value != 0.0D) instance.addModifier(new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removeModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(key);
        if (existing != null) instance.removeModifier(existing);
    }
}
