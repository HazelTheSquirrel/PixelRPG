// src/main/java/de/pixelrpg/rpg/command/impl/ShopSubCommand.java
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.gui.ShopEditorGUI;
import de.pixelrpg.rpg.shop.ShopEntry;
import de.pixelrpg.rpg.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public final class ShopSubCommand implements SubCommand {

    private final ShopManager shopManager;
    private final ShopEditorGUI shopEditorGUI;

    public ShopSubCommand(ShopManager shopManager, ShopEditorGUI shopEditorGUI) {
        this.shopManager = shopManager;
        this.shopEditorGUI = shopEditorGUI;
    }

    @Override
    public String name() {
        return "shop";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /rpgadmin shop <edit|list> <npcId>", NamedTextColor.RED));
            return true;
        }

        String action = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        return switch (action) {
            case "edit" -> handleEdit(sender, rest);
            case "list" -> handleList(sender, rest);
            default -> {
                sender.sendMessage(Component.text("Unknown shop action.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 1) {
            sender.sendMessage(Component.text("Usage: /rpgadmin shop edit <npcId>", NamedTextColor.RED));
            return true;
        }
        shopEditorGUI.open(player, args[0]);
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /rpgadmin shop list <npcId>", NamedTextColor.RED));
            return true;
        }

        List<ShopEntry> entries = shopManager.getEntries(args[0]);
        sender.sendMessage(Component.text("Shop entries for " + args[0] + ":", NamedTextColor.GOLD));
        int index = 0;
        for (ShopEntry entry : entries) {
            sender.sendMessage(Component.text(" [" + index + "] " + entry.item().getType() + " - " + entry.price() + " Gold", NamedTextColor.YELLOW));
            index++;
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("edit", "list");
        }
        return List.of();
    }
}