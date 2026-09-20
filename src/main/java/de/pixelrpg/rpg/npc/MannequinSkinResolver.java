package de.pixelrpg.rpg.npc;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/** Resolves Mojang player names and external skin URLs for mannequins. */
public final class MannequinSkinResolver {
    private static final ConcurrentMap<String, CompletableFuture<ResolvableProfile>> PLAYER_PROFILE_CACHE = new ConcurrentHashMap<>();

    private MannequinSkinResolver() {
    }

    public static CompletableFuture<Void> apply(Mannequin mannequin, String skinSource, Logger logger) {
        if (skinSource == null || skinSource.isBlank() || !mannequin.isValid()) {
            return CompletableFuture.completedFuture(null);
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("PixelRPG");
        if (plugin == null) {
            logger.warning("Cannot resolve mannequin skin: PixelRPG plugin is not loaded.");
            return CompletableFuture.failedFuture(new IllegalStateException("PixelRPG plugin is not loaded"));
        }

        String source = skinSource.trim();
        try {
            if (isUrl(source)) {
                String normalizedUrl = normalizeUrl(source);
                return new ExternalSkinService(plugin).apply(mannequin, normalizedUrl)
                        .whenComplete((ignored, exception) -> {
                            if (exception != null) {
                                Throwable cause = unwrap(exception);
                                logger.warning("Failed to resolve external mannequin skin '" + normalizedUrl + "': "
                                        + message(cause));
                            }
                        });
            }

            if (looksLikeUrl(source)) {
                logger.warning("Invalid mannequin skin URL '" + source
                        + "'. Expected http(s)://host/path or a Minecraft player name.");
                return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid mannequin skin URL"));
            }

            return applyPlayerName(mannequin, source, plugin, logger);
        } catch (RuntimeException exception) {
            logger.warning("Failed to start mannequin skin resolution for '" + source + "': " + message(exception));
            return CompletableFuture.failedFuture(exception);
        }
    }

    private static CompletableFuture<Void> applyPlayerName(Mannequin mannequin, String playerName,
                                                            Plugin plugin, Logger logger) {
        String normalizedName = playerName.toLowerCase(Locale.ROOT);
        if (playerName.length() < 3 || playerName.length() > 16) {
            logger.warning("Invalid mannequin player skin name '" + playerName
                    + "': names must contain 3 to 16 characters.");
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid Minecraft player skin name"));
        }

        CompletableFuture<ResolvableProfile> profileFuture = PLAYER_PROFILE_CACHE.computeIfAbsent(
                normalizedName, ignored -> resolvePlayerProfile(playerName));

        return profileFuture.thenAcceptAsync(profile -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!mannequin.isValid()) return;
            mannequin.setProfile(profile);
            refreshForNearbyPlayers(mannequin);
        })).whenComplete((ignored, exception) -> {
            if (exception == null) return;
            PLAYER_PROFILE_CACHE.remove(normalizedName, profileFuture);
            Throwable cause = unwrap(exception);
            logger.warning("Failed to resolve player skin for mannequin '" + playerName + "': " + message(cause));
        });
    }

    private static CompletableFuture<ResolvableProfile> resolvePlayerProfile(String playerName) {
        ResolvableProfile profile = ResolvableProfile.resolvableProfile().name(playerName).build();
        return profile.resolve().thenApplyAsync(ResolvableProfile::resolvableProfile);
    }

    private static void refreshForNearbyPlayers(Mannequin mannequin) {
        for (Entity entity : mannequin.getNearbyEntities(64.0D, 64.0D, 64.0D)) {
            if (entity instanceof Player player) {
                player.hideEntity(Bukkit.getPluginManager().getPlugin("PixelRPG"), mannequin);
                player.showEntity(Bukkit.getPluginManager().getPlugin("PixelRPG"), mannequin);
            }
        }
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
        String lower = source.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("www.")
                || lower.contains("/")
                || lower.contains("\")
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
