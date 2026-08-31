package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.CharacterStatType;
import de.pixelrpg.rpg.balance.BalanceModel;
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

/** Central character aggregation and final PixelRPG -> Minecraft attribute bridge. */
public final class StatEngine {
    private static final double BASE_MINECRAFT_HEALTH = 20.0D;
    private static final double BASE_RPG_HEALTH = 100.0D;
    private static final double BASE_CRIT_DAMAGE_MULTIPLIER = 2.0D;

    public record CachedStats(double maxHealth, double armor, double movementSpeedBonus, double blockReach,
                              double entityReach, double critChance, double critDamageMultiplier,
                              double lifestealBonus, double attackPower) {
        public static final CachedStats EMPTY = new CachedStats(BASE_MINECRAFT_HEALTH, 0.0D, 0.0D,
                BalanceModel.BASE_BLOCK_REACH, BalanceModel.BASE_ENTITY_REACH,
                0.0D, BASE_CRIT_DAMAGE_MULTIPLIER, 0.0D, 0.0D);
        public double rpgHealth() { return maxHealth * 5.0D; }
        public double rpgHealthBonus() { return Math.max(0.0D, rpgHealth() - BASE_RPG_HEALTH); }
        public double reach() { return Math.max(blockReach, entityReach); }
        public double critDamageBonusPercent() { return (critDamageMultiplier - BASE_CRIT_DAMAGE_MULTIPLIER) * 100.0D; }
    }

    private final PlayerProfileManager profileManager;
    private final EquipmentSetService equipmentSets;
    private final Map<UUID, CachedStats> cache = new ConcurrentHashMap<>();

    public StatEngine(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
        PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
        this.equipmentSets = plugin == null ? null : new EquipmentSetService(plugin);
    }

    public CachedStats getCachedStats(UUID uuid) { return cache.getOrDefault(uuid, CachedStats.EMPTY); }

    public void recalculate(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) { clear(player); return; }
        int level = BalanceModel.clampLevel(profile.getLevel());

        double hpBonus = sum(player, level, RPGKeys.Item.healthBonus());
        double armor = sum(player, level, RPGKeys.Item.armorValue());
        double crit = sum(player, level, RPGKeys.Item.critChance());
        double critDamage = sum(player, level, RPGKeys.Item.critDamage());
        double lifesteal = sum(player, level, RPGKeys.Item.lifestealPercent());
        double reachBonus = sum(player, level, RPGKeys.Item.reachBonus());
        double movement = sum(player, level, RPGKeys.Item.movementSpeed());
        double attack = sum(player, level, RPGKeys.Item.attackPower());

        Map<String, Double> set = equipmentSets == null ? Map.of() : equipmentSets.bonuses(player, level);
        hpBonus += set.getOrDefault("HP", 0.0D);
        armor += set.getOrDefault("ARMOR", 0.0D);
        movement += set.getOrDefault("MOVEMENT_SPEED", 0.0D);
        reachBonus += set.getOrDefault("REACH", 0.0D);
        crit += set.getOrDefault("CRIT", 0.0D) + set.getOrDefault("CRIT_CHANCE", 0.0D);
        critDamage += set.getOrDefault("CRIT_DAMAGE", 0.0D);
        lifesteal += set.getOrDefault("LIFESTEAL", 0.0D);
        attack += set.getOrDefault("ATTACK_POWER", 0.0D);

        CompanionPassiveStats companion = activeCompanionPassiveStats(player.getUniqueId());
        double rpgHp = Math.clamp(100.0D + hpBonus + companion.hp(), 100.0D, 200.0D);
        double finalArmor = Math.clamp(armor + companion.armor(), 0.0D, 20.0D);
        double movePercent = Math.clamp((movement + companion.movementSpeed()) * 100.0D, 0.0D, 30.0D);
        double entityReach = Math.clamp(3.0D + reachBonus + companion.reach(), 3.0D, 5.0D);
        double blockReach = Math.clamp(4.5D + reachBonus + companion.reach(), 4.5D, 5.0D);
        double finalCrit = Math.clamp(crit + companion.crit(), 0.0D, 100.0D);
        double finalCritDamage = Math.clamp(critDamage + companion.critDamage(), 0.0D, 1.0D);
        double finalLifesteal = Math.clamp(lifesteal + companion.lifesteal(), 0.0D, 8.0D);
        double finalAttack = Math.clamp(attack + companion.damage() + companion.attackPower(), 0.0D, 15.0D);
        double mcHealth = Math.clamp(rpgHp / 5.0D, 20.0D, 40.0D);

