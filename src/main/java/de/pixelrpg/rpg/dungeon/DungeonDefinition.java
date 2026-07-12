// src/main/java/de/pixelrpg/rpg/dungeon/DungeonDefinition.java
package de.pixelrpg.rpg.dungeon;

import de.pixelrpg.rpg.core.Rank;

import java.util.ArrayList;
import java.util.List;

public final class DungeonDefinition {

    private final String id;
    private String displayName;
    private Rank minRank;
    private Rank maxRank;
    private DungeonInstanceMode instanceMode;
    private int cooldownMinutes;

    private String captureWorld;
    private int originX;
    private int originY;
    private int originZ;
    private int width;
    private int height;
    private int length;
    private String schematicFile;

    private final List<RelativeMarker> markers = new ArrayList<>();

    public DungeonDefinition(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.minRank = Rank.F;
        this.maxRank = Rank.S;
        this.instanceMode = DungeonInstanceMode.INSTANCED;
        this.cooldownMinutes = 30;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Rank getMinRank() {
        return minRank;
    }

    public void setMinRank(Rank minRank) {
        this.minRank = minRank;
    }

    public Rank getMaxRank() {
        return maxRank;
    }

    public void setMaxRank(Rank maxRank) {
        this.maxRank = maxRank;
    }

    public DungeonInstanceMode getInstanceMode() {
        return instanceMode;
    }

    public void setInstanceMode(DungeonInstanceMode instanceMode) {
        this.instanceMode = instanceMode;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public String getCaptureWorld() {
        return captureWorld;
    }

    public void setCaptureWorld(String captureWorld) {
        this.captureWorld = captureWorld;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }

    public void setOrigin(int x, int y, int z) {
        this.originX = x;
        this.originY = y;
        this.originZ = z;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public void setDimensions(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
    }

    public String getSchematicFile() {
        return schematicFile;
    }

    public void setSchematicFile(String schematicFile) {
        this.schematicFile = schematicFile;
    }

    public List<RelativeMarker> getMarkers() {
        return markers;
    }

    public void addMarker(RelativeMarker marker) {
        markers.add(marker);
    }

    public boolean hasSchematic() {
        return schematicFile != null;
    }
}