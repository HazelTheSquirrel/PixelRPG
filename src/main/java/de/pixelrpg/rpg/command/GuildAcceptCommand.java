package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.guild.GuildManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles the compatibility guild invitation acceptance command. */
public final class GuildAcceptCommand implements CommandExecutor {
    private final GuildManager guilds;

    public GuildAcceptCommand(GuildManager guilds) {
        this.guilds = guilds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        switch (guilds.acceptInvitation(player)) {
            case SUCCESS -> player.sendMessage(Component.text("Du bist der Gilde beigetreten.", NamedTextColor.GREEN));
            case NO_INVITATION -> player.sendMessage(Component.text("Du hast keine offene Gildeneinladung.", NamedTextColor.RED));
            case GUILD_FULL -> player.sendMessage(Component.text("Die Gilde ist inzwischen voll.", NamedTextColor.RED));
            case ALREADY_IN_GUILD -> player.sendMessage(Component.text("Du bist bereits in einer Gilde.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Einladung konnte nicht angenommen werden.", NamedTextColor.RED));
        }
        return true;
    }
}
