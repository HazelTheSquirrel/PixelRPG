package de.pixelrpg.rpg.npc;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves and caches player skins and external skin sources for mannequins. */
public final class ExternalSkinService {
    private static final String TEXTURES_HOST = "textures.minecraft.net";
    private static final String TEXTURE_PATH_PREFIX = "/texture/";
    private static final String MINESKIN_API = "https://api.mineskin.org/v2/generate";
    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_RESPONSE_BYTES = 1_500_000;
    private static final int MAX_PAGE_CANDIDATES = 8;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final Pattern TEXTURE_URL_PATTERN = Pattern.compile(
            "https://textures\\.minecraft\\.net/texture/[0-9a-fA-F]{64}/?", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
            "https?://[^\\\"'<>\\s]+\\.(?:png|jpe?g)(?:\\?[^\\\"'<>\\s]*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELATIVE_IMAGE_PATTERN = Pattern.compile(
            "(?:src|content|href)\\s*=\\s*[\\\"']([^\\\"']+\\.(?:png|jpe?g)(?:\\?[^\\\"']*)?)[\\\"']",
            Pattern.CASE_INSENSITIVE);

    private final Plugin plugin;
    private final Logger logger;
    private final HttpClient httpClient;
    private final ConcurrentMap<String, ProfileProperty> resolvedCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<ProfileProperty>> inFlight = new ConcurrentHashMap<>();

    public ExternalSkinService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CompletableFuture<Void> apply(Mannequin mannequin, String skinSource) {
        return applyAndGetProperty(mannequin, skinSource).thenApply(ignored -> null);
    }

