package de.pixelrpg.rpg.dungeon;

import de.pixelrpg.rpg.region.CuboidBounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class SchematicIO {

    private SchematicIO() {
    }

    public static SchematicData capture(World world, CuboidBounds bounds) {
        int width = bounds.maxX() - bounds.minX() + 1;
        int height = bounds.maxY() - bounds.minY() + 1;
        int length = bounds.maxZ() - bounds.minZ() + 1;

        String[] data = new String[width * height * length];

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockData blockData = world.getBlockAt(
                            bounds.minX() + x, bounds.minY() + y, bounds.minZ() + z).getBlockData();
                    int index = x + y * width + z * width * height;
                    data[index] = blockData.getAsString();
                }
            }
        }

        return new SchematicData(width, height, length, data);
    }

    public static void save(SchematicData schematic, File file) throws IOException {
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(schematic.getWidth() + ";" + schematic.getHeight() + ";" + schematic.getLength());
            writer.newLine();
            for (int y = 0; y < schematic.getHeight(); y++) {
                for (int z = 0; z < schematic.getLength(); z++) {
                    for (int x = 0; x < schematic.getWidth(); x++) {
                        writer.write(schematic.getBlockDataAt(x, y, z));
                        writer.newLine();
                    }
                }
            }
        }
    }

    public static SchematicData load(File file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            String[] parts = header.split(";");
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            int length = Integer.parseInt(parts[2]);

            List<String> lines = new ArrayList<>(width * height * length);
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            return new SchematicData(width, height, length, lines.toArray(new String[0]));
        }
    }

    /**
     * Setzt die Blöcke des Schematics gestreckt über mehrere Ticks (statt in einem
     * synchronen Durchlauf), um TPS-Einbrüche bei großen Dungeons zu vermeiden.
     * onComplete wird auf dem Main-Thread aufgerufen, sobald alle Blöcke gesetzt sind.
     */
    public static void pasteIncremental(Plugin plugin, SchematicData schematic, Location origin,
                                         int blocksPerTick, Runnable onComplete) {
        World world = origin.getWorld();
        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();
        int width = schematic.getWidth();
        int height = schematic.getHeight();
        int length = schematic.getLength();
        int totalBlocks = width * length * height;
        int safeBlocksPerTick = Math.max(1, blocksPerTick);

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int placedThisTick = 0;
                while (placedThisTick < safeBlocksPerTick && index < totalBlocks) {
                    int y = index / (width * length);
                    int remainder = index % (width * length);
                    int z = remainder / width;
                    int x = remainder % width;

                    String rawBlockData = schematic.getBlockDataAt(x, y, z);
                    BlockData blockData = Bukkit.createBlockData(rawBlockData);
                    world.getBlockAt(baseX + x, baseY + y, baseZ + z).setBlockData(blockData, false);

                    index++;
                    placedThisTick++;
                }

                if (index >= totalBlocks) {
                    cancel();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Entfernt die Blöcke des Schematics gestreckt über mehrere Ticks, analog zu
     * pasteIncremental. Wird beim Instanz-Cleanup nach Ablauf der Lebenszeit genutzt.
     */
    public static void clearIncremental(Plugin plugin, SchematicData schematic, Location origin,
                                         int blocksPerTick, Runnable onComplete) {
        World world = origin.getWorld();
        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();
        int width = schematic.getWidth();
        int height = schematic.getHeight();
        int length = schematic.getLength();
        int totalBlocks = width * length * height;
        int safeBlocksPerTick = Math.max(1, blocksPerTick);

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int clearedThisTick = 0;
                while (clearedThisTick < safeBlocksPerTick && index < totalBlocks) {
                    int y = index / (width * length);
                    int remainder = index % (width * length);
                    int z = remainder / width;
                    int x = remainder % width;

                    world.getBlockAt(baseX + x, baseY + y, baseZ + z).setType(Material.AIR, false);

                    index++;
                    clearedThisTick++;
                }

                if (index >= totalBlocks) {
                    cancel();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}