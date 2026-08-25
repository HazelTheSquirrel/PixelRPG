package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.guild.GuildManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles acceptance of a pending guild invitation. */
public final class GuildAcceptCommand implements CommandExecutor {
    private final GuildManager guilds;
    public GuildAcceptCommand(GuildManager guilds) { this.guilds = guilds; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        switch (guilds.acceptInvitation(player)) {
            case SUCCESS -> player.sendRichMessage("<green>Du bist der Gilde beigetreten.</green>");
            case NO_INVITATION -> player.sendRichMessage("<red>Du hast keine offene Gildeneinladung.</red>");
            case GUILD_FULL -> player.sendRichMessage("<red>Die Gilde ist inzwischen voll.</red>");
            case ALREADY_IN_GUILD -> player.sendRichMessage("<red>Du bist bereits in einer Gilde.</red>");
            default -> player.sendRichMessage("<red>Die Einladung konnte nicht angenommen werden.</red>");
        }
        return true;
    }
}