        CachedStats stats = new CachedStats(mcHealth, finalArmor, movePercent, blockReach, entityReach,
                finalCrit, BASE_CRIT_DAMAGE_MULTIPLIER + finalCritDamage, finalLifesteal, finalAttack);
        cache.put(player.getUniqueId(), stats);

        applyTotalModifier(player, Attribute.MAX_HEALTH, RPGKeys.Stats.maxHealth(), mcHealth);
        applyTotalModifier(player, Attribute.ARMOR, RPGKeys.Stats.armor(), finalArmor);
        applyModifier(player, Attribute.MOVEMENT_SPEED, RPGKeys.Stats.movementSpeed(), movePercent / 100.0D,
                AttributeModifier.Operation.ADD_SCALAR);
        applyTotalModifier(player, Attribute.BLOCK_INTERACTION_RANGE, RPGKeys.Stats.blockRange(), blockReach);
        applyTotalModifier(player, Attribute.ENTITY_INTERACTION_RANGE, RPGKeys.Stats.entityRange(), entityReach);

        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            double max = Math.clamp(health.getValue(), 20.0D, 40.0D);
            player.setHealthScaled(true);
            player.setHealthScale(max);
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
            case HP -> stats.rpgHealth();
            case ARMOR -> stats.armor();
            case MOVEMENT_SPEED -> stats.movementSpeedBonus();
            case REACH -> stats.reach();
            case CRIT -> stats.critChance();
            case CRIT_DAMAGE -> stats.critDamageBonusPercent();
            case LIFESTEAL -> stats.lifestealBonus();
            case ATTACK_POWER -> stats.attackPower();
        };
    }

    private CompanionPassiveStats activeCompanionPassiveStats(UUID playerId) {
        PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
        if (plugin == null) return CompanionPassiveStats.EMPTY;
        CompanionService service = plugin.getCompanionService();
        if (service == null) return CompanionPassiveStats.EMPTY;
        UUID entityId = service.getActiveEntity(playerId);
        if (entityId == null) return CompanionPassiveStats.EMPTY;
        Entity entity = Bukkit.getEntity(entityId);
        if (entity == null || entity.isDead()) return CompanionPassiveStats.EMPTY;
        Companion active = service.getActive(playerId);
        if (active == null || active.rarity().isUnique()) return CompanionPassiveStats.EMPTY;
        CompanionDefinition definition = service.definition(active.id());
        if (!definition.passive()) return CompanionPassiveStats.EMPTY;
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
                0.0D, 0.0D, 0.0D,
                configured.critChance() > 0.0D ? configured.critChance() : tier * 0.5D,
                configured.critDamage() > 0.0D ? configured.critDamage() : tier * 0.02D,
                configured.lifesteal() > 0.0D ? configured.lifesteal() : tier * 0.25D,
                0.0D);
    }

    private double sum(Player player, int level, org.bukkit.NamespacedKey key) {
        double total = 0.0D;
        for (ItemStack item : equippedItems(player)) {
            if (!isUsable(item, level)) continue;
            total += item.getItemMeta().getPersistentDataContainer().getOrDefault(key, PersistentDataType.DOUBLE, 0.0D);
        }
        return total;
    }

    private boolean isUsable(ItemStack item, int playerLevel) {
        if (item == null || !item.hasItemMeta()) return false;
        Integer required = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        return required == null || playerLevel >= required;
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

    private void applyTotalModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key, double desired) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        removeModifier(player, attribute, key);
        double delta = desired - instance.getValue();
        if (Math.abs(delta) > 1.0E-9D) {
            instance.addModifier(new AttributeModifier(key, delta, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private void applyModifier(Player player, Attribute attribute, org.bukkit.NamespacedKey key,
                               double value, AttributeModifier.Operation operation) {
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
}
