package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.guild.GuildManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/** Handles the player-facing guild invitation command. */
public final class GuildInviteCommand implements CommandExecutor, TabCompleter {
    private final GuildManager guilds;
    public GuildInviteCommand(GuildManager guilds) { this.guilds = guilds; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length != 1) { player.sendRichMessage("<yellow>Nutze: /gildeneinladen <Spieler></yellow>"); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { player.sendRichMessage("<red>Dieser Spieler ist nicht online.</red>"); return true; }
        switch (guilds.invite(player, target)) {
            case SUCCESS -> { }
            case NOT_IN_GUILD -> player.sendRichMessage("<red>Du bist in keiner Gilde.</red>");
            case NOT_LEADER -> player.sendRichMessage("<red>Nur der Gildenmeister darf Spieler einladen.</red>");
            case GUILD_FULL -> player.sendRichMessage("<red>Die Gilde hat bereits 50 Mitglieder.</red>");
            case TARGET_ALREADY_IN_GUILD -> player.sendRichMessage("<red>Dieser Spieler ist bereits in einer Gilde.</red>");
            default -> player.sendRichMessage("<red>Die Einladung konnte nicht gesendet werden.</red>");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(prefix)).sorted().toList();
    }
}
