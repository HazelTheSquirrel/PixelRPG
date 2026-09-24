package de.pixelrpg.rpg.npc;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MannequinSkinResolver {
    private static final ConcurrentMap<String, CompletableFuture<ResolvableProfile>> CACHE = new ConcurrentHashMap<>();

    private MannequinSkinResolver() {}

    public static CompletableFuture<Void> applyStoredTexture(Mannequin mannequin, String value, String signature, Plugin plugin) {
        if (mannequin == null || !mannequin.isValid() || value == null || value.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        ProfileProperty property = new ProfileProperty("textures", value, signature);
        CompletableFuture<Void> result = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!mannequin.isValid()) { result.complete(null); return; }
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
                refresh(mannequin, plugin);
                result.complete(null);
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            }
        });
        return result;
    }

    public static CompletableFuture<ProfileProperty> applyPlayerName(Mannequin mannequin, String playerName, Plugin plugin) {
        if (mannequin == null || !mannequin.isValid()) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid mannequin"));
        String name = playerName == null ? "" : playerName.trim();
        if (name.length() < 3 || name.length() > 16 || !name.matches("[A-Za-z0-9_]+")) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid Minecraft player name"));
        }
        String key = name.toLowerCase(Locale.ROOT);
        PlayerProfile bukkitProfile = Bukkit.createProfile(name);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!mannequin.isValid()) return;
            mannequin.setProfile(ResolvableProfile.resolvableProfile(bukkitProfile));
            refresh(mannequin, plugin);
        });
        CompletableFuture<ResolvableProfile> future = CACHE.computeIfAbsent(key,
                ignored -> bukkitProfile.update().thenApply(ResolvableProfile::resolvableProfile));
        return future.thenApplyAsync(profile -> {
            if (!mannequin.isValid()) throw new IllegalStateException("Mannequin became invalid during skin resolution");
            mannequin.setProfile(profile);
            refresh(mannequin, plugin);
            return profile.properties().stream()
                    .filter(property -> "textures".equals(property.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Resolved profile contains no textures property"));
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable))
                .whenComplete((ignored, failure) -> {
                    if (failure != null) CACHE.remove(key, future);
                });
    }

    private static void refresh(Mannequin mannequin, Plugin plugin) {
        for (Entity entity : mannequin.getNearbyEntities(64.0D, 64.0D, 64.0D)) {
            if (entity instanceof Player player) {
                player.hideEntity(plugin, mannequin);
                player.showEntity(plugin, mannequin);
            }
        }
    }
}
