package de.pixelrpg.rpg.core;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class RPGKeys {
    private final Plugin plugin;

    public RPGKeys(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(plugin, value);
    }

    public NamespacedKey itemId() { return key("item_id"); }
    public NamespacedKey identified() { return key("item_identified"); }
    public NamespacedKey rarity() { return key("item_rarity"); }
    public NamespacedKey category() { return key("item_category"); }
    public NamespacedKey itemLevel() { return key("item_level"); }
    public NamespacedKey requiredLevel() { return key("item_required_level"); }
    public NamespacedKey gearscore() { return key("item_gearscore"); }
    public NamespacedKey resourcepackId() { return key("item_resourcepack_id"); }
    public NamespacedKey unique() { return key("item_unique"); }
    public NamespacedKey equipmentSlot() { return key("item_equipment_slot"); }
    public NamespacedKey setId() { return key("item_set_id"); }
    public NamespacedKey attackPower() { return key("item_attack_power"); }
    public NamespacedKey critChance() { return key("item_crit_chance"); }
    public NamespacedKey critDamage() { return key("item_crit_damage"); }
    public NamespacedKey armorValue() { return key("item_armor_value"); }
    public NamespacedKey healthBonus() { return key("item_health_bonus"); }
    public NamespacedKey movementSpeed() { return key("item_movement_speed"); }
    public NamespacedKey reachBonus() { return key("item_reach_bonus"); }
    public NamespacedKey toolBonus() { return key("item_tool_bonus"); }
    public NamespacedKey lifestealPercent() { return key("item_lifesteal_percent"); }
    public NamespacedKey soulbound() { return key("item_soulbound"); }
    public NamespacedKey weaponAbility() { return key("item_weapon_ability"); }
    public NamespacedKey weaponAbilityCooldownMillis() { return key("item_weapon_ability_cooldown"); }
    public NamespacedKey guildItem() { return key("item_guild_marker"); }
    public NamespacedKey shopBuyPriceTag() { return key("item_shop_buy_price_tag"); }
    public NamespacedKey shopSellPriceTag() { return key("item_shop_sell_price_tag"); }
    public NamespacedKey instanceId() { return key("item_instance_id"); }
    public NamespacedKey guildGold() { return key("economy_guild_gold"); }

    public NamespacedKey companionId() { return key("companion_id"); }
    public NamespacedKey companionLevel() { return key("companion_level"); }
    public NamespacedKey companionRarity() { return key("companion_rarity"); }
    public NamespacedKey npcId() { return key("npc_id"); }
    public NamespacedKey npcType() { return key("npc_type"); }
    public static final class Item { private Item() {} public static NamespacedKey identified(){return new NamespacedKey("pixelrpg","item_identified");} public static NamespacedKey itemId(){return new NamespacedKey("pixelrpg","item_id");} public static NamespacedKey rarity(){return new NamespacedKey("pixelrpg","item_rarity");} public static NamespacedKey category(){return new NamespacedKey("pixelrpg","item_category");} public static NamespacedKey itemLevel(){return new NamespacedKey("pixelrpg","item_level");} public static NamespacedKey requiredLevel(){return new NamespacedKey("pixelrpg","item_required_level");} public static NamespacedKey gearscore(){return new NamespacedKey("pixelrpg","item_gearscore");} public static NamespacedKey instanceId(){return new NamespacedKey("pixelrpg","item_instance_id");} public static NamespacedKey attackPower(){return new NamespacedKey("pixelrpg","item_attack_power");} public static NamespacedKey critChance(){return new NamespacedKey("pixelrpg","item_crit_chance");} public static NamespacedKey critDamage(){return new NamespacedKey("pixelrpg","item_crit_damage");} public static NamespacedKey armorValue(){return new NamespacedKey("pixelrpg","item_armor_value");} public static NamespacedKey healthBonus(){return new NamespacedKey("pixelrpg","item_health_bonus");} public static NamespacedKey movementSpeed(){return new NamespacedKey("pixelrpg","item_movement_speed");} public static NamespacedKey reachBonus(){return new NamespacedKey("pixelrpg","item_reach_bonus");} public static NamespacedKey lifestealPercent(){return new NamespacedKey("pixelrpg","item_lifesteal_percent");} public static NamespacedKey setId(){return new NamespacedKey("pixelrpg","item_set_id");} public static NamespacedKey weaponAbility(){return new NamespacedKey("pixelrpg","item_weapon_ability");} public static NamespacedKey weaponAbilityCooldownMillis(){return new NamespacedKey("pixelrpg","item_weapon_ability_cooldown");} }
    public static final class Companion { private Companion() {} public static NamespacedKey id(){return new NamespacedKey("pixelrpg","companion_id");} public static NamespacedKey level(){return new NamespacedKey("pixelrpg","companion_level");} public static NamespacedKey rarity(){return new NamespacedKey("pixelrpg","companion_rarity");} }
    public static final class Combat { private Combat() {} public static NamespacedKey mobLevel(){return new NamespacedKey("pixelrpg","combat_mob_level");} public static NamespacedKey originalMaxHealth(){return new NamespacedKey("pixelrpg","combat_original_max_health");} public static NamespacedKey originalAttackDamage(){return new NamespacedKey("pixelrpg","combat_original_attack_damage");} }
    public static final class Stats { private Stats() {} public static NamespacedKey maxHealth(){return new NamespacedKey("pixelrpg","stat_max_health");} public static NamespacedKey armor(){return new NamespacedKey("pixelrpg","stat_armor");} public static NamespacedKey movementSpeed(){return new NamespacedKey("pixelrpg","stat_movement_speed");} public static NamespacedKey blockRange(){return new NamespacedKey("pixelrpg","stat_block_range");} public static NamespacedKey entityRange(){return new NamespacedKey("pixelrpg","stat_entity_range");} }
    public static final class Boss { private Boss() {} public static NamespacedKey bossId(){return new NamespacedKey("pixelrpg","boss_id");} public static NamespacedKey worldBossMarker(){return new NamespacedKey("pixelrpg","boss_world_marker");} }
}
