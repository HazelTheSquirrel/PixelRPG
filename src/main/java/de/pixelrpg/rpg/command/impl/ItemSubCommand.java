package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** Admin-only command for inspecting definitions and granting concrete PixelRPG items. */
public final class ItemSubCommand implements SubCommand {
    private final ItemService itemService;

    public ItemSubCommand(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public String name() {
        return "item";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public String description() {
        return "PixelRPG-Items verwalten";
    }

    @Override
    public String usage() {
        return "/rpgadmin item <list|give> ...";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return false;

        if (args[0].equalsIgnoreCase("list")) {
            itemService.definitions().stream()
                    .map(ItemDefinition::id)
                    .sorted()
                    .forEach(id -> sender.sendMessage(Component.text(id, NamedTextColor.YELLOW)));
            return true;
        }

        if (!args[0].equalsIgnoreCase("give") || args.length < 3) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Spieler nicht online.", NamedTextColor.RED));
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException exception) {
                sender.sendMessage(Component.text("Ungültige Anzahl.", NamedTextColor.RED));
                return true;
            }
        }
        if (amount < 1 || amount > 64) {
            sender.sendMessage(Component.text("Anzahl muss zwischen 1 und 64 liegen.", NamedTextColor.RED));
            return true;
        }

        ItemDefinition definition = itemService.definitions().stream()
                .filter(value -> value.id().equalsIgnoreCase(args[2]) || value.id().equalsIgnoreCase("pixelrpg:" + args[2]))
                .findFirst().orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("Unbekannte Item-ID.", NamedTextColor.RED));
            return true;
        }
        if (definition.unique() && amount != 1) {
            sender.sendMessage(Component.text("UNIQUE-Items können nur einzeln vergeben werden.", NamedTextColor.RED));
            return true;
        }

        var item = itemService.createAdminItem(definition.id()).orElse(null);
        if (item == null) {
            sender.sendMessage(Component.text("Dieses Item ist bereits vergeben oder konnte nicht erstellt werden.", NamedTextColor.RED));
            return true;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item).values()
                .forEach(stack -> target.getWorld().dropItemNaturally(target.getLocation(), stack));
        sender.sendMessage(Component.text("Item vergeben: " + definition.id(), NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("list", "give");
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return itemService.definitions().stream().map(ItemDefinition::id).toList();
        return List.of();
    }
}
