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
    public record CachedStats(double maxHealth, double armor, double movementSpeedBonus,
                              double blockReach, double entityReach, double critChance,
                              double critDamageMultiplier, double lifestealBonus, double attackPower) {
        public static final CachedStats EMPTY =
                new CachedStats(100.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 2.0D, 0.0D, 0.0D);
        public double reach() { return Math.max(blockReach, entityReach); }
    }

    private final PlayerProfileManager profiles;
    private final Map<UUID, CachedStats> cache = new ConcurrentHashMap<>();

    public StatEngine(PlayerProfileManager profiles) {
        this.profiles = profiles;
    }

    public CachedStats getCachedStats(UUID uuid) {
        return cache.getOrDefault(uuid, CachedStats.EMPTY);
    }

    public void recalculate(Player player) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || !profile.registered()) {
            clear(player);
            return;
        }

        int level = Math.clamp(profile.level(), 1, 99);
        Totals totals = new Totals();
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getArmorContents()) add(totals, item, level);
        add(totals, inventory.getItemInMainHand(), level);
        add(totals, inventory.getItemInOffHand(), level);

        CachedStats stats = new CachedStats(
                Math.clamp(100.0D + totals.health, 100.0D, 200.0D),
                Math.clamp(totals.armor, 0.0D, 20.0D),
                Math.clamp(totals.movementSpeed * 100.0D, 0.0D, 30.0D),
                Math.clamp(totals.reach, 0.0D, 5.0D),
                Math.clamp(totals.reach, 0.0D, 5.0D),
                Math.clamp(totals.critChance, 0.0D, 100.0D),
                Math.clamp(2.0D + totals.critDamage, 2.0D, 3.0D),
                Math.clamp(totals.lifesteal, 0.0D, 8.0D),
                Math.clamp(totals.attackPower, 0.0D, 15.0D));
        cache.put(player.getUniqueId(), stats);

        apply(player, Attribute.MAX_HEALTH, RPGKeys.Stats.maxHealth(), stats.maxHealth() / 5.0D - 20.0D);
        apply(player, Attribute.ARMOR, RPGKeys.Stats.armor(), stats.armor());
        apply(player, Attribute.MOVEMENT_SPEED, RPGKeys.Stats.movementSpeed(),
                stats.movementSpeedBonus() / 100.0D, AttributeModifier.Operation.ADD_SCALAR);
        apply(player, Attribute.BLOCK_INTERACTION_RANGE, RPGKeys.Stats.blockRange(), stats.blockReach());
        apply(player, Attribute.ENTITY_INTERACTION_RANGE, RPGKeys.Stats.entityRange(), stats.entityReach());

        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            player.setHealthScaled(true);
            player.setHealthScale(Math.min(40.0D, health.getValue()));
            if (player.getHealth() > health.getValue()) player.setHealth(health.getValue());
        }
    }

    public void clear(Player player) {
        cache.remove(player.getUniqueId());
        remove(player, Attribute.MAX_HEALTH, RPGKeys.Stats.maxHealth());
        remove(player, Attribute.ARMOR, RPGKeys.Stats.armor());
        remove(player, Attribute.MOVEMENT_SPEED, RPGKeys.Stats.movementSpeed());
        remove(player, Attribute.BLOCK_INTERACTION_RANGE, RPGKeys.Stats.blockRange());
        remove(player, Attribute.ENTITY_INTERACTION_RANGE, RPGKeys.Stats.entityRange());
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

    private void add(Totals totals, ItemStack item, int level) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer required = pdc.get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (required != null && level < required) return;
        totals.health += value(pdc, RPGKeys.Item.healthBonus());
        totals.armor += value(pdc, RPGKeys.Item.armorValue());
        totals.movementSpeed += value(pdc, RPGKeys.Item.movementSpeed());
        totals.reach += value(pdc, RPGKeys.Item.reachBonus());
        totals.attackPower += value(pdc, RPGKeys.Item.attackPower());
        totals.critChance += value(pdc, RPGKeys.Item.critChance());
        totals.critDamage += value(pdc, RPGKeys.Item.critDamage());
        totals.lifesteal += value(pdc, RPGKeys.Item.lifestealPercent());
    }

    private static double value(org.bukkit.persistence.PersistentDataContainer pdc, org.bukkit.NamespacedKey key) {
        return pdc.getOrDefault(key, PersistentDataType.DOUBLE, 0.0D);
    }

    private static void apply(Player player, Attribute attribute, org.bukkit.NamespacedKey key, double value) {
        apply(player, attribute, key, value, AttributeModifier.Operation.ADD_NUMBER);
    }

    private static void apply(Player player, Attribute attribute, org.bukkit.NamespacedKey key,
                              double value, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        remove(player, attribute, key);
        if (value != 0.0D) instance.addModifier(new AttributeModifier(key, value, operation));
    }

    private static void remove(Player player, Attribute attribute, org.bukkit.NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(key);
        if (existing != null) instance.removeModifier(existing);
    }

    private static final class Totals {
        private double health;
        private double armor;
        private double movementSpeed;
        private double reach;
        private double attackPower;
        private double critChance;
        private double critDamage;
        private double lifesteal;
    }
}
