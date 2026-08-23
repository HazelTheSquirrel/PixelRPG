package de.pixelrpg.rpg.npc;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Resolves player-backed skins and applies the profile to a mannequin. */
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
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to start mannequin skin resolution for '" + skinSource + "'", exception);
        }
    }

    private static void applyPlayerName(Mannequin mannequin, String playerName, Plugin plugin, Logger logger) {
        PlayerProfile profile = Bukkit.createProfile(playerName);

        // Give the client a dynamic name-backed profile immediately; Paper can resolve the texture client-side.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (mannequin.isValid()) mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
        });

        // Also resolve the profile server-side so the mannequin receives the completed UUID/name/textures when available.
        profile.update().thenAcceptAsync(updatedProfile -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!mannequin.isValid()) return;
            mannequin.setProfile(ResolvableProfile.resolvableProfile(updatedProfile));
        })).exceptionally(exception -> {
            logger.log(Level.WARNING, "Failed to resolve player skin for mannequin '" + playerName + "'", exception);
            return null;
        });
    }

    private static void applySkinUrl(Mannequin mannequin, String skinUrl, Plugin plugin) throws Exception {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "MannequinSkin");
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(URI.create(skinUrl).toURL());
        profile.setTextures(textures);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (mannequin.isValid()) mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
        });
    }
}
