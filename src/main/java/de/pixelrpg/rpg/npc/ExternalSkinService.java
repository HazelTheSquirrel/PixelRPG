package de.pixelrpg.rpg.npc;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExternalSkinService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Map<String, ProfileProperty> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<ProfileProperty>> IN_FLIGHT = new ConcurrentHashMap<>();

    private final Plugin plugin;
    private final Logger logger;

    public ExternalSkinService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public CompletableFuture<Void> apply(Mannequin mannequin, String input) {
        String key = input == null ? "" : input.trim();
        if (key.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin input is empty"));
        }

        ProfileProperty cached = CACHE.get(key.toLowerCase(java.util.Locale.ROOT));
        if (cached != null) {
            return applyOnMainThread(mannequin, cached);
        }

        CompletableFuture<ProfileProperty> propertyFuture = IN_FLIGHT.computeIfAbsent(
                key.toLowerCase(java.util.Locale.ROOT), ignored -> resolve(key)
                        .whenComplete((property, throwable) -> IN_FLIGHT.remove(key.toLowerCase(java.util.Locale.ROOT)))
        );
        return propertyFuture.thenCompose(property -> applyOnMainThread(mannequin, property));
    }

    private CompletableFuture<ProfileProperty> resolve(String input) {
        String lower = input.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return resolveTextureUrl(input);
        }
        if (lower.startsWith("minecraftskins:")) {
            String skinId = input.substring("minecraftskins:".length()).trim();
            return resolveMinecraftSkins(skinId);
        }
        return CompletableFuture.completedFuture(unsignedTextureProperty(
                "https://mc-heads.net/skin/" + input
        ));
    }

    private CompletableFuture<ProfileProperty> resolveTextureUrl(String input) {
        URI uri = URI.create(input);
        if (!isSafeExternalHost(uri)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("External skin host is not allowed"));
        }
        return CompletableFuture.completedFuture(unsignedTextureProperty(input));
    }

    private CompletableFuture<ProfileProperty> resolveMinecraftSkins(String skinId) {
        if (skinId.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("MinecraftSkins id is empty"));
        }
        URI uri = URI.create("https://www.minecraftskins.com/skin/" + skinId + "/");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "PixelRPG/1.0")
                .GET()
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("MinecraftSkins returned HTTP " + response.statusCode());
                    }
                    Matcher matcher = Pattern.compile("https://www\\.minecraftskins\\.com/uploads/[^\\\"']+\\.png")
                            .matcher(response.body());
                    while (matcher.find()) {
                        String imageUrl = matcher.group();
                        if (!isSafeExternalHost(URI.create(imageUrl))) {
                            throw new IllegalStateException("MinecraftSkins returned an unexpected image host");
                        }
                        return unsignedTextureProperty(imageUrl);
                    }
                    throw new IllegalStateException("MinecraftSkins page did not expose the PNG for skin id " + skinId);
                });
    }

    private CompletableFuture<Void> applyOnMainThread(Mannequin mannequin, ProfileProperty property) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!mannequin.isValid()) {
                    result.complete(null);
                    return;
                }
                mannequin.setProfile(ResolvableProfile.resolvableProfile().addProperty(property).build());
                for (Entity entity : mannequin.getNearbyEntities(64.0D)) {
                    if (!(entity instanceof Player player)) continue;
                    player.hideEntity(plugin, mannequin);
                    player.showEntity(plugin, mannequin);
                }
                result.complete(null);
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            }
        });
        return result;
    }

    private static ProfileProperty unsignedTextureProperty(String textureUrl) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + escapeJson(textureUrl) + "\"}}}";
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return new ProfileProperty("textures", encoded);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean isSafeExternalHost(URI uri) {
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) return false;
        String host = uri.getHost();
        if (host == null || host.isBlank()) return false;
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    return false;
                }
            }
        } catch (Exception exception) {
            return false;
        }
        return true;
    }
}
