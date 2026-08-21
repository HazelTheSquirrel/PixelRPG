package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/** Builds the player-specific PixelRPG character card opened from the G action. */
public final class QuickActionsDialogService {
    private final PlayerProfileManager profiles;
    private final StatEngine statEngine;

    public QuickActionsDialogService(PlayerProfileManager profiles, StatEngine statEngine) {
        this.profiles = profiles;
        this.statEngine = statEngine;
    }

    public PlayerProfileManager profileManager() {
        return profiles;
    }

    public StatEngine statEngine() {
        return statEngine;
    }

    public boolean isAvailable(Player player) {
        return profiles.isRegistered(player.getUniqueId());
    }

    public Component characterCard(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId())
                .filter(PlayerProfile::isRegisteredInGuild)
                .orElseThrow(() -> new IllegalStateException("No registered PixelRPG profile for player"));
        StatEngine.CachedStats stats = statEngine.getCachedStats(player.getUniqueId());

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
                : stats.maxHealth();
        double armor = player.getAttribute(Attribute.ARMOR) != null
                ? player.getAttribute(Attribute.ARMOR).getValue()
                : stats.armor();

        Component section = Component.text("────────────────────────", NamedTextColor.DARK_GRAY);
        Component header = Component.text("CHARAKTER", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        Component identity = Component.text()
                .append(Component.text("Name: ", NamedTextColor.GRAY)).append(player.displayName()).append(Component.newline())
                .append(Component.text("Level: ", NamedTextColor.GRAY)).append(Component.text(profile.getLevel(), NamedTextColor.YELLOW)).append(Component.newline())
                .append(Component.text("Klasse: ", NamedTextColor.GRAY)).append(profile.getPlayerClass().displayName())
                .build();
        Component resources = Component.text()
                .append(Component.text("Leben: ", NamedTextColor.GRAY)).append(Component.text(format(player.getHealth()) + "/" + format(maxHealth), NamedTextColor.RED)).append(Component.newline())
                .append(Component.text("Mana: ", NamedTextColor.GRAY)).append(Component.text(format(profile.getCurrentMana()) + "/" + format(stats.maxMana()), NamedTextColor.BLUE)).append(Component.newline())
                .append(Component.text("Rüstung: ", NamedTextColor.GRAY)).append(Component.text(format(armor), NamedTextColor.AQUA))
                .build();
        Component attributes = Component.text()
                .append(Component.text("Stärke: ", NamedTextColor.GRAY)).append(Component.text(format(stats.strength()), NamedTextColor.WHITE)).append(Component.newline())
                .append(Component.text("Beweglichkeit: ", NamedTextColor.GRAY)).append(Component.text(format(stats.agility()), NamedTextColor.WHITE)).append(Component.newline())
                .append(Component.text("Ausdauer: ", NamedTextColor.GRAY)).append(Component.text(format(stats.stamina()), NamedTextColor.WHITE)).append(Component.newline())
                .append(Component.text("Intelligenz: ", NamedTextColor.GRAY)).append(Component.text(format(stats.intellect()), NamedTextColor.WHITE))
                .build();
        Component combat = Component.text()
                .append(Component.text("Angriffskraft: ", NamedTextColor.GRAY)).append(Component.text(format(stats.attackPower()), NamedTextColor.WHITE)).append(Component.newline())
                .append(Component.text("Zauberkraft: ", NamedTextColor.GRAY)).append(Component.text(format(stats.spellPower()), NamedTextColor.WHITE)).append(Component.newline())
                .append(Component.text("Kritische Trefferchance: ", NamedTextColor.GRAY)).append(Component.text(format(stats.critChance()) + "%", NamedTextColor.WHITE))
                .build();

        return Component.text()
                .append(header).append(Component.newline()).append(section).append(Component.newline())
                .append(identity).append(Component.newline()).append(Component.newline())
                .append(Component.text("RESSOURCEN", NamedTextColor.AQUA).decorate(TextDecoration.BOLD)).append(Component.newline())
                .append(resources).append(Component.newline()).append(Component.newline())
                .append(Component.text("ATTRIBUTE", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD)).append(Component.newline())
                .append(attributes).append(Component.newline()).append(Component.newline())
                .append(Component.text("KAMPF", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)).append(Component.newline())
                .append(combat)
                .build();
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
