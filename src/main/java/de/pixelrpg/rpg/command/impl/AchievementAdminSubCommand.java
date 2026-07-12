// src/main/java/de/pixelrpg/rpg/command/impl/AchievementAdminSubCommand.java
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.achievement.AchievementDefinition;
import de.pixelrpg.rpg.achievement.AchievementManager;
import de.pixelrpg.rpg.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AchievementAdminSubCommand implements SubCommand {

    private final AchievementManager achievementManager;

    public AchievementAdminSubCommand(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }

    @Override
    public String name() {
        return "achievement";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /rpgadmin achievement <reload|list|grant>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                achievementManager.getRepository().load();
                sender.sendMessage(Component.text("Achievements reloaded.", NamedTextColor.GREEN));
            }
            case "list" -> {
                sender.sendMessage(Component.text("Achievements:", NamedTextColor.GOLD));
                for (AchievementDefinition definition : achievementManager.getRepository().getAll()) {
                    sender.sendMessage(Component.text(" - " + definition.id() + " (" + definition.displayName() + ")", NamedTextColor.YELLOW));
                }
            }
            case "grant" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /rpgadmin achievement grant <player> <id>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not online.", NamedTextColor.RED));
                    return true;
                }
                achievementManager.forceUnlock(target, args[2]);
                sender.sendMessage(Component.text("Achievement granted.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown achievement action.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "list", "grant");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("grant")) {
            List<String> ids = new ArrayList<>();
            for (AchievementDefinition definition : achievementManager.getRepository().getAll()) {
                ids.add(definition.id());
            }
            return ids;
        }
        return List.of();
    }
}