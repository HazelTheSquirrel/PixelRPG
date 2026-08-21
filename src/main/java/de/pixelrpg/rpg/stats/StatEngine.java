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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatEngine {
    public record CachedStats(double maxHealth, double armor, double movementSpeedBonus, double blockReach,
                              double entityReach, double bonusDamage, double critChance,
                              double critDamageMultiplier, double lifestealBonus) {
        public static final CachedStats EMPTY = new CachedStats(20.0, 0.0, 0.0, 0.0, 0.0, 5.0, 2.0, 0.0, 0.0);
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
        double maxHealth = 20.0 + classBalance.healthBonus();
        double armor = classBalance.armorBonus();
        double movementSpeedBonus = classBalance.speedBonus();
        double blockReach = 0.0;
        double entityReach = 0.0;
        double bonusDamage = 0.0;
        double critChance = 5.0 + classBalance.critChanceBonus();
        double critDamageMultiplier = 2.0 * classBalance.critDamageMultiplier();
        double lifestealBonus = 0.0;

        maxHealth += profile.getAttributePoints(PlayerAttribute.VITALITY) * AttributeConfig.VITALITY_HP_PER_POINT;
        int agility = profile.getAttributePoints(PlayerAttribute.AGILITY);
        movementSpeedBonus += agility * AttributeConfig.AGILITY_SPEED_PER_POINT;
        critChance += agility * AttributeConfig.AGILITY_CRIT_PER_POINT;
        bonusDamage += profile.getAttributePoints(PlayerAttribute.PRECISION) * AttributeConfig.PRECISION_DAMAGE_PER_POINT;
        int range = profile.getAttributePoints(PlayerAttribute.RANGE);
        blockReach += range * AttributeConfig.RANGE_BLOCK_PER_POINT;
        entityReach += range * AttributeConfig.RANGE_ENTITY_PER_POINT;
        armor += profile.getAttributePoints(PlayerAttribute.TOUGHNESS) * AttributeConfig.TOUGHNESS_ARMOR_PER_POINT;

        CachedStats stats = new CachedStats(maxHealth, armor, movementSpeedBonus, blockReach, entityReach,
                bonusDamage, critChance, critDamageMultiplier, lifestealBonus);
        cache.put(player.getUniqueId(), stats);
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
