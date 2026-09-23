package de.pixelrpg.rpg.npc;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.net.URI;
import com.destroystokyo.paper.profile.PlayerProfile;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Resolves Minecraft player names and direct skin image URLs for mannequins.
 *
 * Player names are resolved through Mojang/Paper profile resolution.
 * Direct URLs are assigned through PlayerTextures#setSkin, exactly as the
 * original 26.2 mannequin implementation did. No MineSkin or other skin API
 * is required.
 */
public final class MannequinSkinResolver {
    private static final ConcurrentMap<String, CompletableFuture<ResolvableProfile>> PLAYER_PROFILE_CACHE =
            new ConcurrentHashMap<>();

    private MannequinSkinResolver() {
    }

    public static CompletableFuture<Void> applyStoredTexture(Mannequin mannequin, String value,
                                                              String signature, Plugin plugin) {
        if (mannequin == null || !mannequin.isValid() || value == null || value.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        com.destroystokyo.paper.profile.ProfileProperty property =
                new com.destroystokyo.paper.profile.ProfileProperty("textures", value, signature);
        CompletableFuture<Void> result = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!mannequin.isValid()) {
                    result.complete(null);
                    return;
                }

                ResolvableProfile current = mannequin.getProfile();
                ResolvableProfile.Builder builder = ResolvableProfile.resolvableProfile()
                        .name(current.name())
                        .uuid(current.uuid())
                        .addProperties(current.properties().stream()
                                .filter(existing -> !"textures".equals(existing.getName()))
                                .toList())
                        .addProperty(property)
                        .skinPatch(current.skinPatch());
                mannequin.setProfile(builder.build());
                refreshForNearbyPlayers(mannequin, plugin);
                result.complete(null);
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            }
        });

        return result;
    }

    public static CompletableFuture<Void> apply(Mannequin mannequin, String skinSource, Logger logger) {
        if (mannequin == null || skinSource == null || skinSource.isBlank() || !mannequin.isValid()) {
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
                return applySkinUrl(mannequin, source, plugin, logger);
            }

            if (looksLikeUrl(source)) {
                logger.warning("Invalid mannequin skin URL '" + source
                        + "'. Expected http(s)://host/path or a Minecraft player name.");
                return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid mannequin skin URL"));
            }

            return applyPlayerName(mannequin, source, plugin, logger);
        } catch (RuntimeException exception) {
            logger.warning("Failed to start mannequin skin resolution for '" + source + "': "
                    + message(exception));
            return CompletableFuture.failedFuture(exception);
        }
    }

    /**
     * Applies a direct skin image URL without MineSkin, an API key, or any
     * external skin-generation service.
     */
    private static CompletableFuture<Void> applySkinUrl(Mannequin mannequin, String skinUrl,
                                                         Plugin plugin, Logger logger) {
        return new ExternalSkinService(plugin).apply(mannequin, skinUrl)
                .whenComplete((ignored, exception) -> {
                    if (exception == null) return;
                    Throwable cause = unwrap(exception);
                    logger.warning("Failed to resolve external mannequin skin '" + skinUrl + "': "
                            + message(cause));
                });
    }

    private static CompletableFuture<Void> applyPlayerName(Mannequin mannequin, String playerName,
                                                            Plugin plugin, Logger logger) {
        String normalizedName = playerName.toLowerCase(Locale.ROOT);
        if (playerName.length() < 3 || playerName.length() > 16
                || !playerName.matches("[A-Za-z0-9_]+")) {
            logger.warning("Invalid mannequin player skin name '" + playerName
                    + "': names must contain 3 to 16 Minecraft username characters.");
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Invalid Minecraft player skin name"));
        }

        PlayerProfile bukkitProfile = Bukkit.createProfile(playerName);

        // Set the name-backed profile immediately. This keeps the mannequin usable
        // even if the Mojang session server is temporarily slow.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!mannequin.isValid()) return;
            mannequin.setProfile(ResolvableProfile.resolvableProfile(bukkitProfile));
            refreshForNearbyPlayers(mannequin, plugin);
        });

        CompletableFuture<ResolvableProfile> profileFuture = PLAYER_PROFILE_CACHE.computeIfAbsent(
                normalizedName, ignored -> resolvePlayerProfile(bukkitProfile));

        return profileFuture.thenAcceptAsync(profile -> {
            if (!mannequin.isValid()) return;
            mannequin.setProfile(profile);
            refreshForNearbyPlayers(mannequin, plugin);
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable)).whenComplete((ignored, exception) -> {
            if (exception == null) return;
            PLAYER_PROFILE_CACHE.remove(normalizedName, profileFuture);
            Throwable cause = unwrap(exception);
            logger.warning("Failed to resolve player skin for mannequin '" + playerName + "': "
                    + message(cause));
        });
    }

    private static CompletableFuture<ResolvableProfile> resolvePlayerProfile(PlayerProfile profile) {
        return profile.update().thenApplyAsync(ResolvableProfile::resolvableProfile);
    }

    private static void refreshForNearbyPlayers(Mannequin mannequin, Plugin plugin) {
        for (Entity entity : mannequin.getNearbyEntities(64.0D, 64.0D, 64.0D)) {
            if (entity instanceof Player player) {
                player.hideEntity(plugin, mannequin);
                player.showEntity(plugin, mannequin);
            }
        }
    }

    private static boolean isUrl(String source) {
        try {
            URI uri = URI.create(source);
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
                || lower.contains("\\")
                || lower.matches("^[a-z0-9.-]+:[0-9]+(?:/.*)?$");
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }
}
