package de.pixelrpg.rpg.npc;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Resolves Mojang player names and direct external skin URLs for mannequins. */
public final class MannequinSkinResolver {
    private MannequinSkinResolver() {
    }

    public static void apply(Mannequin mannequin, String skinSource, Logger logger) {
        if (skinSource == null || skinSource.isBlank() || !mannequin.isValid()) return;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PixelRPG");
        if (plugin == null) {
            logger.warning("Cannot resolve mannequin skin '" + skinSource + "': PixelRPG plugin is not loaded.");
            return;
        }
        try {
            String source = skinSource.trim();
            if (source.startsWith("http://") || source.startsWith("https://")) {
                applySkinUrl(mannequin, source, plugin);
            } else {
                applyPlayerName(mannequin, source, plugin, logger);
            }
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Failed to start mannequin skin resolution for '" + skinSource + "'", exception);
        }
    }

    private static void applyPlayerName(Mannequin mannequin, String playerName, Plugin plugin, Logger logger) {
        ResolvableProfile profile = ResolvableProfile.resolvableProfile().name(playerName).build();
        mannequin.setProfile(profile);
        profile.resolve().thenAcceptAsync(updatedProfile -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (mannequin.isValid()) mannequin.setProfile(ResolvableProfile.resolvableProfile(updatedProfile));
        })).exceptionally(exception -> {
            logger.log(Level.WARNING, "Failed to resolve player skin for mannequin '" + playerName + "'", exception);
            return null;
        });
    }

    /** Creates an unsigned Minecraft textures property that points directly at the supplied PNG URL. */
    private static void applySkinUrl(Mannequin mannequin, String skinUrl, Plugin plugin) {
        URI uri = URI.create(skinUrl);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Skin URL must use http or https");
        }

        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + escapeJson(skinUrl) + "\"}}}";
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        PlayerProfile profile = Bukkit.createProfile("PixelRPGSkin");
        profile.setProperty(new ProfileProperty("textures", encoded));
        ResolvableProfile resolved = ResolvableProfile.resolvableProfile(profile);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (mannequin.isValid()) mannequin.setProfile(resolved);
        });
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
