package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.gui.PartyGUI;
import de.pixelrpg.rpg.dialogue.InviteDialogService;
import de.pixelrpg.rpg.party.Party;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class PartySubCommand implements SubCommand, CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("invite", "accept", "leave", "kick", "transfer", "disband", "info");

    private final PartyManager partyManager;
    private final PlayerProfileManager profileManager;
    private final InviteDialogService inviteDialogService;

    public PartySubCommand(PartyManager partyManager, PlayerProfileManager profileManager) {
        this(partyManager, profileManager, null);
    }

    public PartySubCommand(PartyManager partyManager, PlayerProfileManager profileManager, InviteDialogService inviteDialogService) {
        this.partyManager = partyManager;
        this.profileManager = profileManager;
        this.inviteDialogService = inviteDialogService;
    }

    @Override public String name() { return "party"; }
    @Override public String permission() { return null; }
    @Override public String usage() { return "/pixelrpg party <invite|accept|leave|kick|transfer|disband|info>"; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return execute(sender, args); }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können diesen Befehl nutzen.", NamedTextColor.RED));
            return true;
        }
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst registriertes Rathausmitglied sein.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            new PartyGUI(player, partyManager, profileManager, inviteDialogService).open(player);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "leave" -> handleLeave(player);
            case "kick" -> handleKick(player, args);
            case "transfer" -> handleTransfer(player, args);
            case "disband" -> handleDisband(player);
            case "info" -> { new PartyGUI(player, partyManager, profileManager, inviteDialogService).open(player); yield true; }
            default -> { player.sendMessage(Component.text("Verwendung: /pixelrpg party <invite|accept|leave|kick|transfer|disband|info>", NamedTextColor.YELLOW)); yield true; }
        };
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(Component.text("Verwendung: /pixelrpg party invite <Spieler>", NamedTextColor.YELLOW)); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { player.sendMessage(Component.text("Spieler ist nicht online.", NamedTextColor.RED)); return true; }
        if (target.getUniqueId().equals(player.getUniqueId())) { player.sendMessage(Component.text("Du kannst dich nicht selbst einladen.", NamedTextColor.RED)); return true; }
        if (!profileManager.isRegistered(target.getUniqueId())) { player.sendMessage(Component.text(target.getName() + " ist kein registriertes Rathausmitglied.", NamedTextColor.RED)); return true; }
        Party party = partyManager.getParty(player.getUniqueId()).orElseGet(() -> partyManager.createParty(player.getUniqueId()));
        if (!party.isLeader(player.getUniqueId())) { player.sendMessage(Component.text("Nur der Gruppenanführer kann einladen.", NamedTextColor.RED)); return true; }
        if (party.isFull()) { player.sendMessage(Component.text("Deine Gruppe ist voll.", NamedTextColor.RED)); return true; }
        if (!partyManager.addInvite(target.getUniqueId(), player.getUniqueId())) { player.sendMessage(Component.text("Die Einladung konnte nicht gesendet werden.", NamedTextColor.RED)); return true; }
        player.sendMessage(Component.text("Einladung an " + target.getName() + " gesendet.", NamedTextColor.GREEN));
        target.sendMessage(Component.text(player.getName() + " hat dich in eine Gruppe eingeladen! Nutze /pixelrpg party accept", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAccept(Player player) {
        boolean success = partyManager.acceptInvite(player);
        player.sendMessage(Component.text(success ? "Du bist der Gruppe beigetreten!" : "Keine ausstehende Einladung oder die Gruppe ist voll.", success ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    private boolean handleLeave(Player player) {
        if (partyManager.getParty(player.getUniqueId()).isEmpty()) { player.sendMessage(Component.text("Du bist in keiner Gruppe.", NamedTextColor.RED)); return true; }
        partyManager.leaveParty(player);
        player.sendMessage(Component.text("Du hast die Gruppe verlassen.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(Component.text("Verwendung: /pixelrpg party kick <Spieler>", NamedTextColor.YELLOW)); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !partyManager.kick(player, target.getUniqueId())) { player.sendMessage(Component.text("Spieler konnte nicht aus der Gruppe entfernt werden.", NamedTextColor.RED)); return true; }
        player.sendMessage(Component.text(target.getName() + " wurde aus der Gruppe entfernt.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleTransfer(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(Component.text("Verwendung: /pixelrpg party transfer <Spieler>", NamedTextColor.YELLOW)); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !partyManager.transferLeadership(player, target.getUniqueId())) { player.sendMessage(Component.text("Die Gruppenleitung konnte nicht übertragen werden.", NamedTextColor.RED)); return true; }
        player.sendMessage(Component.text(target.getName() + " ist jetzt der Gruppenanführer.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleDisband(Player player) {
        Optional<Party> party = partyManager.getParty(player.getUniqueId());
        if (party.isEmpty()) { player.sendMessage(Component.text("Du bist in keiner Gruppe.", NamedTextColor.RED)); return true; }
        if (!party.get().isLeader(player.getUniqueId())) { player.sendMessage(Component.text("Nur der Anführer kann die Gruppe auflösen.", NamedTextColor.RED)); return true; }
        partyManager.disbandParty(party.get());
        return true;
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return resolveTabComplete(args); }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { return resolveTabComplete(args); }

    private List<String> resolveTabComplete(String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && isPlayerArgument(args[0])) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return List.of();
    }

    private boolean isPlayerArgument(String subcommand) {
        return subcommand.equalsIgnoreCase("invite")
                || subcommand.equalsIgnoreCase("kick")
                || subcommand.equalsIgnoreCase("transfer");
    }
}
