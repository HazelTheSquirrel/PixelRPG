package de.pixelrpg.rpg.resourcepack;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.util.UUID;

public final class ResourcePackJoinListener implements Listener {
    private static final UUID RESOURCE_PACK_ID = UUID.fromString("2f0d3d2d-6d0d-4c1d-8a5a-6d0d3d2d6d0d");
    private static final URI RESOURCE_PACK_URI = URI.create(
            "https://raw.githubusercontent.com/HazelTheSquirrel/PixelRPG/test/resourcepack/PixelRPG-resourcepack.zip"
    );

    private final JavaPlugin plugin;
    private final String sha1;

    public ResourcePackJoinListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.sha1 = loadSha1();
    }

    // Sends the current PixelRPG resource pack when a player joins the server.
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (sha1 == null) {
            return;
        }

        ResourcePackInfo pack = ResourcePackInfo.resourcePackInfo(
                RESOURCE_PACK_ID,
                RESOURCE_PACK_URI,
                sha1
        );

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(pack)
                .replace(true)
                .required(false)
                .prompt(Component.text("PixelRPG Resource Pack"))
                .build();

        Audience audience = event.getPlayer();
        audience.sendResourcePacks(request);
    }

    private String loadSha1() {
        try (var input = plugin.getResource("resourcepack.sha1")) {
            if (input == null) {
                plugin.getLogger().warning("Resource pack hash resourcepack.sha1 is missing.");
                return null;
            }

            String value = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII).trim();
            if (!value.matches("[0-9a-fA-F]{40}") || value.chars().allMatch(c -> c == '0')) {
                plugin.getLogger().warning("Resource pack hash resourcepack.sha1 is invalid or not generated yet.");
                return null;
            }
            return value.toLowerCase(java.util.Locale.ROOT);
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not load resource pack hash: " + exception.getMessage());
            return null;
        }
    }
}
