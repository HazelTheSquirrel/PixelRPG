package de.pixelrpg.rpg.party;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Player-facing party command using the current Paper command API. */
public final class PartyCommand implements BasicCommand {
    private static final List<String> SUBCOMMANDS = List.of("invite", "accept", "leave", "kick", "transfer", "disband", "info");
    private final PartyManager parties;
    private final PlayerProfileManager profiles;

    public PartyCommand(PartyManager parties, PlayerProfileManager profiles) {
        this.parties = parties;
        this.profiles = profiles;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text("Nur Spieler können diesen Befehl nutzen.", NamedTextColor.RED));
            return;
        }
        if (!profiles.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst registriertes Rathausmitglied sein.", NamedTextColor.RED));
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sendInfo(player);
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "invite" -> invite(player, args);
            case "accept" -> accept(player);
            case "leave" -> leave(player);
            case "kick" -> kick(player, args);
            case "transfer" -> transfer(player, args);
            case "disband" -> disband(player);
            default -> player.sendMessage(Component.text("Verwendung: /pixelrpgparty <invite|accept|leave|kick|transfer|disband|info>", NamedTextColor.YELLOW));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && SetOfPlayerCommands.contains(args[0].toLowerCase(Locale.ROOT))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
        }
        return List.of();
    }

    @Override public String permission() { return "rpg.member"; }

    private void invite(Player player, String[] args) {
        if (args.length != 2) { player.sendMessage(Component.text("Verwendung: /pixelrpgparty invite <Spieler>", NamedTextColor.YELLOW)); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { player.sendMessage(Component.text("Spieler ist nicht online.", NamedTextColor.RED)); return; }
        if (target.getUniqueId().equals(player.getUniqueId())) { player.sendMessage(Component.text("Du kannst dich nicht selbst einladen.", NamedTextColor.RED)); return; }
        if (!profiles.isRegistered(target.getUniqueId())) { player.sendMessage(Component.text("Dieser Spieler ist nicht registriert.", NamedTextColor.RED)); return; }
        Party party = parties.getParty(player.getUniqueId()).orElseGet(() -> parties.createParty(player.getUniqueId()));
        if (!party.isLeader(player.getUniqueId()) || party.isFull()) { player.sendMessage(Component.text("Du kannst aktuell niemanden einladen.", NamedTextColor.RED)); return; }
        if (!parties.addInvite(target.getUniqueId(), player.getUniqueId())) { player.sendMessage(Component.text("Die Einladung konnte nicht gesendet werden.", NamedTextColor.RED)); return; }
        player.sendMessage(Component.text("Einladung an " + target.getName() + " gesendet.", NamedTextColor.GREEN));
        target.sendMessage(Component.text(player.getName() + " hat dich in eine Gruppe eingeladen. Nutze /pixelrpgparty accept.", NamedTextColor.GREEN));
    }

    private void accept(Player player) {
        boolean success = parties.acceptInvite(player);
        player.sendMessage(Component.text(success ? "Du bist der Gruppe beigetreten." : "Keine gültige Gruppeneinladung.", success ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void leave(Player player) {
        if (parties.getParty(player.getUniqueId()).isEmpty()) {
            player.sendMessage(Component.text("Du bist in keiner Gruppe.", NamedTextColor.RED));
            return;
        }
        parties.leaveParty(player);
        player.sendMessage(Component.text("Du hast die Gruppe verlassen.", NamedTextColor.YELLOW));
    }

    private void kick(Player player, String[] args) {
        if (args.length != 2) { player.sendMessage(Component.text("Verwendung: /pixelrpgparty kick <Spieler>", NamedTextColor.YELLOW)); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !parties.kick(player, target.getUniqueId())) {
            player.sendMessage(Component.text("Spieler konnte nicht aus der Gruppe entfernt werden.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text(target.getName() + " wurde entfernt.", NamedTextColor.GREEN));
    }

    private void transfer(Player player, String[] args) {
        if (args.length != 2) { player.sendMessage(Component.text("Verwendung: /pixelrpgparty transfer <Spieler>", NamedTextColor.YELLOW)); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !parties.transferLeadership(player, target.getUniqueId())) {
            player.sendMessage(Component.text("Die Gruppenleitung konnte nicht übertragen werden.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text(target.getName() + " ist jetzt Gruppenanführer.", NamedTextColor.GREEN));
    }

    private void disband(Player player) {
        Party party = parties.getParty(player.getUniqueId()).orElse(null);
        if (party == null) { player.sendMessage(Component.text("Du bist in keiner Gruppe.", NamedTextColor.RED)); return; }
        if (!party.isLeader(player.getUniqueId())) { player.sendMessage(Component.text("Nur der Anführer kann die Gruppe auflösen.", NamedTextColor.RED)); return; }
        parties.disbandParty(party);
    }

    private void sendInfo(Player player) {
        Party party = parties.getParty(player.getUniqueId()).orElse(null);
        if (party == null) {
            player.sendMessage(Component.text("Du bist in keiner Gruppe.", NamedTextColor.RED));
            return;
        }
        String members = party.getMembers().stream()
                .map(uuid -> {
                    Player online = Bukkit.getPlayer(uuid);
                    return online != null ? online.getName() : uuid.toString();
                })
                .reduce((a, b) -> a + ", " + b).orElse("");
        player.sendMessage(Component.text("Gruppe: " + members + " | Anführer: " + Bukkit.getOfflinePlayer(party.getLeader()).getName(), NamedTextColor.GOLD));
    }

    private static final class SetOfPlayerCommands {
        private static boolean contains(String value) {
            return value.equals("invite") || value.equals("kick") || value.equals("transfer");
        }
    }
}
