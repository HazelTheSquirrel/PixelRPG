package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/** Builds the player-specific PixelRPG character card opened from native quick actions. */
public final class QuickActionsDialogService {
    private final PlayerProfileManager profiles;

    public QuickActionsDialogService(PlayerProfileManager profiles) {
        this.profiles = profiles;
        Bukkit.getPluginManager().registerEvents(new QuickActionsDialogListener(this), PixelRPGPlugin.getInstance());
    }

    public boolean isAvailable(Player player) {
        return profiles.isRegistered(player.getUniqueId());
    }

    public Component characterCard(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("No PixelRPG profile for registered player"));

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
                : 20.0;
        double armor = player.getAttribute(Attribute.ARMOR) != null
                ? player.getAttribute(Attribute.ARMOR).getValue()
                : 0.0;

        return Component.text()
                .append(Component.text("Name: ")).append(player.displayName()).append(Component.newline())
                .append(Component.text("Level: ")).append(Component.text(profile.getLevel())).append(Component.newline())
                .append(Component.text("Klasse: ")).append(Component.text(profile.getPlayerClass().name())).append(Component.newline())
                .append(Component.text("Health: ")).append(Component.text(format(maxHealth))).append(Component.newline())
                .append(Component.text("Armor: ")).append(Component.text(format(armor)))
                .build();
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
