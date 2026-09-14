package de.pixelrpg.rpg.npc;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/** Applies directly usable Minecraft texture URLs to mannequins without external skin-generation services. */
public final class ExternalSkinService {
    private static final String TEXTURES_HOST = "textures.minecraft.net";
    private static final String TEXTURE_PATH_PREFIX = "/texture/";

    private final Plugin plugin;
    private final Logger logger;

    public ExternalSkinService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public CompletableFuture<Void> apply(Mannequin mannequin, String skinSource) {
        if (mannequin == null || !mannequin.isValid()) return CompletableFuture.completedFuture(null);

        String normalized = normalizeUrl(skinSource);
        if (normalized == null || !isMinecraftTextureUrl(normalized)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Only https://textures.minecraft.net/texture/<64-character-hash> URLs are supported without a skin API"));
        }

        ProfileProperty property = unsignedTextureProperty(normalized);
        CompletableFuture<Void> result = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!mannequin.isValid()) {
                    result.complete(null);
                    return;
                }

                mannequin.setProfile(ResolvableProfile.resolvableProfile().addProperty(property).build());
                refreshForNearbyPlayers(mannequin);
                result.complete(null);
            } catch (RuntimeException exception) {
                logger.warning("Failed to apply mannequin texture '" + normalized + "': " + exception.getMessage());
                result.completeExceptionally(exception);
            }
        });

        return result;
    }

    private static void refreshForNearbyPlayers(Mannequin mannequin) {
        for (Entity entity : mannequin.getNearbyEntities(64.0D, 64.0D, 64.0D)) {
            if (entity instanceof Player player) {
                player.hideEntity(mannequin.getServer().getPluginManager().getPlugin("PixelRPG"), mannequin);
                player.showEntity(mannequin.getServer().getPluginManager().getPlugin("PixelRPG"), mannequin);
            }
        }
    }

    private static ProfileProperty unsignedTextureProperty(String textureUrl) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + escapeJson(textureUrl) + "\"}}}";
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return new ProfileProperty("textures", encoded);
    }

    private static boolean isMinecraftTextureUrl(String value) {
        URI uri = URI.create(value);
        return "https".equalsIgnoreCase(uri.getScheme())
                && TEXTURES_HOST.equalsIgnoreCase(uri.getHost())
                && uri.getPath() != null
                && uri.getPath().matches(TEXTURE_PATH_PREFIX + "[0-9a-fA-F]{64}/?");
    }

    private static String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > 2048) return null;
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return null;
            if (uri.getUserInfo() != null || uri.getFragment() != null) return null;
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
