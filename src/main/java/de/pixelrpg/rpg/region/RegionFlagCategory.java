package de.pixelrpg.rpg.region;

/** Groups region flags by the gameplay area they control. */
public enum RegionFlagCategory {
    GENERAL("Allgemein"),
    BLOCKS("Blöcke"),
    INTERACTION("Interaktion"),
    CONTAINERS("Container"),
    COMBAT("Kampf & Schaden"),
    MOB_SPAWN("MobSpawn"),
    ENVIRONMENT("Umwelt"),
    EXPLOSIONS("Explosionen"),
    MOVEMENT("Bewegung & Teleport"),
    PLAYER("Spieler"),
    LEGACY("Legacy");

    private final String displayName;

    RegionFlagCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
