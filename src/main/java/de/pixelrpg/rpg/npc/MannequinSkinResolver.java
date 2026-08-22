// src/main/java/de/pixelrpg/rpg/npc/MannequinSkinResolver.java
// RISIKO-KAPSELUNG: ResolvableProfile ist Teil der neueren Paper-Datacomponent-API.
// Falls sich die exakte Builder-Signatur in eurer Dev-Bundle-Version unterscheidet,
// muss ausschließlich diese Datei angepasst werden.
package de.pixelrpg.rpg.npc;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MannequinSkinResolver {

    private MannequinSkinResolver() {
    }

    public static void apply(Mannequin mannequin, String skinSource, Logger logger) {
        if (skinSource == null || skinSource.isBlank()) {
            return;
        }

        try {
            if (skinSource.startsWith("http://") || skinSource.startsWith("https://")) {
                applySkinUrl(mannequin, skinSource);
            } else {
                applyPlayerName(mannequin, skinSource);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to apply mannequin skin '" + skinSource + "'", e);
        }
    }

    private static void applyPlayerName(Mannequin mannequin, String playerName) {
        PlayerProfile profile = Bukkit.createProfile(playerName);
        mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
    }

    private static void applySkinUrl(Mannequin mannequin, String skinUrl) throws Exception {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "MannequinSkin");
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(URI.create(skinUrl).toURL());
        profile.setTextures(textures);
        mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
    }
}