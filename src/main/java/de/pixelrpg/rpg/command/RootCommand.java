// src/main/java/de/pixelrpg/rpg/command/RootCommand.java
package de.pixelrpg.rpg.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RootCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public void register(SubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            sendHelp(sender);
            return true;
        }

        String permission = subCommand.permission();
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        boolean handled = subCommand.execute(sender, remaining);
        if (!handled) {
            sender.sendMessage(Component.text("Invalid usage.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            for (String name : subCommands.keySet()) {
                if (name.startsWith(args[0].toLowerCase())) {
                    matches.add(name);
                }
            }
            return matches;
        }

        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Available subcommands:", NamedTextColor.GOLD));
        for (String name : subCommands.keySet()) {
            sender.sendMessage(Component.text(" - " + name, NamedTextColor.YELLOW));
        }
    }
}