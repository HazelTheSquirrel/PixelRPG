// src/main/java/de/pixelrpg/rpg/command/impl/ShopSubCommand.java (VOLLSTÄNDIG, ersetzt alte Datei — Tab-Complete mit echten NPC-IDs, Existenzprüfung beim Edit/List)
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.gui.ShopEditorGUI;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.shop.ShopEntry;
import de.pixelrpg.rpg.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ShopSubCommand implements SubCommand {

    private final ShopManager shopManager;
    private final ShopEditorGUI shopEditorGUI;
    private final NpcManager npcManager;

    public ShopSubCommand(ShopManager shopManager, ShopEditorGUI shopEditorGUI, NpcManager npcManager) {
        this.shopManager = shopManager;
        this.shopEditorGUI = shopEditorGUI;
        this.npcManager = npcManager;
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

        // Prüft, ob die eingegebene ID tatsächlich zu einem existierenden NPC gehört —
        // verhindert, dass Admins versehentlich unter einer falschen ID editieren, die
        // vom eigentlichen Shop-NPC (npc.id()) nie abgefragt wird.
        if (npcManager.getById(args[0]).isEmpty()) {
            player.sendMessage(Component.text(
                    "No NPC with internal id '" + args[0] + "' exists. Use /rpgadmin npc list to find the correct id.",
                    NamedTextColor.RED));
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

        if (npcManager.getById(args[0]).isEmpty()) {
            sender.sendMessage(Component.text(
                    "No NPC with internal id '" + args[0] + "' exists. Use /rpgadmin npc list to find the correct id.",
                    NamedTextColor.RED));
            return true;
        }

        List<ShopEntry> entries = shopManager.getEntries(args[0]);
        sender.sendMessage(Component.text("Shop entries for " + args[0] + ":", NamedTextColor.GOLD));
        if (entries.isEmpty()) {
            sender.sendMessage(Component.text(" (empty)", NamedTextColor.GRAY));
        }
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
        if (args.length == 2 && (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("list"))) {
            List<String> ids = new ArrayList<>();
            for (RPGNpc npc : npcManager.getAll()) {
                ids.add(npc.id());
            }
            return ids;
        }
        return List.of();
    }
}