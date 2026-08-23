package de.pixelrpg.rpg.core;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class RPGKeys {
    private static Plugin plugin;

    private RPGKeys() { }

    public static void init(Plugin owningPlugin) {
        plugin = owningPlugin;
    }

    private static NamespacedKey of(String value) {
        if (plugin == null) throw new IllegalStateException("RPGKeys.init() was not called before first use.");
        return new NamespacedKey(plugin, value);
    }

    public static final class Item {
        private Item() { }

        public static NamespacedKey identified() { return of("item_identified"); }
        public static NamespacedKey itemId() { return of("item_id"); }
        public static NamespacedKey rarity() { return of("item_rarity"); }
        public static NamespacedKey category() { return of("item_category"); }
        public static NamespacedKey itemLevel() { return of("item_level"); }
        public static NamespacedKey bonusDamage() { return of("item_bonus_damage"); }
        public static NamespacedKey critChance() { return of("item_crit_chance"); }
        public static NamespacedKey armorValue() { return of("item_armor_value"); }
        public static NamespacedKey healthBonus() { return of("item_health_bonus"); }
        public static NamespacedKey toolBonus() { return of("item_tool_bonus"); }
        public static NamespacedKey lifestealPercent() { return of("item_lifesteal_percent"); }
        public static NamespacedKey soulbound() { return of("item_soulbound"); }
        public static NamespacedKey weaponAbility() { return of("item_weapon_ability"); }
        public static NamespacedKey weaponAbilityCooldownMillis() { return of("item_weapon_ability_cooldown"); }
        public static NamespacedKey guildItem() { return of("item_guild_marker"); }
        public static NamespacedKey shopPriceTag() { return of("item_shop_price_tag"); }
        public static NamespacedKey instanceId() { return of("item_instance_id"); }
        public static NamespacedKey attackDamageModifier(String instanceId) { return of("item_mod_attack_damage_" + instanceId); }
        public static NamespacedKey armorModifier(String instanceId) { return of("item_mod_armor_" + instanceId); }
        public static NamespacedKey healthModifier(String instanceId) { return of("item_mod_health_" + instanceId); }
    }

    public static final class Combat {
        private Combat() { }

        public static NamespacedKey mobLevel() { return of("combat_mob_level"); }
        public static NamespacedKey mobBarExpiry() { return of("combat_mob_bar_expiry"); }
        public static NamespacedKey originalMaxHealth() { return of("combat_original_max_health"); }
        public static NamespacedKey originalAttackDamage() { return of("combat_original_attack_damage"); }
    }

    public static final class Stats {
        private Stats() { }

        public static NamespacedKey maxHealth() { return of("stat_max_health"); }
        public static NamespacedKey armor() { return of("stat_armor"); }
        public static NamespacedKey movementSpeed() { return of("stat_movement_speed"); }
        public static NamespacedKey blockRange() { return of("stat_block_range"); }
        public static NamespacedKey entityRange() { return of("stat_entity_range"); }
    }

    public static final class Npc {
        private Npc() { }

        public static NamespacedKey npcType() { return of("npc_type"); }
        public static NamespacedKey npcId() { return of("npc_id"); }
    }

    public static final class Quest {
        private Quest() { }

        public static NamespacedKey navigationCompass() { return of("quest_navigation_compass"); }
    }

    public static final class Companion {
        private Companion() { }

        public static NamespacedKey id() { return of("companion_id"); }
        public static NamespacedKey level() { return of("companion_level"); }
        public static NamespacedKey rarity() { return of("companion_rarity"); }
    }

    public static final class Economy {
        private Economy() { }

        public static NamespacedKey guildGold() { return of("economy_guild_gold"); }
    }

    public static final class Boss {
        private Boss() { }

        public static NamespacedKey bossId() { return of("boss_id"); }
        public static NamespacedKey worldBossMarker() { return of("boss_world_marker"); }
    }

    public static final class Special {
        private Special() { }

        public static NamespacedKey guildCompassMarker() { return of("special_guild_compass"); }
    }
}
