package de.pixelrpg.rpg.region;

import java.util.Arrays;
import java.util.List;

/** Fine-grained region permissions grouped by gameplay category. */
public enum RegionFlag {
    ENTRY(RegionFlagCategory.REGION, "Region betreten"),
    EXIT(RegionFlagCategory.REGION, "Region verlassen"),

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
    NOTE_BLOCK_USE(RegionFlagCategory.INTERACTION, "Notenblöcke benutzen"),
    JUKEBOX_USE(RegionFlagCategory.INTERACTION, "Jukeboxen benutzen"),
    COMPOSTER_USE(RegionFlagCategory.INTERACTION, "Komposter benutzen"),
    LECTERN_USE(RegionFlagCategory.INTERACTION, "Lesepulte benutzen"),
    BEEHIVE_USE(RegionFlagCategory.INTERACTION, "Bienenstöcke benutzen"),
    BEE_NEST_USE(RegionFlagCategory.INTERACTION, "Bienennester benutzen"),
    CAKE_USE(RegionFlagCategory.INTERACTION, "Kuchen benutzen"),
    ITEM_FRAME_USE(RegionFlagCategory.INTERACTION, "Itemrahmen benutzen"),
    ARMOR_STAND_USE(RegionFlagCategory.INTERACTION, "Rüstungsständer benutzen"),
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
    GRINDSTONE_USE(RegionFlagCategory.CONTAINERS, "Schleifsteine benutzen"),
    STONECUTTER_USE(RegionFlagCategory.CONTAINERS, "Steinsägen benutzen"),
    LOOM_USE(RegionFlagCategory.CONTAINERS, "Webstühle benutzen"),
    CARTOGRAPHY_TABLE_USE(RegionFlagCategory.CONTAINERS, "Kartentische benutzen"),
    SMITHING_TABLE_USE(RegionFlagCategory.CONTAINERS, "Schmiedetische benutzen"),

    PVP(RegionFlagCategory.COMBAT, "PvP"),
    MOB_DAMAGE(RegionFlagCategory.COMBAT, "Mob-Schaden"),
    DAMAGE_ANIMALS(RegionFlagCategory.COMBAT, "Tierschaden"),
    FALL_DAMAGE(RegionFlagCategory.COMBAT, "Fallschaden"),

