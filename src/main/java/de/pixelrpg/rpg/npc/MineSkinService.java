package de.pixelrpg.rpg.npc;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Generates Mojang-compatible signed texture properties for external skin images through MineSkin V2. */
public final class MineSkinService {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_RESPONSE_BYTES = 512 * 1024;

    private final Plugin plugin;
    private final HttpClient httpClient;
    private final String apiBase;
    private final String apiKey;
    private final String userAgent;
    private final String visibility;
    private final String variant;

    public MineSkinService(Plugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.apiBase = normalizeApiBase(plugin.getConfig().getString("npc.skin.mineskin.api-base", "https://api.mineskin.org"));
        this.apiKey = trimToNull(plugin.getConfig().getString("npc.skin.mineskin.api-key", ""));
        this.userAgent = trimToNull(plugin.getConfig().getString("npc.skin.mineskin.user-agent", "PixelRPG/1.0"));
        this.visibility = normalizeVisibility(plugin.getConfig().getString("npc.skin.mineskin.visibility", "unlisted"));
        this.variant = normalizeVariant(plugin.getConfig().getString("npc.skin.mineskin.variant", "auto"));
    }

    public CompletableFuture<ProfileProperty> generate(String imageUrl) {
        JsonObject payload = new JsonObject();
        payload.addProperty("url", imageUrl);
        payload.addProperty("visibility", visibility);
        if (variant != null) payload.addProperty("variant", variant);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(apiBase + "/v2/generate"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8));

        if (apiKey != null) requestBuilder.header("Authorization", "Bearer " + apiKey);

        return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.body().length > MAX_RESPONSE_BYTES) {
                        throw new IllegalStateException("MineSkin response exceeded the configured size limit");
                    }
                    if (response.statusCode() / 100 != 2) {
                        String body = new String(response.body(), StandardCharsets.UTF_8);
                        throw new IllegalStateException("MineSkin returned HTTP " + response.statusCode()
                                + (body.isBlank() ? "" : ": " + summarize(body)));
                    }
                    return parseTextureProperty(new String(response.body(), StandardCharsets.UTF_8));
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger().fine("MineSkin URL generation failed: " + rootMessage(throwable));
                    }
                });
    }

    private static ProfileProperty parseTextureProperty(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        JsonObject data = objectAt(root, "skin", "texture", "data");
        if (data == null) data = objectAt(root, "data", "texture");
        if (data == null) throw new IllegalStateException("MineSkin response contains no texture data");

        String value = stringAt(data, "value");
        String signature = stringAt(data, "signature");
        if (value == null || value.isBlank()) throw new IllegalStateException("MineSkin response contains no texture value");
        if (signature == null || signature.isBlank()) throw new IllegalStateException("MineSkin response contains no texture signature");

        return new ProfileProperty("textures", value, signature);
    }

    private static JsonObject objectAt(JsonObject root, String... path) {
        JsonElement current = root;
        for (String part : path) {
            if (current == null || !current.isJsonObject()) return null;
            current = current.getAsJsonObject().get(part);
        }
        return current != null && current.isJsonObject() ? current.getAsJsonObject() : null;
    }

    private static String stringAt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static String normalizeApiBase(String value) {
        String normalized = value == null || value.isBlank() ? "https://api.mineskin.org" : value.trim();
        if (!normalized.startsWith("https://")) throw new IllegalArgumentException("MineSkin API base must use HTTPS");
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private static String normalizeVisibility(String value) {
        String normalized = value == null ? "unlisted" : value.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "public", "unlisted" -> normalized;
            default -> "unlisted";
        };
    }

    private static String normalizeVariant(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("auto")) return null;
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("classic") || normalized.equals("slim") ? normalized : null;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String summarize(String value) {
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 240 ? compact : compact.substring(0, 240) + "...";
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
