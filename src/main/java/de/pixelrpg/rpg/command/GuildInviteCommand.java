package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.guild.GuildManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** Handles the compatibility guild invitation command while pointing players to the canonical command tree. */
public final class GuildInviteCommand implements CommandExecutor, TabCompleter {
    private final GuildManager guilds;

    public GuildInviteCommand(GuildManager guilds) {
        this.guilds = guilds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length != 1) {
            player.sendMessage(Component.text("Nutze: /pixelrpg guild invite <Spieler>", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Dieser Spieler ist nicht online.", NamedTextColor.RED));
            return true;
        }
        switch (guilds.invite(player, target)) {
            case SUCCESS -> { }
            case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            case NOT_LEADER -> player.sendMessage(Component.text("Nur der Gildenmeister darf Spieler einladen.", NamedTextColor.RED));
            case GUILD_FULL -> player.sendMessage(Component.text("Die Gilde hat bereits 50 Mitglieder.", NamedTextColor.RED));
            case TARGET_ALREADY_IN_GUILD -> player.sendMessage(Component.text("Dieser Spieler ist bereits in einer Gilde.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Einladung konnte nicht gesendet werden.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
