package de.pixelrpg.rpg.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {
    String name();

    String permission();

    boolean execute(CommandSender sender, String[] args);

    default String description() {
        return "";
    }

    default String usage() {
        return "/pixelrpg " + name();
    }

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
