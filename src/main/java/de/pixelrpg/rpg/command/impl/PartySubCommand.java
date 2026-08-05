package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.gui.PartyGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
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
    private final LanguageManager lang;

    public PartySubCommand(PartyManager partyManager, PlayerProfileManager profileManager) {
        this.partyManager = partyManager;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
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
            lang.send(player, "common.not-registered");
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
                lang.send(player, "party.usage-root");
                yield true;
            }
        };
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            lang.send(player, "party.usage-invite");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            lang.send(player, "party.player-not-online");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            lang.send(player, "party.cannot-invite-self");
            return true;
        }
        if (!profileManager.isRegistered(target.getUniqueId())) {
            lang.send(player, "party.target-not-registered", "player", target.getName());
            return true;
        }

        Optional<Party> existingParty = partyManager.getParty(player.getUniqueId());
        Party party;

        if (existingParty.isEmpty()) {
            party = partyManager.createParty(player.getUniqueId());
        } else {
            party = existingParty.get();
            if (!party.isLeader(player.getUniqueId())) {
                lang.send(player, "party.only-leader-invite");
                return true;
            }
        }

        if (party.isFull()) {
            lang.send(player, "party.party-full");
            return true;
        }

        partyManager.addInvite(target.getUniqueId(), player.getUniqueId());
        lang.send(player, "party.invite-sent", "player", target.getName());
        lang.send(target, "party.invited-you", "player", player.getName());
        return true;
    }

    private boolean handleAccept(Player player) {
        boolean success = partyManager.acceptInvite(player);
        if (success) {
            lang.send(player, "party.joined");
        } else {
            lang.send(player, "party.no-pending-invite");
        }
        return true;
    }

    private boolean handleLeave(Player player) {
        Optional<Party> party = partyManager.getParty(player.getUniqueId());
        if (party.isEmpty()) {
            lang.send(player, "party.not-in-party");
            return true;
        }
        partyManager.leaveParty(player);
        lang.send(player, "party.left");
        return true;
    }

    private boolean handleDisband(Player player) {
        Optional<Party> party = partyManager.getParty(player.getUniqueId());
        if (party.isEmpty()) {
            lang.send(player, "party.not-in-party");
            return true;
        }
        if (!party.get().isLeader(player.getUniqueId())) {
            lang.send(player, "party.only-leader-disband");
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