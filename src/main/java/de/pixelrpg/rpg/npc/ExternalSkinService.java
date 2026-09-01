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
import java.util.logging.Logger;

/** Resolves external image URLs into Minecraft texture properties. */
public final class ExternalSkinService {
    private static final URI MINESKIN_GENERATE_URI = URI.create("https://api.mineskin.org/v2/generate");
    private static final String TEXTURES_HOST = "textures.minecraft.net";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_CACHE_ENTRIES = 512;
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final long CACHE_TTL_MILLIS = Duration.ofHours(12).toMillis();

    private final Plugin plugin;
    private final Logger logger;
    private final HttpClient httpClient;
    private final Map<String, CacheEntry> cache = new LinkedHashMap<>(64, 0.75f, true);

    public ExternalSkinService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public CompletableFuture<Void> apply(Mannequin mannequin, String skinUrl) {
        String normalized = normalizeUrl(skinUrl);
        if (normalized == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid skin URL"));

        ProfileProperty cached = getCached(normalized);
        if (cached != null) return applyOnMainThread(mannequin, cached);

        if (isMinecraftTextureUrl(normalized)) {
            ProfileProperty property = unsignedTextureProperty(normalized);
            putCached(normalized, property);
            return applyOnMainThread(mannequin, property);
        }

        String apiKey = configuredApiKey();
        if (apiKey.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "External mannequin skins require an NPC MineSkin API key. Configure npc.skin.mineskin.api-key or PIXELRPG_MINESKIN_API_KEY."));
        }

        if (!isSafeExternalHost(normalized)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin URL resolves to a private or reserved network address"));
        }

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("url", normalized);

        HttpRequest request = HttpRequest.newBuilder(MINESKIN_GENERATE_URI)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString(), StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
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
                })
                .thenCompose(property -> applyOnMainThread(mannequin, property))
                .exceptionallyCompose(exception -> {
                    logger.warning("Failed to resolve external mannequin skin '" + normalized + "': " + rootMessage(exception));
                    return CompletableFuture.failedFuture(exception);
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

    private static String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > 2048) return null;
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

    private synchronized ProfileProperty getCached(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.createdAtMillis() > CACHE_TTL_MILLIS) {
            cache.remove(key);
            return null;
        }
        return entry.property();
    }

    private synchronized void putCached(String key, ProfileProperty property) {
        cache.put(key, new CacheEntry(property, System.currentTimeMillis()));
        while (cache.size() > MAX_CACHE_ENTRIES) cache.remove(cache.keySet().iterator().next());
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record CacheEntry(ProfileProperty property, long createdAtMillis) { }
}
