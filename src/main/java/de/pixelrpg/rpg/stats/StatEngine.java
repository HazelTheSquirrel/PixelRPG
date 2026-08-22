package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.AttributeConfig;
import de.pixelrpg.rpg.player.ClassBalance;
import de.pixelrpg.rpg.player.PlayerAttribute;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatEngine {
    private static final double BASE_CRIT_CHANCE = 5.0D;
    private static final double MAX_CRIT_CHANCE = 50.0D;
    private static final double MAX_CRIT_DAMAGE_MULTIPLIER = 2.5D;

    public record CachedStats(double maxHealth, double armor, double movementSpeedBonus, double blockReach,
                              double entityReach, double bonusDamage, double critChance,
                              double critDamageMultiplier, double lifestealBonus,
                              double strength, double agility, double stamina, double intellect,
                              double attackPower, double spellPower, double maxMana) {
        public static final CachedStats EMPTY = new CachedStats(
                20.0, 0.0, 0.0, 0.0, 0.0, 0.0, BASE_CRIT_CHANCE, 2.0, 0.0,
                10.0, 10.0, 10.0, 10.0, 0.0, 0.0, 100.0);
    }

    private final PlayerProfileManager profileManager;
    private final Map<UUID, CachedStats> cache = new ConcurrentHashMap<>();
    private final Set<UUID> managedSoulview = ConcurrentHashMap.newKeySet();

    public StatEngine(PlayerProfileManager profileManager) { this.profileManager = profileManager; }
    public CachedStats getCachedStats(UUID uuid) { return cache.getOrDefault(uuid, CachedStats.EMPTY); }

    public void recalculate(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            clear(player);
            return;
        }

        ClassBalance classBalance = ClassBalance.of(profile);
        double itemArmor = getEquippedItemArmor(player, profile.getLevel());
        double itemHealth = getEquippedItemHealth(player, profile.getLevel());
        double itemCritChance = getEquippedItemCritChance(player, profile.getLevel());
        double maxHealth = 20.0 + classBalance.healthBonus() + itemHealth;
        double armor = classBalance.armorBonus() + itemArmor;
        double movementSpeedBonus = classBalance.speedBonus();
        double blockReach = 0.0;
        double entityReach = 0.0;
        double bonusDamage = 0.0;
        double critChance = BASE_CRIT_CHANCE + classBalance.critChanceBonus();
        double critDamageMultiplier = Math.min(MAX_CRIT_DAMAGE_MULTIPLIER, 2.0 * classBalance.critDamageMultiplier());
        double lifestealBonus = 0.0;

        int vitality = profile.getAttributePoints(PlayerAttribute.VITALITY);
        int agility = profile.getAttributePoints(PlayerAttribute.AGILITY);
        int precision = profile.getAttributePoints(PlayerAttribute.PRECISION);
        int range = Math.max(0, profile.getAttributePoints(PlayerAttribute.RANGE));
        int toughness = profile.getAttributePoints(PlayerAttribute.TOUGHNESS);

        maxHealth += vitality * AttributeConfig.VITALITY_HP_PER_POINT;
        movementSpeedBonus += agility * AttributeConfig.AGILITY_SPEED_PER_POINT;
        critChance += agility * AttributeConfig.AGILITY_CRIT_PER_POINT;
        critChance += itemCritChance;
        bonusDamage += precision * AttributeConfig.PRECISION_DAMAGE_PER_POINT;
        blockReach += range * AttributeConfig.RANGE_BLOCK_PER_POINT;
        entityReach += range * AttributeConfig.RANGE_ENTITY_PER_POINT;
        armor += toughness * AttributeConfig.TOUGHNESS_ARMOR_PER_POINT;

        double strength = 10.0 + toughness;
        double agilityStat = 10.0 + agility;
        double stamina = 10.0 + vitality;
        double intellect = 10.0 + precision;
        double attackPower = strength + bonusDamage;
        double spellPower = intellect * classBalance.spellDamageMultiplier();
        double classManaBase = switch (profile.getPlayerClass()) {
            case MAGE -> 150.0;
            case HEALER -> 130.0;
            case RANGER -> 110.0;
            case ROGUE -> 100.0;
            case WARRIOR -> 80.0;
            case NONE -> 100.0;
        };
        double maxMana = classManaBase + intellect * 5.0;

        CachedStats stats = new CachedStats(
                maxHealth,
                armor,
                movementSpeedBonus,
                blockReach,
                entityReach,
                bonusDamage,
                Math.min(MAX_CRIT_CHANCE, Math.max(0.0D, critChance)),
                Math.min(MAX_CRIT_DAMAGE_MULTIPLIER, Math.max(1.0D, critDamageMultiplier)),
                lifestealBonus,
                strength,
                agilityStat,
                stamina,
                intellect,
                attackPower,
                spellPower,
                maxMana
        );
        cache.put(player.getUniqueId(), stats);

        boolean manaChanged = false;
        if (!profile.isManaInitialized()) {
            profile.initializeMana(maxMana);
            manaChanged = true;
        } else if (profile.getCurrentMana() > maxMana) {
            profile.setCurrentMana(profile.getCurrentMana(), maxMana);
            manaChanged = true;
        }
        if (manaChanged) profileManager.saveProfileAsync(player.getUniqueId());

        applyModifier(player, Attribute.MAX_HEALTH, RPGKeys.Stats.maxHealth(), maxHealth - 20.0);
        applyModifier(player, Attribute.ARMOR, RPGKeys.Stats.armor(), armor);
        applyModifier(player, Attribute.MOVEMENT_SPEED, RPGKeys.Stats.movementSpeed(), movementSpeedBonus);
        applyModifier(player, Attribute.BLOCK_INTERACTION_RANGE, RPGKeys.Stats.blockRange(), blockReach);
        applyModifier(player, Attribute.ENTITY_INTERACTION_RANGE, RPGKeys.Stats.entityRange(), entityReach);

        AttributeInstance healthInstance = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthInstance != null) {
            double max = healthInstance.getValue();
            player.setHealthScaled(true);
            player.setHealthScale(Math.min(40.0, max));
            if (player.getHealth() > max) player.setHealth(max);
        }

        boolean hasSoulview = profile.getAttributePoints(PlayerAttribute.SOULVIEW) > 0;
        if (hasSoulview) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, false, false));
            managedSoulview.add(player.getUniqueId());
        } else if (managedSoulview.remove(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
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
        if (managedSoulview.remove(player.getUniqueId())) player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    public void restoreMana(Player player, double amount) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) return;
        profile.restoreMana(amount, getCachedStats(player.getUniqueId()).maxMana());
    }

    public boolean consumeMana(Player player, double amount) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) return false;
        return profile.consumeMana(amount, getCachedStats(player.getUniqueId()).maxMana());
    }

    public double getMana(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        return profile != null && profile.isRegisteredInGuild() ? profile.getCurrentMana() : 0.0;
    }

    public double getMaxMana(Player player) { return getCachedStats(player.getUniqueId()).maxMana(); }

    private double getEquippedItemArmor(Player player, int playerLevel) {
        double total = 0.0;
        for (ItemStack item : equippedItems(player)) {
            if (item == null || !item.hasItemMeta() || !meetsLevelRequirement(item, playerLevel)) continue;
            total += item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, 0.0);
        }
        return total;
    }

    private double getEquippedItemHealth(Player player, int playerLevel) {
        double total = 0.0;
        for (ItemStack item : equippedItems(player)) {
            if (item == null || !item.hasItemMeta() || !meetsLevelRequirement(item, playerLevel)) continue;
            total += item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, 0.0);
        }
        return total;
    }

    private double getEquippedItemCritChance(Player player, int playerLevel) {
        double total = 0.0;
        for (ItemStack item : equippedItems(player)) {
            if (item == null || !item.hasItemMeta() || !meetsLevelRequirement(item, playerLevel)) continue;
            total += item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, 0.0);
        }
        return total;
    }

    private boolean meetsLevelRequirement(ItemStack item, int playerLevel) {
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
        if (value != 0.0) instance.addModifier(new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removeModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(key);
        if (existing != null) instance.removeModifier(existing);
    }
}
