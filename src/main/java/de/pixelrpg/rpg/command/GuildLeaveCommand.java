package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.guild.GuildManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles leaving a guild while protecting the guild leader from orphaning the guild. */
public final class GuildLeaveCommand implements CommandExecutor {
    private final GuildManager guilds;
    public GuildLeaveCommand(GuildManager guilds) { this.guilds = guilds; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        switch (guilds.leave(player)) {
            case SUCCESS -> player.sendRichMessage("<yellow>Du hast die Gilde verlassen.</yellow>");
            case NOT_IN_GUILD -> player.sendRichMessage("<red>Du bist in keiner Gilde.</red>");
            case LEADER_CANNOT_LEAVE -> player.sendRichMessage("<red>Der Gildenmeister kann die Gilde nicht verlassen.</red>");
            default -> player.sendRichMessage("<red>Du konntest die Gilde nicht verlassen.</red>");
        }
        return true;
    }
}