    MOB_SPAWNING(RegionFlagCategory.MOB_SPAWN, "Mob-Spawning"),
    DENY_SPAWN(RegionFlagCategory.MOB_SPAWN, "Spawning vollständig verbieten"),
    SPAWN_BOGGED(RegionFlagCategory.MOB_SPAWN, "Bogged"),
    SPAWN_PARCHED(RegionFlagCategory.MOB_SPAWN, "Parched"),
    SPAWN_SKELETON(RegionFlagCategory.MOB_SPAWN, "Skelett"),
    SPAWN_STRAY(RegionFlagCategory.MOB_SPAWN, "Stray"),
    SPAWN_WITHER_SKELETON(RegionFlagCategory.MOB_SPAWN, "Wither-Skelett"),
    SPAWN_BLAZE(RegionFlagCategory.MOB_SPAWN, "Blaze"),
    SPAWN_BREEZE(RegionFlagCategory.MOB_SPAWN, "Breeze"),
    SPAWN_CREAKING(RegionFlagCategory.MOB_SPAWN, "Creaking"),
    SPAWN_CREEPER(RegionFlagCategory.MOB_SPAWN, "Creeper"),
    SPAWN_ENDERMAN(RegionFlagCategory.MOB_SPAWN, "Enderman"),
    SPAWN_ENDERMITE(RegionFlagCategory.MOB_SPAWN, "Endermite"),
    SPAWN_GHAST(RegionFlagCategory.MOB_SPAWN, "Ghast"),
    SPAWN_GIANT(RegionFlagCategory.MOB_SPAWN, "Giant"),
    SPAWN_GUARDIAN(RegionFlagCategory.MOB_SPAWN, "Guardian"),
    SPAWN_ELDER_GUARDIAN(RegionFlagCategory.MOB_SPAWN, "Elder Guardian"),
    SPAWN_HOGLIN(RegionFlagCategory.MOB_SPAWN, "Hoglin"),
    SPAWN_MAGMA_CUBE(RegionFlagCategory.MOB_SPAWN, "Magmawürfel"),
    SPAWN_PHANTOM(RegionFlagCategory.MOB_SPAWN, "Phantom"),
    SPAWN_SLIME(RegionFlagCategory.MOB_SPAWN, "Schleim"),
    SPAWN_PIGLIN(RegionFlagCategory.MOB_SPAWN, "Piglin"),
    SPAWN_PIGLIN_BRUTE(RegionFlagCategory.MOB_SPAWN, "Piglin Brute"),
    SPAWN_PILLAGER(RegionFlagCategory.MOB_SPAWN, "Pillager"),
    SPAWN_EVOKER(RegionFlagCategory.MOB_SPAWN, "Evoker"),
    SPAWN_ILLUSIONER(RegionFlagCategory.MOB_SPAWN, "Illusioner"),
    SPAWN_VINDICATOR(RegionFlagCategory.MOB_SPAWN, "Vindicator"),
    SPAWN_RAVAGER(RegionFlagCategory.MOB_SPAWN, "Ravager"),
    SPAWN_WITCH(RegionFlagCategory.MOB_SPAWN, "Hexe"),
    SPAWN_SILVERFISH(RegionFlagCategory.MOB_SPAWN, "Silberfisch"),
    SPAWN_SPIDER(RegionFlagCategory.MOB_SPAWN, "Spinne"),
    SPAWN_CAVE_SPIDER(RegionFlagCategory.MOB_SPAWN, "Höhlenspinne"),
    SPAWN_VEX(RegionFlagCategory.MOB_SPAWN, "Vex"),
    SPAWN_WARDEN(RegionFlagCategory.MOB_SPAWN, "Warden"),
    SPAWN_WITHER(RegionFlagCategory.MOB_SPAWN, "Wither"),
    SPAWN_ZOGLIN(RegionFlagCategory.MOB_SPAWN, "Zoglin"),
    SPAWN_ZOMBIE(RegionFlagCategory.MOB_SPAWN, "Zombie"),
    SPAWN_DROWNED(RegionFlagCategory.MOB_SPAWN, "Ertrunkener"),
    SPAWN_HUSK(RegionFlagCategory.MOB_SPAWN, "Husk"),
    SPAWN_PIG_ZOMBIE(RegionFlagCategory.MOB_SPAWN, "Zombifizierter Piglin"),
    SPAWN_ZOMBIE_VILLAGER(RegionFlagCategory.MOB_SPAWN, "Zombie-Dorfbewohner"),

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

    RESPAWN_ANCHORS(RegionFlagCategory.MOVEMENT, "Respawn-Anker"),
    ENDERPEARL(RegionFlagCategory.MOVEMENT, "Enderperlen-Teleport"),
    CHORUS_FRUIT_TELEPORT(RegionFlagCategory.MOVEMENT, "Chorusfrucht-Teleport"),

    @Deprecated
    INTERACT(RegionFlagCategory.LEGACY, "Legacy: Entity-Interaktion"),
    @Deprecated
    USE(RegionFlagCategory.LEGACY, "Legacy: Block-/Item-Nutzung"),
    @Deprecated
    CHEST_ACCESS(RegionFlagCategory.LEGACY, "Legacy: Container-Zugriff"),
    @Deprecated
    MONSTER_SPAWN(RegionFlagCategory.LEGACY, "Legacy: Monster-Spawns");

    private final RegionFlagCategory category;
    private final String displayName;

    RegionFlag(RegionFlagCategory category, String displayName) {
        this.category = category;
        this.displayName = displayName;
    }

    public RegionFlagCategory category() { return category; }
    public String displayName() { return displayName; }

    public static List<RegionFlag> forCategory(RegionFlagCategory category) {
        return Arrays.stream(values()).filter(flag -> flag.category == category).toList();
    }
}
