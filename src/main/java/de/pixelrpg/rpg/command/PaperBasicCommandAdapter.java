package de.pixelrpg.rpg.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Adapts the existing command implementations to Paper's current BasicCommand API. */
public final class PaperBasicCommandAdapter implements BasicCommand {
    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;
    private final Command command;
    private final String permission;

    public PaperBasicCommandAdapter(String name, CommandExecutor executor, TabCompleter tabCompleter, String permission) {
        this.executor = executor;
        this.tabCompleter = tabCompleter;
        this.command = new Command(name) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }
        };
        this.permission = "pixelrpg".equalsIgnoreCase(name) ? "rpg.member" : permission;

        List<String> aliases = legacyAliases(name);
        if (!aliases.isEmpty()) {
            JavaPlugin.getProvidingPlugin(PaperBasicCommandAdapter.class).registerCommand(name, aliases, this);
        }
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        executor.onCommand(source.getSender(), command, command.getName(), args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (tabCompleter == null) return List.of();
        List<String> suggestions = tabCompleter.onTabComplete(source.getSender(), command, command.getName(), args);
        return suggestions == null ? List.of() : suggestions;
    }

    @Override
    public String permission() {
        return permission;
    }

    private static List<String> legacyAliases(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "pixelrpg" -> List.of("rpgadmin");
            case "pixelrpgparty" -> List.of("rpgparty");
            case "pixelrpgquestlog" -> List.of("questlog");
            case "pixelrpgdialogue" -> List.of("dialogue");
            default -> List.of();
        };
    }
}
