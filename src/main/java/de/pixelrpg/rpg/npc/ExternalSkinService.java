package de.pixelrpg.rpg.npc;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves external skin URLs into signed Minecraft texture properties. */
public final class ExternalSkinService {
    private static final URI MINESKIN_GENERATE_URI = URI.create("https://api.mineskin.org/v2/generate");
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

        String apiKey = configuredApiKey();
        if (apiKey.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "External mannequin skins require a MineSkin API key. Configure npc.skin.mineskin.api-key or PIXELRPG_MINESKIN_API_KEY."));
        }

        CompletableFuture<String> imageUrlFuture = isMinecraftSkinsPage(normalized)
                ? resolveMinecraftSkinsImageUrl(normalized)
                : CompletableFuture.completedFuture(normalized);

        return imageUrlFuture.thenCompose(imageUrl -> {
            if (!isSafeExternalHost(imageUrl)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException(
                        "Skin URL resolves to a private or reserved network address"));
            }

            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("url", imageUrl);

            HttpRequest request = HttpRequest.newBuilder(MINESKIN_GENERATE_URI)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString(), StandardCharsets.UTF_8))
                    .build();

            return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new IllegalStateException("MineSkin returned HTTP " + response.statusCode());
                        }
                        byte[] body = response.body();
                        if (body.length > MAX_RESPONSE_BYTES) {
                            throw new IllegalStateException("MineSkin response exceeded the configured size limit");
                        }
                        return parseTextureProperty(new String(body, StandardCharsets.UTF_8));
                    })
                    .thenApply(property -> {
                        putCached(normalized, property);
                        return property;
                    });
        }).exceptionallyCompose(exception -> {
            Throwable cause = unwrap(exception);
            logger.warning("Failed to resolve external mannequin skin '" + normalized + "': " + message(cause));
            return CompletableFuture.failedFuture(cause);
        });
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
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (player.getWorld().equals(mannequin.getWorld())
                            && player.getLocation().distanceSquared(mannequin.getLocation()) <= 4096.0D) {
                        player.hideEntity(plugin, mannequin);
                        player.showEntity(plugin, mannequin);
                    }
                });
                result.complete(null);
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            }
        });
        return result;
    }

    private String configuredApiKey() {
        String configured = plugin.getConfig().getString("npc.skin.mineskin.api-key", "");
        if (configured != null && !configured.isBlank()) return configured.trim();
        String environment = System.getenv("PIXELRPG_MINESKIN_API_KEY");
        return environment == null ? "" : environment.trim();
    }

    private static ProfileProperty parseTextureProperty(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject skin = root.getAsJsonObject("skin");
        if (skin == null) throw new IllegalStateException("MineSkin response did not contain skin data");
        JsonObject texture = skin.getAsJsonObject("texture");
        if (texture == null) throw new IllegalStateException("MineSkin response did not contain texture data");
        JsonObject data = texture.getAsJsonObject("data");
        if (data == null) throw new IllegalStateException("MineSkin response did not contain texture payload");
        String value = data.has("value") ? data.get("value").getAsString() : "";
        String signature = data.has("signature") ? data.get("signature").getAsString() : "";
        if (value.isBlank() || signature.isBlank()) throw new IllegalStateException("MineSkin returned incomplete texture data");
        return new ProfileProperty("textures", value, signature);
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
        return uri.toString();
    }

    private static boolean isSafeExternalHost(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
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

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException) && current.getCause() != null) current = current.getCause();
        return current;
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record CacheEntry(ProfileProperty property, long createdAtMillis) { }
}
