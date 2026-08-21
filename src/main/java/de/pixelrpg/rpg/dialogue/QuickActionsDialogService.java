package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;

/** Provides the PixelRPG character-card data for the native quick-actions dialog. */
public final class QuickActionsDialogService {
    private final PlayerProfileManager profiles;

    public QuickActionsDialogService(PlayerProfileManager profiles) {
        this.profiles = profiles;
    }

    public boolean isAvailable(Player player) {
        return profiles.getProfile(player.getUniqueId())
                .map(profile -> profile.isRegistered())
                .orElse(false);
    }

    public String characterCard(Player player) {
        var profile = profiles.getProfile(player.getUniqueId()).orElseThrow();
        return "PixelRPG\nLevel " + profile.getLevel();
    }
}
