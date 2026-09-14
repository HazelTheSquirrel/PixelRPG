package de.pixelrpg.rpg.region;

import java.util.Arrays;
import java.util.List;

/** Fine-grained region permissions grouped by gameplay category. */
public enum RegionFlag {
    ENTRY(RegionFlagCategory.GENERAL, "Region betreten"),
    EXIT(RegionFlagCategory.GENERAL, "Region verlassen"),

    BLOCK_BREAK(RegionFlagCategory.BLOCKS, "Blöcke abbauen"),
    BLOCK_PLACE(RegionFlagCategory.BLOCKS, "Blöcke platzieren"),
    BLOCK_TRAMPLING(RegionFlagCategory.BLOCKS, "Blöcke zertrampeln"),
    CROP_GROWTH(RegionFlagCategory.BLOCKS, "Pflanzenwachstum"),
    LEAF_DECAY(RegionFlagCategory.BLOCKS, "Laubzerfall"),

    DOOR_USE(RegionFlagCategory.INTERACTION, "Türen benutzen"),
    TRAPDOOR_USE(RegionFlagCategory.INTERACTION, "Falltüren benutzen"),
    FENCE_GATE_USE(RegionFlagCategory.INTERACTION, "Zauntore benutzen"),
    BUTTON_USE(RegionFlagCategory.INTERACTION, "Knöpfe benutzen"),
    LEVER_USE(RegionFlagCategory.INTERACTION, "Hebel benutzen"),
    PRESSURE_PLATE_USE(RegionFlagCategory.INTERACTION, "Druckplatten benutzen"),
    ENTITY_INTERACTION(RegionFlagCategory.INTERACTION, "Entities interagieren"),

    CHEST_USE(RegionFlagCategory.CONTAINERS, "Kisten öffnen"),
    BARREL_USE(RegionFlagCategory.CONTAINERS, "Fässer öffnen"),
    SHULKER_BOX_USE(RegionFlagCategory.CONTAINERS, "Shulkerboxen öffnen"),
    HOPPER_USE(RegionFlagCategory.CONTAINERS, "Trichter benutzen"),
    DROPPER_USE(RegionFlagCategory.CONTAINERS, "Dropper benutzen"),
    DISPENSER_USE(RegionFlagCategory.CONTAINERS, "Dispenser benutzen"),
    FURNACE_USE(RegionFlagCategory.CONTAINERS, "Öfen benutzen"),
    BLAST_FURNACE_USE(RegionFlagCategory.CONTAINERS, "Schmelzöfen benutzen"),
    SMOKER_USE(RegionFlagCategory.CONTAINERS, "Räucheröfen benutzen"),
    BREWING_STAND_USE(RegionFlagCategory.CONTAINERS, "Braustände benutzen"),
    ENCHANTING_TABLE_USE(RegionFlagCategory.CONTAINERS, "Zaubertische benutzen"),
    CRAFTING_TABLE_USE(RegionFlagCategory.CONTAINERS, "Werkbänke benutzen"),
    ANVIL_USE(RegionFlagCategory.CONTAINERS, "Ambosse benutzen"),

    PVP(RegionFlagCategory.COMBAT, "PvP"),
    MOB_DAMAGE(RegionFlagCategory.COMBAT, "Mob-Schaden"),
    DAMAGE_ANIMALS(RegionFlagCategory.COMBAT, "Tierschaden"),
    FALL_DAMAGE(RegionFlagCategory.COMBAT, "Fallschaden"),

    MOB_SPAWNING(RegionFlagCategory.MOB_SPAWN, "Mob-Spawning"),
    DENY_SPAWN(RegionFlagCategory.MOB_SPAWN, "Spawning vollständig verbieten"),

    FIRE_SPREAD(RegionFlagCategory.ENVIRONMENT, "Feuerausbreitung"),
    LAVA_FLOW(RegionFlagCategory.ENVIRONMENT, "Lavafluss"),
    WATER_FLOW(RegionFlagCategory.ENVIRONMENT, "Wasserfluss"),
    LIGHTNING(RegionFlagCategory.ENVIRONMENT, "Blitze"),

    EXPLOSION(RegionFlagCategory.EXPLOSIONS, "Explosionen"),
    TNT(RegionFlagCategory.EXPLOSIONS, "TNT"),
    CREEPER_EXPLOSION(RegionFlagCategory.EXPLOSIONS, "Creeper-Explosionen"),
    GHAST_FIREBALL(RegionFlagCategory.EXPLOSIONS, "Ghast-Feuerbälle"),
    ENDERMAN_GRIEF(RegionFlagCategory.EXPLOSIONS, "Enderman-Griefing"),

    ITEM_DROP(RegionFlagCategory.PLAYER, "Items droppen"),
    ITEM_PICKUP(RegionFlagCategory.PLAYER, "Items aufheben"),
    NATURAL_HEALTH_REGEN(RegionFlagCategory.PLAYER, "Natürliche Heilung"),
    NATURAL_HUNGER_DRAIN(RegionFlagCategory.PLAYER, "Natürlicher Hunger"),
    SLEEP(RegionFlagCategory.PLAYER, "Schlafen"),
    RESPAWN_ANCHORS(RegionFlagCategory.PLAYER, "Respawn-Anker"),
    ENDERPEARL(RegionFlagCategory.PLAYER, "Enderperlen-Teleport"),
    CHORUS_FRUIT_TELEPORT(RegionFlagCategory.PLAYER, "Chorusfrucht-Teleport");

    private final RegionFlagCategory category;
    private final String displayName;

    RegionFlag(RegionFlagCategory category, String displayName) {
        this.category = category;
        this.displayName = displayName;
    }

    public RegionFlagCategory category() {
        return category;
    }

    public String displayName() {
        return displayName;
    }

    public static List<RegionFlag> forCategory(RegionFlagCategory category) {
        return Arrays.stream(values()).filter(flag -> flag.category == category).toList();
    }
}
