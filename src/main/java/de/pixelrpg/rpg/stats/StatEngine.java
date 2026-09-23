package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.CharacterStatType;
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
        public static final CachedStats EMPTY = new CachedStats(BASE_HP, 0.0D, 0.0D, 0.0D,
                0.0D, BASE_CRIT_CHANCE, BASE_CRIT_DAMAGE_MULTIPLIER, 0.0D, 0.0D);
        public double reach() { return Math.max(blockReach, entityReach); }
    }

    private final PlayerProfileManager profiles;
    private final Map<UUID, CachedStats> cache = new ConcurrentHashMap<>();

    public StatEngine(PlayerProfileManager profiles) {
        this.profiles = profiles;
    }

    public CachedStats getCachedStats(UUID uuid) { return cache.getOrDefault(uuid, CachedStats.EMPTY); }

    public void recalculate(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) { clear(player); return; }

        ItemTotals items = sumEquippedItemStats(player, Math.clamp(profile.getLevel(), 1, 99));
        double maxHealth = clamp(BASE_HP + items.health, BASE_HP, MAX_HP);
        double armor = clamp(items.armor, 0.0D, MAX_ARMOR);
        double movement = clamp(items.movement * 100.0D, 0.0D, MAX_MOVEMENT_SPEED_PERCENT);
        double reach = clamp(items.reach, 0.0D, MAX_REACH);
        double crit = clamp(items.crit, 0.0D, MAX_CRIT_CHANCE);
        double critDamage = clamp(BASE_CRIT_DAMAGE_MULTIPLIER + items.critDamage,
                BASE_CRIT_DAMAGE_MULTIPLIER, MAX_CRIT_DAMAGE_MULTIPLIER);
        double lifesteal = clamp(items.lifesteal, 0.0D, MAX_LIFESTEAL);
        double attack = clamp(items.attack, 0.0D, MAX_ATTACK_POWER);

        CachedStats stats = new CachedStats(maxHealth, armor, movement, reach, reach, crit, critDamage, lifesteal, attack);
        cache.put(player.getUniqueId(), stats);

        applyModifier(player, Attribute.MAX_HEALTH, RPGKeys.Stats.maxHealth(), maxHealth / PIXELRPG_HP_PER_MINECRAFT_HEALTH - 20.0D);
        applyModifier(player, Attribute.ARMOR, RPGKeys.Stats.armor(), armor);
        applyModifier(player, Attribute.MOVEMENT_SPEED, RPGKeys.Stats.movementSpeed(), movement / 100.0D, AttributeModifier.Operation.ADD_SCALAR);
        applyModifier(player, Attribute.BLOCK_INTERACTION_RANGE, RPGKeys.Stats.blockRange(), reach);
        applyModifier(player, Attribute.ENTITY_INTERACTION_RANGE, RPGKeys.Stats.entityRange(), reach);

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double max = Math.max(1.0D, maxHealthAttribute.getValue());
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
            case CRIT -> stats.critChance();
            case CRIT_DAMAGE -> stats.critDamageMultiplier();
            case LIFESTEAL -> stats.lifestealBonus();
            case ATTACK_POWER -> stats.attackPower();
        };
    }

    private ItemTotals sumEquippedItemStats(Player player, int level) {
        ItemTotals totals = new ItemTotals();
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getArmorContents()) addItem(totals, item, level);
        addItem(totals, inventory.getItemInMainHand(), level);
        addItem(totals, inventory.getItemInOffHand(), level);
        return totals;
    }

    private void addItem(ItemTotals totals, ItemStack item, int level) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer required = pdc.get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (required != null && level < required) return;
        totals.health += pdc.getOrDefault(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, 0.0D);
        totals.armor += pdc.getOrDefault(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, 0.0D);
        totals.crit += pdc.getOrDefault(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, 0.0D);
        totals.critDamage += pdc.getOrDefault(RPGKeys.Item.critDamage(), PersistentDataType.DOUBLE, 0.0D);
        totals.lifesteal += pdc.getOrDefault(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, 0.0D);
        totals.reach += pdc.getOrDefault(RPGKeys.Item.reachBonus(), PersistentDataType.DOUBLE, 0.0D);
        totals.movement += pdc.getOrDefault(RPGKeys.Item.movementSpeed(), PersistentDataType.DOUBLE, 0.0D);
        totals.attack += pdc.getOrDefault(RPGKeys.Item.attackPower(), PersistentDataType.DOUBLE, 0.0D);
    }

    private void applyModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key, double value) {
        applyModifier(player, attribute, key, value, AttributeModifier.Operation.ADD_NUMBER);
    }

    private void applyModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key, double value, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        removeModifier(player, attribute, key);
        if (value != 0.0D) instance.addModifier(new AttributeModifier(key, value, operation));
    }

    private void removeModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier modifier = instance.getModifier(key);
        if (modifier != null) instance.removeModifier(modifier);
    }

    private static double clamp(double value, double min, double max) { return Math.clamp(value, min, max); }

    private record CompanionTotals(double health, double armor, double movement, double crit, double critDamage, double lifesteal, double attack) {
        private static final CompanionTotals EMPTY = new CompanionTotals(0,0,0,0,0,0,0);
    }

    private static final class ItemTotals {
        private double health, armor, crit, critDamage, lifesteal, reach, movement, attack;
    }
}
