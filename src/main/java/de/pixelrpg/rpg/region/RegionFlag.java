package de.pixelrpg.rpg.region;

public enum RegionFlag {
    PVP("PvP"),
    MONSTER_SPAWN("Monster-Spawns"),
    BLOCK_BREAK("Blockabbau"),
    BLOCK_PLACE("Blockplatzierung"),
    FIRE_SPREAD("Feuerausbreitung"),
    LAVA_FLOW("Lavafluss"),
    EXPLOSION("Explosionen"),
    CREEPER_EXPLOSION("Creeper-Explosionen"),
    GHAST_FIREBALL("Ghast-Feuerbälle"),
    ENDERMAN_GRIEF("Enderman-Griefing");

    private final String displayName;

    RegionFlag(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
