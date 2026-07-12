// src/main/java/de/pixelrpg/rpg/dungeon/SchematicData.java
package de.pixelrpg.rpg.dungeon;

public final class SchematicData {

    private final int width;
    private final int height;
    private final int length;
    private final String[] blockData;

    public SchematicData(int width, int height, int length, String[] blockData) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.blockData = blockData;
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

    public String getBlockDataAt(int x, int y, int z) {
        int index = x + y * width + z * width * height;
        return blockData[index];
    }

    public int getVolume() {
        return blockData.length;
    }
}