    /**
     * Resolves and applies the external skin while returning the exact texture
     * property that was accepted. This lets persistence store the resolved
     * texture directly instead of re-reading mutable mannequin state later.
     */
    public CompletableFuture<ProfileProperty> applyAndGetProperty(Mannequin mannequin, String skinSource) {
        if (mannequin == null || !mannequin.isValid()) return CompletableFuture.failedFuture(
                new IllegalArgumentException("Mannequin is not valid"));
        String source = normalizeUrl(skinSource);
        if (source == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid skin URL"));
        return resolveProperty(source).thenCompose(property ->
                applyProperty(mannequin, property, source).thenApply(ignored -> property));
    }

    private CompletableFuture<ProfileProperty> resolveProperty(String source) {
        ProfileProperty cached = resolvedCache.get(source);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return inFlight.computeIfAbsent(source, key -> resolveUncached(key)
                .whenComplete((property, exception) -> inFlight.remove(key)));
    }

    private CompletableFuture<ProfileProperty> resolveUncached(String source) {
        if (isMinecraftTextureUrl(source)) {
            ProfileProperty property = unsignedTextureProperty(source);
            resolvedCache.put(source, property);
            return CompletableFuture.completedFuture(property);
        }

        URI uri = URI.create(source);
        if (!isSafeExternalUri(uri)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin URL points to a local or private address"));
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "PixelRPG/1.0 Minecraft-Skin-Resolver")
                .header("Accept", "text/html, image/png, image/jpeg, */*;q=0.8")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenCompose(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        return CompletableFuture.failedFuture(new IllegalStateException(
                                "Skin source returned HTTP " + response.statusCode()));
                    }
                    long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                    if (contentLength > MAX_RESPONSE_BYTES || response.body().length > MAX_RESPONSE_BYTES) {
                        return CompletableFuture.failedFuture(new IllegalStateException("Skin source response is too large"));
                    }

                    URI finalUri = response.uri();
                    if (!isSafeExternalUri(finalUri)) {
                        return CompletableFuture.failedFuture(new IllegalStateException(
                                "Skin source redirected to a local or private address"));
                    }

                    String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
                    if (contentType.startsWith("image/") || looksLikeImageUrl(finalUri.toString())) {
                        return generateSignedSkin(finalUri, response.body());
                    }

                    String html = new String(response.body(), StandardCharsets.UTF_8);
                    String textureUrl = findTextureUrl(html);
                    if (textureUrl != null) {
                        ProfileProperty property = unsignedTextureProperty(textureUrl);
                        resolvedCache.put(source, property);
                        return CompletableFuture.completedFuture(property);
                    }

                    Set<String> candidates = findImageUrls(html, finalUri);
                    return generateFromCandidates(candidates);
                });
    }

    private CompletableFuture<ProfileProperty> generateFromCandidates(Set<String> candidates) {
        if (candidates.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "No Minecraft texture or skin image was found on the supplied page"));
        }
        CompletableFuture<ProfileProperty> result = new CompletableFuture<>();
        generateCandidate(candidates.iterator(), result);
        return result;
    }

    private void generateCandidate(java.util.Iterator<String> iterator, CompletableFuture<ProfileProperty> result) {
        if (!iterator.hasNext()) {
            result.completeExceptionally(new IllegalArgumentException("No usable Minecraft skin image was found on the supplied page"));
            return;
        }

        URI candidate = URI.create(iterator.next());
        fetch(candidate).thenCompose(response -> {
            String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
            if (!contentType.startsWith("image/") || response.body().length == 0) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Candidate is not a usable image"));
            }
            return generateSignedSkin(response.uri(), response.body());
        }).whenComplete((property, exception) -> {
            if (exception == null && property != null) result.complete(property);
            else generateCandidate(iterator, result);
        });
    }

    private CompletableFuture<HttpResponse<byte[]>> fetch(URI uri) {
        if (!isSafeExternalUri(uri)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin image URL points to a local or private address"));
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "PixelRPG/1.0 Minecraft-Skin-Resolver")
                .header("Accept", "image/png,image/jpeg,*/*;q=0.8")
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("Skin image returned HTTP " + response.statusCode());
                    }
                    if (response.body().length > MAX_RESPONSE_BYTES) {
                        throw new IllegalStateException("Skin image is too large");
                    }
                    if (!isSafeExternalUri(response.uri())) {
                        throw new IllegalStateException("Skin image redirected to a local or private address");
                    }
                    return response;
                });
    }

    private CompletableFuture<ProfileProperty> generateSignedSkin(URI imageUri, byte[] imageBytes) {
        if (!isSafeExternalUri(imageUri)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin image URL points to a local or private address"));
        }
        if (imageBytes.length == 0 || imageBytes.length > MAX_RESPONSE_BYTES) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin image is empty or too large"));
        }

        String cacheKey = imageUri.toString();
        ProfileProperty cached = resolvedCache.get(cacheKey);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        String boundary = "----PixelRPG" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = multipartBody(boundary, imageBytes);
        HttpRequest request = HttpRequest.newBuilder(URI.create(MINESKIN_API))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "PixelRPG/1.0 Minecraft-Skin-Resolver")
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        String details = new String(response.body(), StandardCharsets.UTF_8);
                        throw new IllegalStateException("MineSkin returned HTTP " + response.statusCode()
                                + (details.isBlank() ? "" : ": " + details));
                    }
                    if (response.body().length > MAX_RESPONSE_BYTES) {
                        throw new IllegalStateException("MineSkin response is too large");
                    }
                    try {
                        JsonObject root = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
                        JsonObject data = root.getAsJsonObject("skin").getAsJsonObject("texture").getAsJsonObject("data");
                        String value = data.get("value").getAsString();
                        String signature = data.has("signature") && !data.get("signature").isJsonNull()
                                ? data.get("signature").getAsString() : null;
                        if (value.isBlank()) throw new IllegalStateException("MineSkin returned incomplete texture data");
                        ProfileProperty property = new ProfileProperty("textures", value, signature);
                        resolvedCache.putIfAbsent(cacheKey, property);
                        return property;
                    } catch (RuntimeException exception) {
                        throw new IllegalStateException("Invalid MineSkin response", exception);
                    }
                });
    }

    private static byte[] multipartBody(String boundary, byte[] imageBytes) {
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[prefix.length + imageBytes.length + suffix.length];
        System.arraycopy(prefix, 0, body, 0, prefix.length);
        System.arraycopy(imageBytes, 0, body, prefix.length, imageBytes.length);
        System.arraycopy(suffix, 0, body, prefix.length + imageBytes.length, suffix.length);
        return body;
    }

    private CompletableFuture<Void> applyProperty(Mannequin mannequin, ProfileProperty property, String source) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!mannequin.isValid()) { result.complete(null); return; }
                ResolvableProfile current = mannequin.getProfile();
                if (sameTextureProperty(current, property)) { result.complete(null); return; }

                ResolvableProfile.Builder builder = ResolvableProfile.resolvableProfile()
                        .name(current.name())
                        .uuid(current.uuid())
                        .addProperties(current.properties().stream()
                                .filter(existing -> !existing.getName().equals(property.getName()))
                                .toList())
                        .addProperty(property)
                        .skinPatch(current.skinPatch());
                mannequin.setProfile(builder.build());
                refreshForNearbyPlayers(mannequin);
                result.complete(null);
            } catch (RuntimeException exception) {
                logger.warning("Failed to apply mannequin skin '" + source + "': " + exception.getMessage());
                result.completeExceptionally(exception);
            }
        });
        return result;
    }

    private void refreshForNearbyPlayers(Mannequin mannequin) {
        for (Entity entity : mannequin.getNearbyEntities(64.0D, 64.0D, 64.0D)) {
            if (entity instanceof Player player) {
                player.hideEntity(plugin, mannequin);
                player.showEntity(plugin, mannequin);
            }
        }
    }

    private static boolean sameTextureProperty(ResolvableProfile profile, ProfileProperty property) {
        return profile.properties().stream()
                .filter(existing -> existing.getName().equals(property.getName()))
                .anyMatch(existing -> existing.getValue().equals(property.getValue())
                        && java.util.Objects.equals(existing.getSignature(), property.getSignature()));
    }

    private static String findTextureUrl(String content) {
        Matcher matcher = TEXTURE_URL_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : null;
    }

    private static Set<String> findImageUrls(String html, URI baseUri) {
        Set<String> candidates = new LinkedHashSet<>();
        Matcher matcher = IMAGE_URL_PATTERN.matcher(html);
        while (matcher.find() && candidates.size() < MAX_PAGE_CANDIDATES) addCandidate(candidates, URI.create(matcher.group()));
        Matcher relativeMatcher = RELATIVE_IMAGE_PATTERN.matcher(html);
        while (relativeMatcher.find() && candidates.size() < MAX_PAGE_CANDIDATES) {
            try { addCandidate(candidates, baseUri.resolve(relativeMatcher.group(1))); }
            catch (IllegalArgumentException ignored) { }
        }
        return candidates;
    }

    private static void addCandidate(Set<String> candidates, URI uri) {
        if (isSafeExternalUri(uri) && isLikelySkinImage(uri)) candidates.add(uri.toString());
    }

    private static boolean isLikelySkinImage(URI uri) {
        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null || path == null) return false;
        String lowerHost = host.toLowerCase(Locale.ROOT);
        String lowerPath = path.toLowerCase(Locale.ROOT);
        return lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")
                || lowerHost.contains("minecraftskins") || lowerHost.contains("namemc") || lowerHost.contains("novaskin");
    }

    private static boolean looksLikeImageUrl(String value) {
        String path = URI.create(value).getPath();
        if (path == null) return false;
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static boolean isMinecraftTextureUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && TEXTURES_HOST.equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null && uri.getPath().matches(TEXTURE_PATH_PREFIX + "[0-9a-fA-F]{64}/?");
        } catch (IllegalArgumentException exception) { return false; }
    }

    private static boolean isSafeExternalUri(URI uri) {
        if (uri == null || uri.getHost() == null) return false;
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) return false;
        if (uri.getUserInfo() != null || uri.getFragment() != null) return false;
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || isPrivateOrReserved(address)) return false;
            }
            return true;
        } catch (UnknownHostException exception) { return false; }
    }

    private static boolean isPrivateOrReserved(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int a = bytes[0] & 0xFF, b = bytes[1] & 0xFF;
            return a == 0 || a == 10 || (a == 100 && b >= 64 && b <= 127) || (a == 169 && b == 254)
                    || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 0) || (a == 192 && b == 168)
                    || (a == 198 && (b == 18 || b == 19)) || a >= 224;
        }
        return address.isSiteLocalAddress() || address.isLinkLocalAddress();
    }

    private static ProfileProperty unsignedTextureProperty(String textureUrl) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + escapeJson(textureUrl) + "\"}}}";
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return new ProfileProperty("textures", encoded);
    }

    private static String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > MAX_URL_LENGTH) return null;
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || uri.getHost() == null) return null;
            if (uri.getUserInfo() != null || uri.getFragment() != null) return null;
            return uri.toString();
        } catch (IllegalArgumentException exception) { return null; }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
