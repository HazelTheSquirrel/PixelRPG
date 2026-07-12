// src/main/java/de/pixelrpg/rpg/combat/gem/DefaultGemData.java
// Erzeugt beim ersten Start die vollständigen Default-Dateien: 10 Active-Gems je Klasse (50 gesamt)
// und 60 Passive-Gems (universell + klassengebunden). Alles editierbar nach Erstgenerierung.
package de.pixelrpg.rpg.combat.gem;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DefaultGemData {

    private DefaultGemData() {
    }

    public static void writeActiveDefaults(File file) {
        YamlConfiguration yaml = new YamlConfiguration();

        // WARRIOR (Tank/Melee)
        active(yaml, "shield_bash", "Shield Bash", "WARRIOR", 7000, "SHIELD", "SINGLE_TARGET_STRIKE", 0.5, 6.0, 0, 60, false, true, false, false, false, "ITEM_SHIELD_BLOCK", "CRIT");
        active(yaml, "battle_cry", "Battle Cry", "WARRIOR", 20000, "RED_BANNER", "BUFF_GROUP_STRENGTH", 0, 0, 10, 200, false, false, false, false, false, "ENTITY_ENDER_DRAGON_GROWL", "ANGRY_VILLAGER");
        active(yaml, "ground_slam", "Ground Slam", "WARRIOR", 18000, "NETHERITE_INGOT", "GROUND_SLAM", 0.5, 6.0, 4, 0, false, false, false, false, false, "ITEM_MACE_SMASH_AIR", "EXPLOSION");
        active(yaml, "cleave", "Cleave", "WARRIOR", 9000, "IRON_AXE", "CONE_SWEEP", 0.6, 5.0, 3.5, 0, false, false, false, false, false, "ENTITY_PLAYER_ATTACK_SWEEP", "SWEEP_ATTACK");
        active(yaml, "crushing_blow", "Crushing Blow", "WARRIOR", 10000, "GOLDEN_SWORD", "SINGLE_TARGET_STRIKE", 0.8, 7.0, 0, 80, false, false, false, true, false, "ITEM_MACE_SMASH_GROUND", "DAMAGE_INDICATOR");
        active(yaml, "unbreakable_wall", "Unbreakable Wall", "WARRIOR", 22000, "NETHERITE_CHESTPLATE", "SHIELD_SELF", 0, 0, 0, 200, false, false, false, false, false, "ITEM_ARMOR_EQUIP_NETHERITE", "TOTEM_OF_UNDYING");
        active(yaml, "reckless_charge", "Reckless Charge", "WARRIOR", 12000, "IRON_BOOTS", "DASH_STRIKE", 0.7, 6.0, 0, 60, false, true, false, false, false, "ENTITY_RAVAGER_ATTACK", "CLOUD");
        active(yaml, "warlords_fury", "Warlord's Fury", "WARRIOR", 15000, "NETHERITE_AXE", "AOE_NOVA", 0.6, 6.0, 4.5, 0, false, false, false, true, false, "ENTITY_RAVAGER_ROAR", "SWEEP_ATTACK");
        active(yaml, "vampiric_edge", "Vampiric Edge", "WARRIOR", 13000, "IRON_SWORD", "SINGLE_TARGET_STRIKE", 0.7, 6.0, 0, 0, false, false, false, false, true, "ENTITY_PLAYER_ATTACK_CRIT", "CRIMSON_SPORE");
        active(yaml, "earthbreaker", "Earthbreaker", "WARRIOR", 25000, "ANVIL", "GROUND_SLAM", 0.9, 9.0, 5.0, 0, false, true, false, false, false, "BLOCK_ANVIL_LAND", "BLOCK_CRACK");

        // RANGER (Ranged DPS)
        active(yaml, "multi_shot", "Multi Shot", "RANGER", 6000, "ARROW", "PROJECTILE_VOLLEY", 0.6, 2.0, 0, 0, false, false, false, false, false, "ENTITY_ARROW_SHOOT", "CRIT");
        active(yaml, "piercing_shot", "Piercing Shot", "RANGER", 12000, "SPECTRAL_ARROW", "PIERCING_BEAM", 1.0, 8.0, 15.0, 0, false, false, false, false, false, "ENTITY_ARROW_HIT_PLAYER", "CRIT");
        active(yaml, "quick_volley", "Quick Volley", "RANGER", 9000, "TIPPED_ARROW", "PROJECTILE_VOLLEY", 0.5, 4.0, 0, 60, false, false, false, false, false, "ITEM_CROSSBOW_QUICK_CHARGE_3", "CLOUD");
        active(yaml, "hunters_mark", "Hunter's Mark", "RANGER", 10000, "COMPASS", "SINGLE_TARGET_STRIKE", 0.9, 6.0, 0, 100, false, false, false, true, false, "ENTITY_ARROW_HIT", "SMOKE");
        active(yaml, "explosive_arrow", "Explosive Arrow", "RANGER", 14000, "TNT", "AOE_NOVA", 0.8, 7.0, 3.5, 0, true, false, false, false, false, "ENTITY_GENERIC_EXPLODE", "EXPLOSION");
        active(yaml, "windrunner_dash", "Windrunner Dash", "RANGER", 11000, "FEATHER", "DASH_STRIKE", 0.5, 4.0, 0, 0, false, false, false, false, false, "ENTITY_PHANTOM_FLAP", "CLOUD");
        active(yaml, "snare_shot", "Snare Shot", "RANGER", 13000, "COBWEB", "SINGLE_TARGET_STRIKE", 0.6, 5.0, 0, 100, false, false, true, false, false, "BLOCK_COBWEB_BREAK", "SMOKE");
        active(yaml, "rain_of_arrows", "Rain of Arrows", "RANGER", 20000, "BOW", "AOE_NOVA", 0.7, 6.0, 5.0, 0, false, true, false, false, false, "ENTITY_ARROW_SHOOT", "CLOUD");
        active(yaml, "focused_shot", "Focused Shot", "RANGER", 8000, "CROSSBOW", "SINGLE_TARGET_STRIKE", 1.2, 10.0, 0, 0, false, false, false, false, false, "ITEM_CROSSBOW_SHOOT", "CRIT");
        active(yaml, "eagle_eye", "Eagle Eye", "RANGER", 16000, "SPYGLASS", "SHIELD_SELF", 0, 0, 0, 160, false, false, false, false, false, "ENTITY_PLAYER_LEVELUP", "END_ROD");

        // ROGUE (Melee Burst/Crit)
        active(yaml, "rending_strike", "Rending Strike", "ROGUE", 16000, "IRON_SWORD", "SINGLE_TARGET_STRIKE", 0.8, 8.0, 0, 100, false, true, false, true, false, "ENTITY_PLAYER_ATTACK_STRONG", "DAMAGE_INDICATOR");
        active(yaml, "backstab", "Backstab", "ROGUE", 8000, "IRON_SWORD", "SINGLE_TARGET_STRIKE", 0.9, 8.0, 0, 60, false, false, false, false, false, "ENTITY_PLAYER_ATTACK_CRIT", "SMOKE");
        active(yaml, "shadow_step", "Shadow Step", "ROGUE", 12000, "ENDER_PEARL", "DASH_STRIKE", 0.8, 7.0, 0, 0, false, false, false, false, false, "ENTITY_ENDERMAN_TELEPORT", "PORTAL");
        active(yaml, "smoke_bomb", "Smoke Bomb", "ROGUE", 15000, "GUNPOWDER", "SHIELD_SELF", 0, 0, 0, 60, false, false, false, false, false, "ENTITY_PHANTOM_FLAP", "LARGE_SMOKE");
        active(yaml, "execute", "Execute", "ROGUE", 18000, "NETHERITE_SWORD", "SINGLE_TARGET_STRIKE", 1.4, 8.0, 0, 0, false, false, false, false, false, "ENTITY_PLAYER_ATTACK_STRONG", "CRIT");
        active(yaml, "poison_blade", "Poison Blade", "ROGUE", 10000, "SPIDER_EYE", "SINGLE_TARGET_STRIKE", 0.6, 6.0, 0, 100, false, false, false, true, false, "ENTITY_SPIDER_HURT", "ITEM_SLIME");
        active(yaml, "whirling_blades", "Whirling Blades", "ROGUE", 14000, "GOLDEN_SWORD", "CONE_SWEEP", 0.7, 6.0, 4.0, 0, false, false, false, false, false, "ENTITY_PLAYER_ATTACK_SWEEP", "SWEEP_ATTACK");
        active(yaml, "bloodletting", "Bloodletting", "ROGUE", 11000, "REDSTONE", "SINGLE_TARGET_STRIKE", 0.7, 6.0, 0, 0, false, false, false, false, true, "ENTITY_PLAYER_ATTACK_CRIT", "CRIMSON_SPORE");
        active(yaml, "vanish_strike", "Vanish Strike", "ROGUE", 17000, "BLACK_DYE", "SINGLE_TARGET_STRIKE", 1.1, 9.0, 0, 0, false, false, false, false, false, "ENTITY_ENDERMAN_TELEPORT", "SMOKE");
        active(yaml, "adrenaline_rush", "Adrenaline Rush", "ROGUE", 20000, "SUGAR", "BUFF_SELF_SPEED", 0, 0, 0, 160, false, false, false, false, false, "ENTITY_PLAYER_LEVELUP", "CLOUD");

        // MAGE (Spell DPS)
        active(yaml, "arcane_bolt", "Arcane Bolt", "MAGE", 4000, "BLAZE_POWDER", "SINGLE_TARGET_STRIKE", 1.0, 8.0, 0, 0, false, false, false, false, false, "ENTITY_BLAZE_SHOOT", "WITCH");
        active(yaml, "frost_nova", "Frost Nova", "MAGE", 10000, "PACKED_ICE", "AOE_NOVA", 0.5, 5.0, 5.0, 60, false, true, false, false, false, "BLOCK_GLASS_BREAK", "SNOWFLAKE");
        active(yaml, "fireball", "Fireball", "MAGE", 9000, "FIRE_CHARGE", "SINGLE_TARGET_STRIKE", 1.1, 9.0, 0, 100, true, false, false, false, false, "ENTITY_BLAZE_AMBIENT", "FLAME");
        active(yaml, "chain_lightning", "Chain Lightning", "MAGE", 13000, "LIGHTNING_ROD", "PIERCING_BEAM", 0.8, 7.0, 12.0, 0, false, false, false, false, false, "ENTITY_LIGHTNING_BOLT_THUNDER", "ELECTRIC_SPARK");
        active(yaml, "arcane_shield", "Arcane Shield", "MAGE", 18000, "AMETHYST_SHARD", "SHIELD_SELF", 0, 0, 0, 200, false, false, false, false, false, "BLOCK_AMETHYST_BLOCK_CHIME", "END_ROD");
        active(yaml, "meteor", "Meteor", "MAGE", 22000, "MAGMA_CREAM", "AOE_NOVA", 1.2, 12.0, 4.0, 100, true, false, false, false, false, "ENTITY_GENERIC_EXPLODE", "EXPLOSION_EMITTER");
        active(yaml, "root_of_thorns", "Root of Thorns", "MAGE", 12000, "OAK_SAPLING", "SINGLE_TARGET_STRIKE", 0.5, 4.0, 0, 100, false, false, true, false, false, "BLOCK_GRASS_BREAK", "COMPOSTER");
        active(yaml, "mana_burn", "Mana Burn", "MAGE", 11000, "GLOWSTONE_DUST", "SINGLE_TARGET_STRIKE", 0.7, 6.0, 0, 80, false, false, false, true, false, "BLOCK_GLASS_BREAK", "WITCH");
        active(yaml, "blink", "Blink", "MAGE", 9000, "ENDER_EYE", "DASH_STRIKE", 0.4, 3.0, 0, 0, false, false, false, false, false, "ENTITY_ENDERMAN_TELEPORT", "PORTAL");
        active(yaml, "arcane_intellect", "Arcane Intellect", "MAGE", 20000, "BOOK", "BUFF_SELF_SPEED", 0, 0, 0, 200, false, false, false, false, false, "BLOCK_ENCHANTMENT_TABLE_USE", "ENCHANT");

        // HEALER (Support)
        active(yaml, "healing_wave", "Healing Wave", "HEALER", 5000, "GOLDEN_APPLE", "HEAL_GROUP", 1.0, 6.0, 8.0, 0, false, false, false, false, false, "ENTITY_EVOKER_PREPARE_WOLOLO", "HEART");
        active(yaml, "purify", "Purify", "HEALER", 12000, "MILK_BUCKET", "CLEANSE_GROUP", 0, 0, 8.0, 0, false, false, false, false, false, "BLOCK_BEACON_POWER_SELECT", "HAPPY_VILLAGER");
        active(yaml, "divine_shield", "Divine Shield", "HEALER", 20000, "TOTEM_OF_UNDYING", "SHIELD_SELF", 0, 0, 0, 200, false, false, false, false, false, "ITEM_TOTEM_USE", "TOTEM_OF_UNDYING");
        active(yaml, "renew", "Renew", "HEALER", 9000, "GLISTERING_MELON_SLICE", "HEAL_SELF", 1.2, 8.0, 0, 0, false, false, false, false, false, "ENTITY_PLAYER_LEVELUP", "HEART");
        active(yaml, "smite", "Smite", "HEALER", 8000, "GOLDEN_SWORD", "SINGLE_TARGET_STRIKE", 0.7, 6.0, 0, 0, false, false, false, false, false, "ENTITY_PLAYER_ATTACK_STRONG", "END_ROD");
        active(yaml, "circle_of_life", "Circle of Life", "HEALER", 25000, "BEACON", "HEAL_GROUP", 1.5, 10.0, 10.0, 0, false, false, false, false, false, "BLOCK_BEACON_ACTIVATE", "HEART");
        active(yaml, "guardian_spirit", "Guardian Spirit", "HEALER", 16000, "SHIELD", "BUFF_GROUP_STRENGTH", 0, 0, 8.0, 160, false, false, false, false, false, "ENTITY_ILLUSIONER_CAST_SPELL", "END_ROD");
        active(yaml, "consecration", "Consecration", "HEALER", 14000, "GLOWSTONE", "AOE_NOVA", 0.6, 5.0, 4.5, 0, false, false, false, false, false, "BLOCK_BEACON_AMBIENT", "SOUL_FIRE_FLAME");
        active(yaml, "spirit_link", "Spirit Link", "HEALER", 18000, "LEAD", "HEAL_GROUP", 0.8, 5.0, 10.0, 0, false, false, false, false, false, "ENTITY_EVOKER_PREPARE_WOLOLO", "WITCH");
        active(yaml, "blessing_of_light", "Blessing of Light", "HEALER", 22000, "NETHER_STAR", "BUFF_GROUP_STRENGTH", 0, 0, 10.0, 200, false, false, false, false, false, "BLOCK_BEACON_POWER_SELECT", "END_ROD");

        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            Logger.getLogger("PixelRPG").log(Level.SEVERE, "Failed to write default active gems", e);
        }
    }

    private static void active(YamlConfiguration yaml, String id, String name, String cls, long cd, String icon,
                                String action, double dmgMult, double flatBase, double radius, int duration,
                                boolean burn, boolean slow, boolean root, boolean weaken, boolean lifesteal,
                                String sound, String particle) {
        String p = "gems." + id;
        yaml.set(p + ".name", name);
        yaml.set(p + ".description", name + " skill for " + cls.toLowerCase() + "s.");
        yaml.set(p + ".class", cls);
        yaml.set(p + ".cooldown-ms", cd);
        yaml.set(p + ".icon", icon);
        yaml.set(p + ".action", action);
        yaml.set(p + ".damage-multiplier", dmgMult);
        yaml.set(p + ".flat-base", flatBase);
        yaml.set(p + ".radius", radius);
        yaml.set(p + ".effect-duration-ticks", duration);
        yaml.set(p + ".burn", burn);
        yaml.set(p + ".slow", slow);
        yaml.set(p + ".root", root);
        yaml.set(p + ".weaken", weaken);
        yaml.set(p + ".lifesteal", lifesteal);
        yaml.set(p + ".sound", sound);
        yaml.set(p + ".particle", particle);
    }

    public static void writePassiveDefaults(File file) {
        YamlConfiguration yaml = new YamlConfiguration();

        String[] modifiers = {"MAX_HEALTH", "ARMOR", "BONUS_DAMAGE", "CRIT_CHANCE", "CRIT_DAMAGE_MULT", "MOVEMENT_SPEED", "LIFESTEAL"};
        String[] icons = {"APPLE", "SHIELD", "IRON_SWORD", "AMETHYST_SHARD", "BLAZE_POWDER", "SUGAR", "REDSTONE"};
        double[] smallValues = {6.0, 2.0, 1.5, 3.0, 0.05, 0.01, 2.0};
        double[] mediumValues = {12.0, 4.0, 3.0, 6.0, 0.10, 0.02, 4.0};
        double[] largeValues = {20.0, 7.0, 5.5, 10.0, 0.18, 0.035, 7.0};

        int index = 1;
        for (int tier = 0; tier < 3; tier++) {
            double[] values = tier == 0 ? smallValues : tier == 1 ? mediumValues : largeValues;
            String tierLabel = tier == 0 ? "Minor" : tier == 1 ? "Greater" : "Major";
            for (int i = 0; i < modifiers.length; i++) {
                String id = "universal_" + modifiers[i].toLowerCase() + "_" + tier;
                passive(yaml, id, tierLabel + " " + prettify(modifiers[i]), icons[i], modifiers[i], values[i]);
                index++;
            }
        }

        String[] classes = {"WARRIOR", "RANGER", "ROGUE", "MAGE", "HEALER"};
        String[][] classModifiers = {
                {"ARMOR", "MAX_HEALTH", "BONUS_DAMAGE"},
                {"CRIT_CHANCE", "MOVEMENT_SPEED", "BONUS_DAMAGE"},
                {"CRIT_DAMAGE_MULT", "CRIT_CHANCE", "LIFESTEAL"},
                {"BONUS_DAMAGE", "CRIT_DAMAGE_MULT", "MAX_HEALTH"},
                {"MAX_HEALTH", "ARMOR", "MOVEMENT_SPEED"}
        };
        String[] classIcons = {"NETHERITE_CHESTPLATE", "BOW", "IRON_SWORD", "BLAZE_ROD", "GOLDEN_APPLE"};

        for (int c = 0; c < classes.length; c++) {
            for (int m = 0; m < classModifiers[c].length; m++) {
                for (int tier = 1; tier <= 3; tier++) {
                    String id = classes[c].toLowerCase() + "_sigil_" + classModifiers[c][m].toLowerCase() + "_" + tier;
                    double value = baseValueFor(classModifiers[c][m]) * tier;
                    passive(yaml, id, classes[c] + " Sigil of " + prettify(classModifiers[c][m]) + " " + toRoman(tier),
                            classIcons[c], classModifiers[c][m], value);
                }
            }
        }

        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            Logger.getLogger("PixelRPG").log(Level.SEVERE, "Failed to write default passive gems", e);
        }
    }

    private static double baseValueFor(String modifier) {
        return switch (modifier) {
            case "MAX_HEALTH" -> 5.0;
            case "ARMOR" -> 2.0;
            case "BONUS_DAMAGE" -> 1.5;
            case "CRIT_CHANCE" -> 3.0;
            case "CRIT_DAMAGE_MULT" -> 0.06;
            case "MOVEMENT_SPEED" -> 0.008;
            case "LIFESTEAL" -> 2.0;
            default -> 1.0;
        };
    }

    private static void passive(YamlConfiguration yaml, String id, String name, String icon, String modifier, double value) {
        String p = "gems." + id;
        yaml.set(p + ".name", name + " Gem");
        yaml.set(p + ".description", "Passively grants " + value + " " + prettify(modifier) + ".");
        yaml.set(p + ".icon", icon);
        yaml.set(p + ".modifier", modifier);
        yaml.set(p + ".value", value);
    }

    private static String prettify(String raw) {
        String[] words = raw.replace('_', ' ').toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private static String toRoman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(value);
        };
    }
}