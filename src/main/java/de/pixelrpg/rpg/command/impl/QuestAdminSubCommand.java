// src/main/java/de/pixelrpg/rpg/command/impl/QuestAdminSubCommand.java
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public final class QuestAdminSubCommand implements SubCommand {

    private final QuestManager questManager;

    public QuestAdminSubCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public String name() {
        return "quest";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /rpgadmin quest <reload|list>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                questManager.getRepository().load();
                sender.sendMessage(Component.text("Quests reloaded.", NamedTextColor.GREEN));
            }
            case "list" -> {
                sender.sendMessage(Component.text("Quests:", NamedTextColor.GOLD));
                for (Quest quest : questManager.getRepository().getAllQuests()) {
                    sender.sendMessage(Component.text(" - " + quest.id() + " [" + quest.type() + "] " + quest.title(), NamedTextColor.YELLOW));
                }
            }
            default -> sender.sendMessage(Component.text("Unknown quest action.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "list");
        }
        return List.of();
    }
}