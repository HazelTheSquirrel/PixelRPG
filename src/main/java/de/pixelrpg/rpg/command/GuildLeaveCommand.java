package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.guild.GuildManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles the compatibility guild leave command. */
public final class GuildLeaveCommand implements CommandExecutor {
    private final GuildManager guilds;

    public GuildLeaveCommand(GuildManager guilds) {
        this.guilds = guilds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        switch (guilds.leave(player)) {
            case SUCCESS -> player.sendMessage(Component.text("Du hast die Gilde verlassen.", NamedTextColor.YELLOW));
            case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            case LEADER_CANNOT_LEAVE -> player.sendMessage(Component.text("Der Gildenmeister kann die Gilde nicht verlassen.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Du konntest die Gilde nicht verlassen.", NamedTextColor.RED));
        }
        return true;
    }
}
