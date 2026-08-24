package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.command.impl.ItemSubCommand;
import de.pixelrpg.rpg.item.ItemService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RootCommand implements CommandExecutor, TabCompleter {
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public RootCommand() {
        register(new ItemSubCommand(new ItemService()));
    }

    public void register(SubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, command.getName());
            return true;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        SubCommand subCommand = subCommands.get(name);
        if (subCommand == null) {
            sender.sendMessage(Component.text("Unbekannter Unterbefehl: " + args[0], NamedTextColor.RED));
            sendHelp(sender, command.getName());
            return true;
        }

        String permission = subCommand.permission();
        if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("Keine Berechtigung für /" + command.getName() + " " + name + ".", NamedTextColor.RED));
            return true;
        }

        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        if (!subCommand.execute(sender, remaining)) {
            sender.sendMessage(Component.text("Verwendung: " + subCommand.usage(), NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return subCommands.entrySet().stream()
                    .filter(entry -> hasPermission(sender, entry.getValue()))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return subCommands.entrySet().stream()
                    .filter(entry -> hasPermission(sender, entry.getValue()))
                    .map(Map.Entry::getKey)
                    .filter(value -> value.startsWith(prefix))
                    .sorted()
                    .toList();
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null || !hasPermission(sender, subCommand)) return List.of();

        List<String> suggestions = subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        if (suggestions == null || suggestions.isEmpty()) return List.of();

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(value -> value != null && value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean hasPermission(CommandSender sender, SubCommand subCommand) {
        String permission = subCommand.permission();
        return permission == null || permission.isBlank() || sender.hasPermission(permission);
    }

    private void sendHelp(CommandSender sender, String commandName) {
        sender.sendMessage(Component.text("PixelRPG Admin-Befehle", NamedTextColor.GOLD));
        subCommands.values().stream()
                .filter(subCommand -> hasPermission(sender, subCommand))
                .forEach(subCommand -> {
                    Component line = Component.text(subCommand.usage(), NamedTextColor.YELLOW);
                    if (!subCommand.description().isBlank()) {
                        line = line.append(Component.text(" – " + subCommand.description(), NamedTextColor.GRAY));
                    }
                    sender.sendMessage(line);
                });
    }
}
