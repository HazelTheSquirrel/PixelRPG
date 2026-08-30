package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** Provides the complete player-facing guild command tree below /pixelrpg guild. */
public final class GuildSubCommand implements SubCommand {
    private static final List<String> SUBCOMMANDS = List.of("create", "invite", "accept", "leave", "info", "disband");

    private final GuildManager guildManager;

    public GuildSubCommand(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    @Override
    public String name() {
        return "guild";
    }

    @Override
    public String permission() {
        return null;
    }

    @Override
    public String description() {
        return "Gilden verwalten";
    }

    @Override
    public String usage() {
        return "/pixelrpg guild <create|invite|accept|leave|info|disband>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können diesen Befehl nutzen.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(player, args);
            case "invite" -> invite(player, args);
            case "accept" -> accept(player);
            case "leave" -> leave(player);
            case "info" -> info(player);
            case "disband" -> disband(player);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }

    private boolean create(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Verwendung: /pixelrpg guild create <Name>", NamedTextColor.YELLOW));
            return true;
        }

        GuildManager.Result result = guildManager.createGuild(player, args[1]);
        switch (result) {
            case SUCCESS -> player.sendMessage(Component.text("Gilde erfolgreich erstellt.", NamedTextColor.GREEN));
            case NOT_REGISTERED -> player.sendMessage(Component.text("Du musst registriertes Rathausmitglied sein.", NamedTextColor.RED));
            case ALREADY_IN_GUILD -> player.sendMessage(Component.text("Du bist bereits in einer Gilde.", NamedTextColor.RED));
            case LEVEL_TOO_LOW -> player.sendMessage(Component.text("Dein Level ist für die Gildengründung zu niedrig.", NamedTextColor.RED));
            case INVALID_NAME -> player.sendMessage(Component.text("Der Gildenname ist ungültig.", NamedTextColor.RED));
            case NAME_TAKEN -> player.sendMessage(Component.text("Dieser Gildenname ist bereits vergeben.", NamedTextColor.RED));
            case INSUFFICIENT_GOLD -> player.sendMessage(Component.text("Du hast nicht genug Gold für die Gildengründung.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Gilde konnte nicht erstellt werden.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean invite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Verwendung: /pixelrpg guild invite <Spieler>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Dieser Spieler ist nicht online.", NamedTextColor.RED));
            return true;
        }

        switch (guildManager.invite(player, target)) {
            case SUCCESS -> { }
            case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            case NOT_LEADER -> player.sendMessage(Component.text("Nur der Gildenmeister darf Spieler einladen.", NamedTextColor.RED));
            case GUILD_FULL -> player.sendMessage(Component.text("Die Gilde hat bereits die maximale Mitgliederzahl erreicht.", NamedTextColor.RED));
            case TARGET_ALREADY_IN_GUILD -> player.sendMessage(Component.text("Dieser Spieler ist bereits in einer Gilde.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Einladung konnte nicht gesendet werden.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean accept(Player player) {
        switch (guildManager.acceptInvitation(player)) {
            case SUCCESS -> player.sendMessage(Component.text("Du bist der Gilde beigetreten.", NamedTextColor.GREEN));
            case NO_INVITATION -> player.sendMessage(Component.text("Du hast keine ausstehende Gildeneinladung.", NamedTextColor.RED));
            case GUILD_NOT_FOUND -> player.sendMessage(Component.text("Die Gilde existiert nicht mehr.", NamedTextColor.RED));
            case ALREADY_IN_GUILD -> player.sendMessage(Component.text("Du bist bereits in einer Gilde.", NamedTextColor.RED));
            case GUILD_FULL -> player.sendMessage(Component.text("Die Gilde ist bereits voll.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Einladung konnte nicht angenommen werden.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean leave(Player player) {
        switch (guildManager.leave(player)) {
            case SUCCESS -> player.sendMessage(Component.text("Du hast die Gilde verlassen.", NamedTextColor.YELLOW));
            case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            case LEADER_CANNOT_LEAVE -> player.sendMessage(Component.text("Als Gildenmeister kannst du die Gilde nicht einfach verlassen. Löse sie stattdessen auf.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Du konntest die Gilde nicht verlassen.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean info(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null) {
            player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Gilde: " + guild.name(), NamedTextColor.GOLD));
        player.sendMessage(Component.text("Mitglieder: " + guild.memberCount() + "/" + Guild.MAX_MEMBERS, NamedTextColor.GRAY));
        player.sendMessage(Component.text("Gildenmeister: " + Bukkit.getOfflinePlayer(guild.leaderId()).getName(), NamedTextColor.GRAY));
        return true;
    }

    private boolean disband(Player player) {
        switch (guildManager.disband(player)) {
            case SUCCESS -> player.sendMessage(Component.text("Die Gilde wurde aufgelöst.", NamedTextColor.YELLOW));
            case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            case NOT_LEADER -> player.sendMessage(Component.text("Nur der Gildenmeister kann die Gilde auflösen.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Gilde konnte nicht aufgelöst werden.", NamedTextColor.RED));
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("PixelRPG Gildenbefehle", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/pixelrpg guild create <Name>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild invite <Spieler>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild accept", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild leave", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild info", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild disband", NamedTextColor.YELLOW));
    }
}
