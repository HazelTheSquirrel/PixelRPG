package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.companion.CompanionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/** Administrative companion management, including fixed-name Unique rewards. */
public final class CompanionSubCommand implements SubCommand {
    private final CompanionService companionService;

    public CompanionSubCommand(CompanionService companionService) {
        this.companionService = companionService;
    }

    @Override
    public String name() {
        return "companion";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("unique")) {
            sender.sendMessage(Component.text(
                    "Usage: /rpgadmin companion unique <player> <id> <fixed-name> <entity-type>", NamedTextColor.YELLOW));
            return true;
        }
        if (args.length < 5) {
            sender.sendMessage(Component.text(
                    "Usage: /rpgadmin companion unique <player> <id> <fixed-name> <entity-type>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Spieler ist nicht online.", NamedTextColor.RED));
            return true;
        }

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[4].toUpperCase());
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text("Unbekannte EntityType. Nutze eine gültige Minecraft EntityType.", NamedTextColor.RED));
            return true;
        }

        String fixedName = String.join(" ", Arrays.copyOfRange(args, 3, args.length - 1));
        companionService.unlockUnique(target.getUniqueId(), args[2], fixedName, entityType);
        sender.sendMessage(Component.text("Unique-Begleiter für " + target.getName() + " freigeschaltet: " + fixedName, NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("unique");
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
        if (args.length == 5) return Arrays.stream(EntityType.values()).map(EntityType::name).sorted().toList();
        return List.of();
    }
}
