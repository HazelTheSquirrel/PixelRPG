package de.pixelrpg.rpg.region;

public enum RegionFlag {
    PVP("PvP"),
    MOB_DAMAGE("Mob-Schaden"),
    MONSTER_SPAWN("Monster-Spawns"),
    MOB_SPAWNING("Mob-Spawning"),
    DENY_SPAWN("Spawns verbieten"),
    BLOCK_BREAK("Blockabbau"),
    BLOCK_PLACE("Blockplatzierung"),
    INTERACT("Entity-Interaktion"),
    USE("Block-/Item-Nutzung"),
    CHEST_ACCESS("Container-Zugriff"),
    DAMAGE_ANIMALS("Tierschaden"),
    ITEM_DROP("Item-Drops"),
    ITEM_PICKUP("Item-Aufheben"),
    FALL_DAMAGE("Fallschaden"),
    FIRE_SPREAD("Feuerausbreitung"),
    LAVA_FLOW("Lavafluss"),
    WATER_FLOW("Wasserfluss"),
    EXPLOSION("Explosionen"),
    TNT("TNT"),
    CREEPER_EXPLOSION("Creeper-Explosionen"),
    GHAST_FIREBALL("Ghast-Feuerbälle"),
    ENDERMAN_GRIEF("Enderman-Griefing"),
    LIGHTNING("Blitze"),
    CROP_GROWTH("Ackerfrucht-Wachstum"),
    LEAF_DECAY("Laubzerfall"),
    BLOCK_TRAMPLING("Block-Trampling"),
    USE_ANVIL("Amboss-Nutzung"),
    ENTRY("Region betreten"),
    EXIT("Region verlassen"),
    RESPAWN_ANCHORS("Respawn-Anker"),
    SLEEP("Schlafen"),
    ENDERPEARL("Enderperlen-Teleport"),
    CHORUS_FRUIT_TELEPORT("Chorusfrucht-Teleport"),
    NATURAL_HEALTH_REGEN("Natürliche Heilung"),
    NATURAL_HUNGER_DRAIN("Natürlicher Hunger");

    private final String displayName;

    RegionFlag(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
