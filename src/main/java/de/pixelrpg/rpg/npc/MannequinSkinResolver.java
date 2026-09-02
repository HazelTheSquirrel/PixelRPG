package de.pixelrpg.rpg.npc;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

/** Resolves Mojang player names and external skin URLs for mannequins. */
public final class MannequinSkinResolver {
    private MannequinSkinResolver() {
    }

    public static void apply(Mannequin mannequin, String skinSource, Logger logger) {
        if (skinSource == null || skinSource.isBlank() || !mannequin.isValid()) return;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("PixelRPG");
        if (plugin == null) {
            logger.warning("Cannot resolve mannequin skin: PixelRPG plugin is not loaded.");
            return;
        }

        String source = skinSource.trim();
        try {
            if (isUrl(source)) {
                String normalizedUrl = normalizeUrl(source);
                new ExternalSkinService(plugin).apply(mannequin, normalizedUrl)
                        .exceptionally(exception -> {
                            Throwable cause = unwrap(exception);
                            logger.warning("Failed to resolve external mannequin skin '" + normalizedUrl + "': "
                                    + message(cause));
                            return null;
                        });
                return;
            }

            if (looksLikeUrl(source)) {
                logger.warning("Invalid mannequin skin URL '" + source + "'. Expected http(s)://host/path or a Minecraft player name.");
                return;
            }

            applyPlayerName(mannequin, source, plugin, logger);
        } catch (RuntimeException exception) {
            logger.warning("Failed to start mannequin skin resolution for '" + source + "': " + message(exception));
        }
    }

    private static void applyPlayerName(Mannequin mannequin, String playerName, Plugin plugin, Logger logger) {
        if (playerName.length() > 16) {
            logger.warning("Invalid mannequin player skin name '" + playerName + "': names cannot exceed 16 characters.");
            return;
        }

        ResolvableProfile profile = ResolvableProfile.resolvableProfile().name(playerName).build();
        mannequin.setProfile(profile);
        profile.resolve().thenAcceptAsync(updatedProfile -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (mannequin.isValid()) mannequin.setProfile(ResolvableProfile.resolvableProfile(updatedProfile));
        })).exceptionally(exception -> {
            Throwable cause = unwrap(exception);
            logger.warning("Failed to resolve player skin for mannequin '" + playerName + "': " + message(cause));
            return null;
        });
    }

    private static boolean isUrl(String source) {
        try {
            URI uri = URI.create(normalizeUrl(source));
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean looksLikeUrl(String source) {
        String lower = source.toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("www.")
                || lower.contains("/")
                || lower.contains("\\")
                || lower.matches("^[a-z0-9.-]+:[0-9]+(?:/.*)?$");
    }

    private static String normalizeUrl(String source) {
        if (source.startsWith("http://") || source.startsWith("https://")) return source;
        return "https://" + source;
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
