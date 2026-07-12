// src/main/java/de/pixelrpg/rpg/dungeon/RelativeMarker.java
package de.pixelrpg.rpg.dungeon;

public final class RelativeMarker {

    private final DungeonMarkerType type;
    private final int dx;
    private final int dy;
    private final int dz;
    private final String mobType;

    public RelativeMarker(DungeonMarkerType type, int dx, int dy, int dz, String mobType) {
        this.type = type;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.mobType = mobType;
    }

    public DungeonMarkerType getType() {
        return type;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getDz() {
        return dz;
    }

    public String getMobType() {
        return mobType;
    }
}