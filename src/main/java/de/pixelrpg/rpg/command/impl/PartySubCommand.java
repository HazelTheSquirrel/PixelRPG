// src/main/java/de/pixelrpg/rpg/command/impl/PartySubCommand.java
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.gui.PartyGUI;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class PartySubCommand implements SubCommand, CommandExecutor, TabCompleter {

    private final PartyManager partyManager;
    private final PlayerProfileManager profileManager;

    public PartySubCommand(PartyManager partyManager, PlayerProfileManager profileManager) {
        this.partyManager = partyManager;
        this.profileManager = profileManager;
    }

    @Override
    public String name() {
        return "party";
    }

    @Override
    public String permission() {
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return execute(sender, args);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a registered guild member.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            new PartyGUI(player, partyManager, profileManager).open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "leave" -> handleLeave(player);
            case "disband" -> handleDisband(player);
            case "info" -> handleInfo(player);
            default -> {
                player.sendMessage(Component.text("Usage: /rpgparty <invite|accept|leave|disband|info>", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /rpgparty invite <player>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player is not online.", NamedTextColor.RED));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You cannot invite yourself.", NamedTextColor.RED));
            return true;
        }
        if (!profileManager.isRegistered(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is not a registered guild member.", NamedTextColor.RED));
            return true;
        }

        Optional<Party> existingParty = partyManager.getParty(player.getUniqueId());
        Party party;

        if (existingParty.isEmpty()) {
            party = partyManager.createParty(player.getUniqueId());
        } else {
            party = existingParty.get();
            if (!party.isLeader(player.getUniqueId())) {
                player.sendMessage(Component.text("Only the party leader can invite.", NamedTextColor.RED));
                return true;
            }
        }

        if (party.isFull()) {
            player.sendMessage(Component.text("Your party is full.", NamedTextColor.RED));
            return true;
        }

        partyManager.addInvite(target.getUniqueId(), player.getUniqueId());
        player.sendMessage(Component.text("Invite sent to " + target.getName() + ".", NamedTextColor.GREEN));
        target.sendMessage(Component.text(player.getName() + " invited you to a party! Use /rpgparty accept", NamedTextColor.LIGHT_PURPLE));
        return true;
    }

    private boolean handleAccept(Player player) {
        boolean success = partyManager.acceptInvite(player);
        if (success) {
            player.sendMessage(Component.text("You joined the party!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("No pending invite or party is full.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleLeave(Player player) {
        Optional<Party> party = partyManager.getParty(player.getUniqueId());
        if (party.isEmpty()) {
            player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
            return true;
        }
        partyManager.leaveParty(player);
        player.sendMessage(Component.text("You left the party.", NamedTextColor.GOLD));
        return true;
    }

    private boolean handleDisband(Player player) {
        Optional<Party> party = partyManager.getParty(player.getUniqueId());
        if (party.isEmpty()) {
            player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
            return true;
        }
        if (!party.get().isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the leader can disband the party.", NamedTextColor.RED));
            return true;
        }
        partyManager.disbandParty(party.get());
        return true;
    }

    private boolean handleInfo(Player player) {
        new PartyGUI(player, partyManager, profileManager).open(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return resolveTabComplete(args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return resolveTabComplete(args);
    }

    private List<String> resolveTabComplete(String[] args) {
        if (args.length == 1) {
            return Arrays.asList("invite", "accept", "leave", "disband", "info");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return names;
        }
        return List.of();
    }
}