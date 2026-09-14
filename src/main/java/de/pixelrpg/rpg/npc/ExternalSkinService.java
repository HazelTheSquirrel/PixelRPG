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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves skin pages and image URLs into Minecraft-compatible mannequin texture properties. */
public final class ExternalSkinService {
    private static final String TEXTURES_HOST = "textures.minecraft.net";
    private static final String MINECRAFT_SKINS_HOST = "minecraftskins.com";
    private static final String MINECRAFT_SKINS_WWW_HOST = "www.minecraftskins.com";
    private static final String NAMEMC_HOST = "namemc.com";
    private static final String NAMEMC_WWW_HOST = "www.namemc.com";
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
    private static final Pattern NAMEMC_SKIN_PAGE = Pattern.compile(
            "/skin/([0-9a-f]{16})/?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMEMC_TEXTURE_PAGE = Pattern.compile(
            "/texture/([0-9a-f]{16})\\.png/?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE = Pattern.compile(
            "<meta[^>]+(?:property|name)=[\\\"'](?:og:image|twitter:image)[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_REVERSED = Pattern.compile(
            "<meta[^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]+(?:property|name)=[\\\"'](?:og:image|twitter:image)[\\\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_ATTRIBUTE = Pattern.compile(
            "<(?:img|source)[^>]+(?:src|data-src|data-original)=[\\\"']([^\\\"']+)[\\\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final Map<String, CacheEntry> CACHE = new LinkedHashMap<>(64, 0.75f, true);
    private static final Map<String, CompletableFuture<ProfileProperty>> IN_FLIGHT = new ConcurrentHashMap<>();

    private final Plugin plugin;
    private final Logger logger;
    private final MineSkinService mineSkinService;

    public ExternalSkinService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.mineSkinService = new MineSkinService(plugin);
    }

    public CompletableFuture<Void> apply(Mannequin mannequin, String skinSource) {
        if (mannequin == null || !mannequin.isValid()) return CompletableFuture.completedFuture(null);
        String normalized = normalizeUrl(skinSource);
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

        if (isNameMcSkinPage(normalized)) {
            String hash = NAMEMC_SKIN_PAGE.matcher(URI.create(normalized).getPath()).group(1);
            return generateSignedProperty("https://namemc.com/texture/" + hash + ".png");
        }

        if (isNameMcTexturePage(normalized)) {
            return generateSignedProperty(normalized);
        }

        if (isMinecraftSkinsPage(normalized)) {
            return resolveMinecraftSkinsImageUrl(normalized).thenCompose(this::generateSignedProperty);
        }

        if (!isSafeExternalHost(normalized)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Skin URL resolves to a private or reserved network address"));
        }

        if (looksLikeImageUrl(normalized)) {
            return generateSignedProperty(normalized);
        }

        return resolveImageFromPage(normalized).thenCompose(this::generateSignedProperty);
    }

    private CompletableFuture<ProfileProperty> generateSignedProperty(String imageUrl) {
        if (!isSafeExternalHost(imageUrl) && !isMinecraftTextureUrl(imageUrl)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin image URL is not publicly reachable"));
        }

        return mineSkinService.generate(imageUrl)
                .thenApply(property -> {
                    putCached(imageUrl, property);
                    return property;
                });
    }

    private CompletableFuture<String> resolveMinecraftSkinsImageUrl(String pageUrl) {
        URI pageUri = URI.create(pageUrl);
        Matcher pageMatcher = MINECRAFT_SKINS_PAGE.matcher(pageUri.getPath());
        if (!pageMatcher.matches()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid MinecraftSkins skin page URL"));
        }
        String skinId = pageMatcher.group(1);

        return fetchHtml(pageUri).thenApply(html -> {
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

    private CompletableFuture<String> resolveImageFromPage(String pageUrl) {
        URI pageUri = URI.create(pageUrl);
        return fetchHtml(pageUri).thenApply(html -> {
            String imageUrl = firstMatchingImage(html, pageUri, OG_IMAGE);
            if (imageUrl == null) imageUrl = firstMatchingImage(html, pageUri, OG_IMAGE_REVERSED);
            if (imageUrl == null) imageUrl = firstMatchingImage(html, pageUri, IMAGE_ATTRIBUTE);
            if (imageUrl == null) {
                throw new IllegalStateException("Skin page did not expose a usable PNG/JPEG image URL");
            }
            return imageUrl;
        });
    }

    private CompletableFuture<String> fetchHtml(URI uri) {
        if (!isSafeExternalHost(uri.toString())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin page resolves to a private or reserved network address"));
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("User-Agent", "PixelRPG/1.0 (Minecraft NPC skin resolver)")
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("Skin page returned HTTP " + response.statusCode());
                    }
                    byte[] body = response.body();
                    if (body.length > MAX_RESPONSE_BYTES) {
                        throw new IllegalStateException("Skin page exceeded the configured size limit");
                    }
                    return new String(body, StandardCharsets.UTF_8);
                });
    }

    private static String firstMatchingImage(String html, URI pageUri, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            try {
                URI resolved = pageUri.resolve(candidate);
                String value = resolved.toString();
                if (isSafeExternalHost(value) && looksLikeImageUrl(value)) return value;
            } catch (IllegalArgumentException ignored) {
                // Try the next image candidate exposed by the page.
            }
        }
        return null;
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
                    if (entity instanceof Player player) {
                        player.hideEntity(plugin, mannequin);
                        player.showEntity(plugin, mannequin);
                    }
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
                && uri.getPath().matches("/texture/[0-9a-fA-F]{64}/?");
    }

    private static boolean isNameMcSkinPage(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        return (NAMEMC_HOST.equalsIgnoreCase(host) || NAMEMC_WWW_HOST.equalsIgnoreCase(host))
                && uri.getPath() != null
                && NAMEMC_SKIN_PAGE.matcher(uri.getPath()).matches();
    }

    private static boolean isNameMcTexturePage(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        return (NAMEMC_HOST.equalsIgnoreCase(host) || NAMEMC_WWW_HOST.equalsIgnoreCase(host))
                && uri.getPath() != null
                && NAMEMC_TEXTURE_PAGE.matcher(uri.getPath()).matches();
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

    private static boolean looksLikeImageUrl(String value) {
        try {
            String path = URI.create(value).getPath();
            return path != null && path.matches("(?i).*\\.(png|jpe?g)$");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > 2048) return null;
        if (!value.startsWith("http://") && !value.startsWith("https://")) value = "https://" + value;
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return null;
            if (uri.getUserInfo() != null || uri.getFragment() != null) return null;
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
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
