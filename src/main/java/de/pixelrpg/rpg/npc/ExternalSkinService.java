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

/** Resolves external skin URLs into Minecraft texture properties without requiring a third-party signing API. */
public final class ExternalSkinService {
    private static final String TEXTURES_HOST = "textures.minecraft.net";
    private static final String MINECRAFT_SKINS_HOST = "minecraftskins.com";
    private static final String MINECRAFT_SKINS_WWW_HOST = "www.minecraftskins.com";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_CACHE_ENTRIES = 512;
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final long CACHE_TTL_MILLIS = Duration.ofHours(12).toMillis();
    private static final Pattern MINECRAFT_SKINS_IMAGE = Pattern.compile(
            "https://(?:www\\.)?minecraftskins\\.com/uploads/skins/[^\\\"'\\s\\]<>]+?\\.png(?:\\?[^\\\"'\\s\\]<>]+)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MINECRAFT_SKINS_PAGE = Pattern.compile(
            "/skin/([0-9]+)(?:/[^/]*)?/?",
            Pattern.CASE_INSENSITIVE);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final Map<String, CacheEntry> CACHE = new LinkedHashMap<>(64, 0.75f, true);
    private static final Map<String, CompletableFuture<ProfileProperty>> IN_FLIGHT = new ConcurrentHashMap<>();

    private final Plugin plugin;
    private final Logger logger;

    public ExternalSkinService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public CompletableFuture<Void> apply(Mannequin mannequin, String skinUrl) {
        if (mannequin == null || !mannequin.isValid()) return CompletableFuture.completedFuture(null);
        String normalized = normalizeUrl(skinUrl);
        if (normalized == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid skin URL"));

        return resolveProperty(normalized)
                .thenCompose(property -> applyOnMainThread(mannequin, property));
    }

    private CompletableFuture<ProfileProperty> resolveProperty(String normalized) {
        ProfileProperty cached = getCached(normalized);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return IN_FLIGHT.computeIfAbsent(normalized, key -> fetchProperty(key)
                .whenComplete((ignored, throwable) -> IN_FLIGHT.remove(key)));
    }

    private CompletableFuture<ProfileProperty> fetchProperty(String normalized) {
        if (isMinecraftTextureUrl(normalized)) {
            ProfileProperty property = unsignedTextureProperty(normalized);
            putCached(normalized, property);
            return CompletableFuture.completedFuture(property);
        }

        if (isMinecraftSkinsPage(normalized)) {
            return resolveMinecraftSkinsImageUrl(normalized).thenApply(imageUrl -> {
                ProfileProperty property = unsignedTextureProperty(imageUrl);
                putCached(normalized, property);
                return property;
            });
        }

        if (!isSafeExternalHost(normalized)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Skin URL resolves to a private or reserved network address"));
        }

        ProfileProperty property = unsignedTextureProperty(normalized);
        putCached(normalized, property);
        logger.fine("Using unsigned external mannequin skin texture: " + normalized);
        return CompletableFuture.completedFuture(property);
    }

    private CompletableFuture<String> resolveMinecraftSkinsImageUrl(String pageUrl) {
        URI pageUri = URI.create(pageUrl);
        Matcher pageMatcher = MINECRAFT_SKINS_PAGE.matcher(pageUri.getPath());
        if (!pageMatcher.matches()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid MinecraftSkins page URL"));
        }
        String skinId = pageMatcher.group(1);

        HttpRequest request = HttpRequest.newBuilder(pageUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("User-Agent", "PixelRPG/1.0 (Minecraft NPC skin resolver)")
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("MinecraftSkins returned HTTP " + response.statusCode());
                    }
                    byte[] body = response.body();
                    if (body.length > MAX_RESPONSE_BYTES) {
                        throw new IllegalStateException("MinecraftSkins page exceeded the configured size limit");
                    }
                    String html = new String(body, StandardCharsets.UTF_8);
                    Matcher matcher = MINECRAFT_SKINS_IMAGE.matcher(html);
                    while (matcher.find()) {
                        String imageUrl = matcher.group();
                        if (imageUrl.matches("(?i).*[-]" + Pattern.quote(skinId) + "\\.png(?:\\?.*)?$")) {
                            if (!isSafeMinecraftSkinsImage(imageUrl)) {
                                throw new IllegalStateException("MinecraftSkins returned an unexpected image host");
                            }
                            return imageUrl;
                        }
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
                for (Entity entity : mannequin.getNearbyEntities(64.0D, 64.0D, 64.0D)) {
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

    private static boolean isMinecraftTextureUrl(String url) {
        URI uri = URI.create(url);
        return "https".equalsIgnoreCase(uri.getScheme())
                && TEXTURES_HOST.equalsIgnoreCase(uri.getHost())
                && uri.getPath() != null
                && uri.getPath().startsWith("/texture/");
    }

    private static boolean isMinecraftSkinsPage(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        return (MINECRAFT_SKINS_HOST.equalsIgnoreCase(host) || MINECRAFT_SKINS_WWW_HOST.equalsIgnoreCase(host))
                && uri.getPath() != null
                && MINECRAFT_SKINS_PAGE.matcher(uri.getPath()).matches();
    }

    private static boolean isSafeMinecraftSkinsImage(String value) {
        URI uri = URI.create(value);
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme())
                && (MINECRAFT_SKINS_HOST.equalsIgnoreCase(host) || MINECRAFT_SKINS_WWW_HOST.equalsIgnoreCase(host));
    }

    private static String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > 2048) return null;
        if (!value.startsWith("http://") && !value.startsWith("https://")) value = "https://" + value;
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || uri.getHost() == null) return null;
        if (uri.getUserInfo() != null || uri.getFragment() != null) return null;
        if (!"https".equalsIgnoreCase(scheme)) return null;
        return uri.toString();
    }

    private static boolean isSafeExternalHost(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return false;
            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static synchronized ProfileProperty getCached(String key) {
        CacheEntry entry = CACHE.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.createdAtMillis() > CACHE_TTL_MILLIS) {
            CACHE.remove(key);
            return null;
        }
        return entry.property();
    }

    private static synchronized void putCached(String key, ProfileProperty property) {
        CACHE.put(key, new CacheEntry(property, System.currentTimeMillis()));
        while (CACHE.size() > MAX_CACHE_ENTRIES) CACHE.remove(CACHE.keySet().iterator().next());
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record CacheEntry(ProfileProperty property, long createdAtMillis) { }
}
