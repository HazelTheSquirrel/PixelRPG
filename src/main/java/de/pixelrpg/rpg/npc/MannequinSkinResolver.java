package de.pixelrpg.rpg.npc;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Resolves player-backed skins and applies the completed profile to a mannequin. */
public final class MannequinSkinResolver {
    private MannequinSkinResolver() {
    }

    public static void apply(Mannequin mannequin, String skinSource, Logger logger) {
        if (skinSource == null || skinSource.isBlank() || !mannequin.isValid()) return;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PixelRPG");
        if (plugin == null) {
            logger.warning("Cannot resolve mannequin skin '" + skinSource + "': PixelRPG plugin is not loaded.");
            return;
        }
        try {
            if (skinSource.startsWith("http://") || skinSource.startsWith("https://")) {
                applySkinUrl(mannequin, skinSource, plugin);
            } else {
                applyPlayerName(mannequin, skinSource, plugin, logger);
            }
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Failed to start mannequin skin resolution for '" + skinSource + "'", exception);
        }
    }

    private static void applyPlayerName(Mannequin mannequin, String playerName, Plugin plugin, Logger logger) {
        // Resolve by name. Using an OfflinePlayer UUID here can produce an offline UUID and therefore no Mojang skin.
        PlayerProfile profile = Bukkit.createProfile(playerName);
        mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));

        CompletableFuture<PlayerProfile> update = profile.update();
        update.thenAcceptAsync(updatedProfile -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!mannequin.isValid()) return;
            mannequin.setProfile(ResolvableProfile.resolvableProfile(updatedProfile));
        })).exceptionally(exception -> {
            logger.log(Level.WARNING, "Failed to resolve player skin for mannequin '" + playerName + "'", exception);
            return null;
        });
    }

    private static void applySkinUrl(Mannequin mannequin, String skinUrl, Plugin plugin) throws RuntimeException {
        try {
            PlayerProfile profile = Bukkit.createProfile("MannequinSkin");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(skinUrl).toURL());
            profile.setTextures(textures);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (mannequin.isValid()) mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
            });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to configure skin URL", exception);
        }
    }
}
