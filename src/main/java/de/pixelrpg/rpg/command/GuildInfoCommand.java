package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Displays the current player's guild information. */
public final class GuildInfoCommand implements CommandExecutor {
    private final GuildManager guilds;
    public GuildInfoCommand(GuildManager guilds) { this.guilds = guilds; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null) { player.sendRichMessage("<red>Du bist in keiner Gilde.</red>"); return true; }
        player.sendRichMessage("<gold>Gilde: " + guild.name() + "</gold> <gray>|</gray> <aqua>Mitglieder: " + guild.memberCount() + "/50</aqua> <gray>|</gray> <white>Rolle: " + (guild.isLeader(player.getUniqueId()) ? "Gildenmeister" : "Mitglied") + "</white>");
        return true;
    }
}
