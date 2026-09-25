package de.pixelrpg.rpg.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sends region-boundary vertices to JourneyMap through its stable Bukkit/Paper waypoint channel.
 *
 * JourneyMap's public server packet interface exposes waypoints, not polygon overlays.
 * Therefore each region edge is represented by persistent-in-session boundary markers rather
 * than a client-side polygon. No JourneyMap dependency is bundled or required.
 */
public final class RegionMapIntegration implements Listener {
    private static final String CHANNEL = "journeymap:waypoint";
    private static final int DISCRIMINATOR = 0;
    private static final int MAX_BOUNDARY_POINTS = 64;

    private final Plugin plugin;
    private final RegionManager regionManager;

    public RegionMapIntegration(Plugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Sendet die vorhandenen PixelRPG-Grenzpunkte nach dem Join an JourneyMap-Clients.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendRegions(event.getPlayer()), 40L);
    }

    public void sendRegions(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!player.getListeningPluginChannels().contains(CHANNEL)) return;

        for (PixelRegion region : regionManager.all()) {
            World world = Bukkit.getWorld(region.worldName());
            if (world == null || region.geometry() == null) continue;

            var points = region.geometry().points();
            int limit = Math.min(points.size(), MAX_BOUNDARY_POINTS);
            for (int index = 0; index < limit; index++) {
                RegionPoint point = points.get(index);
                int y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, region.minY()));
                sendCreate(player, region, world, point, y, index);
            }
        }
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    private void sendCreate(Player player, PixelRegion region, World world, RegionPoint point, int y, int index) {
        String name = "PixelRPG " + region.name() + " [" + index + "]";
        JsonObject waypoint = new JsonObject();
        waypoint.addProperty("id", name + "_" + (int) point.x() + "," + y + "," + (int) point.z());
        waypoint.addProperty("name", name);
        waypoint.addProperty("icon", "journeymap:ui/img/waypoint-icon.png");
        waypoint.addProperty("enable", true);
        waypoint.addProperty("type", "Normal");
        waypoint.addProperty("origin", "external");
        waypoint.addProperty("x", (int) Math.round(point.x()));
        waypoint.addProperty("y", y);
        waypoint.addProperty("z", (int) Math.round(point.z()));

        int[] rgb = color(region.type());
        waypoint.addProperty("r", rgb[0]);
        waypoint.addProperty("g", rgb[1]);
        waypoint.addProperty("b", rgb[2]);
        waypoint.addProperty("persistent", false);

        JsonArray dimensions = new JsonArray();
        dimensions.add(world.key().asString());
        waypoint.add("dimensions", dimensions);

        send(player, waypoint.toString(), "create", false);
    }

    private void send(Player player, String waypointJson, String action, boolean announce) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(waypointJson.length() + action.length() + 16);
            DataOutputStream out = new DataOutputStream(buffer);
            out.writeByte(DISCRIMINATOR);
            writeMinecraftString(out, waypointJson);
            writeMinecraftString(out, action);
            out.writeBoolean(announce);
            out.flush();
            player.sendPluginMessage(plugin, CHANNEL, buffer.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to serialize JourneyMap region marker: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().fine("JourneyMap channel unavailable for " + player.getName() + ": " + exception.getMessage());
        }
    }

    private static void writeMinecraftString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int[] color(RegionType type) {
        return switch (type) {
            case VILLAGE -> new int[] {80, 200, 120};
            case CITY -> new int[] {80, 140, 255};
            case GUILD_CITY -> new int[] {180, 100, 255};
            case RUINS -> new int[] {180, 150, 90};
            case FORTRESS -> new int[] {220, 90, 70};
            case DUNGEON -> new int[] {150, 80, 220};
            case DANGER_ZONE -> new int[] {255, 70, 50};
            case BOSS_ZONE -> new int[] {255, 30, 30};
            case OTHER, WILDERNESS -> new int[] {180, 180, 180};
        };
    }
}
