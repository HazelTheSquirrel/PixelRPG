package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles the compatibility guild information command. */
public final class GuildInfoCommand implements CommandExecutor {
    private final GuildManager guilds;

    public GuildInfoCommand(GuildManager guilds) {
        this.guilds = guilds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null) {
            player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            return true;
        }
        player.sendMessage(Component.text("Gilde: " + guild.name(), NamedTextColor.GOLD));
        player.sendMessage(Component.text("Mitglieder: " + guild.memberCount() + "/" + Guild.MAX_MEMBERS, NamedTextColor.AQUA));
        player.sendMessage(Component.text("Rolle: " + (guild.isLeader(player.getUniqueId()) ? "Gildenmeister" : "Mitglied"), NamedTextColor.WHITE));
        return true;
    }
}
