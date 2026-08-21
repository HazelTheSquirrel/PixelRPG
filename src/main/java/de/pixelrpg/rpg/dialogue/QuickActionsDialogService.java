package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Builds the player-specific PixelRPG character card opened from native quick actions. */
public final class QuickActionsDialogService {
    private final PlayerProfileManager profiles;
    private final StatEngine statEngine;

    public QuickActionsDialogService(PlayerProfileManager profiles, StatEngine statEngine) {
        this.profiles = profiles;
        this.statEngine = statEngine;
    }

    public boolean isAvailable(Player player) {
        return profiles.isRegistered(player.getUniqueId());
    }

    public Component characterCard(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("No PixelRPG profile for registered player"));
        StatEngine.CachedStats stats = statEngine.getCachedStats(player.getUniqueId());

        return Component.text()
                .append(Component.text("Name: ")).append(player.displayName()).append(Component.newline())
                .append(Component.text("Level: ")).append(Component.text(profile.getLevel())).append(Component.newline())
                .append(Component.text("Klasse: ")).append(Component.text(profile.getPlayerClass().name())).append(Component.newline())
                .append(Component.text("Health: ")).append(Component.text(format(stats.maxHealth()))).append(Component.newline())
                .append(Component.text("Armor: ")).append(Component.text(format(stats.armor())))
                .build();
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
