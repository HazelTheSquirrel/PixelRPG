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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

public final class RootCommand implements CommandExecutor, TabCompleter {
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public void register(SubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        SubCommand subCommand = subCommands.get(name);
        if (subCommand == null) {
            sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
            sendHelp(sender);
            return true;
        }

        String permission = subCommand.permission();
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("You do not have permission to use /" + command.getName() + " " + name + ".", NamedTextColor.RED));
            return true;
        }

        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        if (!subCommand.execute(sender, remaining)) {
            sender.sendMessage(Component.text("Invalid usage for /" + command.getName() + " " + name + ".", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                SubCommand subCommand = entry.getValue();
                String permission = subCommand.permission();
                if ((permission == null || sender.hasPermission(permission)) && entry.getKey().startsWith(prefix)) {
                    matches.add(entry.getKey());
                }
            }
            return matches;
        }

        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
            if (subCommand != null) {
                String permission = subCommand.permission();
                if (permission != null && !sender.hasPermission(permission)) return List.of();
                return filterSuggestions(subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length)), args[args.length - 1]);
            }
        }

        return List.of();
    }

    private List<String> filterSuggestions(List<String> suggestions, String prefix) {
        if (suggestions == null || suggestions.isEmpty()) return List.of();
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(value -> value != null && value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("PixelRPG Admin Commands", NamedTextColor.GOLD));
        subCommands.forEach((name, subCommand) -> {
            String permission = subCommand.permission();
            if (permission == null || sender.hasPermission(permission)) {
                sender.sendMessage(Component.text("/rpgadmin " + name, NamedTextColor.YELLOW));
            }
        });
    }
}
