package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;

/** Provides PixelRPG character-card data for the native quick-actions dialog. */
public final class QuickActionsDialogService {
    private final PlayerProfileManager profiles;

    public QuickActionsDialogService(PlayerProfileManager profiles) {
        this.profiles = profiles;
    }

    public boolean isAvailable(Player player) {
        return profiles.isRegistered(player.getUniqueId());
    }

    public String characterCard(Player player) {
        var profile = profiles.getProfile(player.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("No PixelRPG profile for registered player"));
        return "PixelRPG\nLevel " + profile.getLevel();
    }
}
