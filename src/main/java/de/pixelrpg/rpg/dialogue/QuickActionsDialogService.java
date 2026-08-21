package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
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
                .filter(PlayerProfile::isRegisteredInGuild)
                .orElseThrow(() -> new IllegalStateException("No registered PixelRPG profile for player"));
        StatEngine.CachedStats stats = statEngine.getCachedStats(player.getUniqueId());

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
                : stats.maxHealth();
        double armor = player.getAttribute(Attribute.ARMOR) != null
                ? player.getAttribute(Attribute.ARMOR).getValue()
                : stats.armor();

        return Component.text()
                .append(Component.text("Name: ")).append(player.displayName()).append(Component.newline())
                .append(Component.text("Level: ")).append(Component.text(profile.getLevel())).append(Component.newline())
                .append(Component.text("Klasse: ")).append(Component.text(profile.getPlayerClass().name())).append(Component.newline())
                .append(Component.text("Health: ")).append(Component.text(format(player.getHealth()) + "/" + format(maxHealth))).append(Component.newline())
                .append(Component.text("Mana: ")).append(Component.text(format(profile.getCurrentMana()) + "/" + format(stats.maxMana()))).append(Component.newline())
                .append(Component.text("Armor: ")).append(Component.text(format(armor))).append(Component.newline())
                .append(Component.text("Strength: ")).append(Component.text(format(stats.strength()))).append(Component.newline())
                .append(Component.text("Agility: ")).append(Component.text(format(stats.agility()))).append(Component.newline())
                .append(Component.text("Stamina: ")).append(Component.text(format(stats.stamina()))).append(Component.newline())
                .append(Component.text("Intellect: ")).append(Component.text(format(stats.intellect()))).append(Component.newline())
                .append(Component.text("Attack Power: ")).append(Component.text(format(stats.attackPower()))).append(Component.newline())
                .append(Component.text("Spell Power: ")).append(Component.text(format(stats.spellPower()))).append(Component.newline())
                .append(Component.text("Critical Chance: ")).append(Component.text(format(stats.critChance()) + "%"))
                .build();
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
