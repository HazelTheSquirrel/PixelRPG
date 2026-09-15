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

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves external mannequin skin sources into Minecraft texture profile properties. */
public final class ExternalSkinService {
    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_RESPONSE_BYTES = 1_500_000;
    private static final int MAX_IMAGE_CANDIDATES = 8;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final String MINE_SKIN_GENERATE_URL = "https://api.mineskin.org/v2/generate";
    private static final Pattern TEXTURE_URL_PATTERN = Pattern.compile(
            "https?://textures\\.minecraft\\.net/texture/[a-fA-F0-9]{64}");
    private static final Pattern IMAGE_ATTRIBUTE_PATTERN = Pattern.compile(
            "(?:src|href|data-src|data-image|content)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_URL_PATTERN = Pattern.compile(
            "url\\(\\s*[\\\"']?([^\\\"')]+)[\\\"']?\\s*\\)",
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
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CompletableFuture<Void> apply(Mannequin mannequin, String source) {
        ProfileProperty cached = resolvedCache.get(source);
        if (cached != null) {
            return applyProperty(mannequin, cached, source);
        }

        CompletableFuture<ProfileProperty> future = inFlight.computeIfAbsent(source,
                key -> resolveUncached(key).whenComplete((property, throwable) -> {
                    inFlight.remove(key);
                    if (throwable == null && property != null) {
                        resolvedCache.put(key, property);
                    }
                }));

        return future.thenCompose(property -> applyProperty(mannequin, property, source));
    }

    private CompletableFuture<ProfileProperty> resolveUncached(String source) {
        try {
            URI uri = URI.create(source);
            if (!isSafeExternalUri(uri)) {
                return CompletableFuture.failedFuture(new IOException("External skin URL is not allowed."));
            }

            if (isMojangTextureUrl(uri)) {
                return CompletableFuture.completedFuture(createUnsignedTextureProperty(uri.toString()));
            }

            return fetch(uri).thenCompose(response -> resolveResponse(uri, response));
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private CompletableFuture<HttpResponse<byte[]>> fetch(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "PixelRPG/1.0")
                .header("Accept", "text/html,image/*,*/*;q=0.8")
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.body().length > MAX_RESPONSE_BYTES) {
                        throw new CompletionException(new IOException("External skin response is too large."));
                    }
                    URI finalUri = response.uri();
                    if (!isSafeExternalUri(finalUri)) {
                        throw new CompletionException(new IOException("External skin redirect target is not allowed."));
                    }
                    return response;
                });
    }

    private CompletableFuture<ProfileProperty> resolveResponse(URI sourceUri, HttpResponse<byte[]> response) {
        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        byte[] body = response.body();

        if (contentType.startsWith("image/")) {
            return generateSignedSkin(sourceUri, body);
        }

        String content = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        String directTextureUrl = findTextureUrl(content);
        if (directTextureUrl != null) {
            return CompletableFuture.completedFuture(createUnsignedTextureProperty(directTextureUrl));
        }

        Set<String> candidates = findImageUrls(content, sourceUri);
        if (candidates.isEmpty()) {
            return CompletableFuture.failedFuture(new IOException("No usable Minecraft skin texture or image URL found."));
        }

        CompletableFuture<ProfileProperty> result = new CompletableFuture<>();
        resolveImageCandidates(new ArrayList<>(candidates), 0, result);
        return result;
    }

    private void resolveImageCandidates(ArrayList<String> candidates, int index,
                                        CompletableFuture<ProfileProperty> result) {
        if (index >= candidates.size()) {
            result.completeExceptionally(new IOException("No usable skin image could be generated."));
            return;
        }

        String candidate = candidates.get(index);
        try {
            URI uri = URI.create(candidate);
            if (!isSafeExternalUri(uri)) {
                resolveImageCandidates(candidates, index + 1, result);
                return;
            }
            fetch(uri).thenCompose(response -> {
                String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
                if (!contentType.startsWith("image/")) {
                    return CompletableFuture.failedFuture(new IOException("Candidate is not an image."));
                }
                return generateSignedSkin(uri, response.body());
            }).whenComplete((property, throwable) -> {
                if (throwable == null && property != null) {
                    result.complete(property);
                } else {
                    resolveImageCandidates(candidates, index + 1, result);
                }
            });
        } catch (RuntimeException exception) {
            resolveImageCandidates(candidates, index + 1, result);
        }
    }

    private CompletableFuture<ProfileProperty> generateSignedSkin(URI imageUri, byte[] imageBytes) {
        if (imageBytes.length == 0 || imageBytes.length > MAX_RESPONSE_BYTES) {
            return CompletableFuture.failedFuture(new IOException("Skin image is empty or too large."));
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("url", imageUri.toString());

        HttpRequest request = HttpRequest.newBuilder(URI.create(MINE_SKIN_GENERATE_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "PixelRPG/1.0")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new CompletionException(new IOException("MineSkin returned HTTP " + response.statusCode()));
                    }
                    try {
                        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                        JsonObject texture = root.getAsJsonObject("skin").getAsJsonObject("texture");
                        JsonObject data = texture.getAsJsonObject("data");
                        String value = data.get("value").getAsString();
                        String signature = data.has("signature") && !data.get("signature").isJsonNull()
                                ? data.get("signature").getAsString()
                                : null;
                        if (value.isBlank()) {
                            throw new IOException("MineSkin returned an empty texture value.");
                        }
                        return new ProfileProperty("textures", value, signature);
                    } catch (RuntimeException | IOException exception) {
                        throw new CompletionException(exception);
                    }
                });
    }

    private CompletableFuture<Void> applyProperty(Mannequin mannequin, ProfileProperty property, String source) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!mannequin.isValid()) {
                    result.complete(null);
                    return;
                }

                ResolvableProfile current = mannequin.getProfile();
                if (sameTextureProperty(current, property)) {
                    result.complete(null);
                    return;
                }

                ResolvableProfile.Builder builder = ResolvableProfile.resolvableProfile()
                        .name(current.name())
                        .uuid(current.uuid())
                        .addProperties(current.properties())
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
        addImageMatches(candidates, html, baseUri, IMAGE_ATTRIBUTE_PATTERN);
        addImageMatches(candidates, html, baseUri, CSS_URL_PATTERN);
        return candidates.stream().limit(MAX_IMAGE_CANDIDATES).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static void addImageMatches(Set<String> candidates, String content, URI baseUri, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find() && candidates.size() < MAX_IMAGE_CANDIDATES) {
            String raw = matcher.group(1).trim();
            try {
                URI resolved = baseUri.resolve(raw);
                if ("http".equalsIgnoreCase(resolved.getScheme()) || "https".equalsIgnoreCase(resolved.getScheme())) {
                    candidates.add(resolved.toString());
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed external image references.
            }
        }
    }

    private static boolean isMojangTextureUrl(URI uri) {
        return "textures.minecraft.net".equalsIgnoreCase(uri.getHost())
                && uri.getPath() != null
                && uri.getPath().startsWith("/texture/");
    }

    private static ProfileProperty createUnsignedTextureProperty(String textureUrl) {
        String payload = "{\"timestamp\":0,\"profileId\":\"00000000000000000000000000000000\",\"profileName\":\"\",\"textures\":{\"SKIN\":{\"url\":\""
                + textureUrl + "\"}}}";
        String encoded = Base64.getEncoder().encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new ProfileProperty("textures", encoded);
    }

    private static boolean isSafeExternalUri(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || host == null || host.isBlank()) {
            return false;
        }
        if (uri.toString().length() > MAX_URL_LENGTH) {
            return false;
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    return false;
                }
            }
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
