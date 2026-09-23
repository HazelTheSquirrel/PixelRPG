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
}