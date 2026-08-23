package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.companion.CompanionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** Provides data-driven administrative companion unlocks without companion-specific Java hardcodes. */
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
        if (args.length != 3 || !args[0].equalsIgnoreCase("grant")) {
            sender.sendMessage(Component.text("Usage: /rpgadmin companion grant <player> <companion-id>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Spieler ist nicht online.", NamedTextColor.RED));
            return true;
        }

        String companionId = args[2].strip();
        boolean granted = companionService.unlockDefinition(target.getUniqueId(), companionId)
                || companionService.unlockUnique(target.getUniqueId(), companionId, null, null);
        if (!granted) {
            sender.sendMessage(Component.text("Companion konnte nicht freigeschaltet werden. Prüfe ID und Unlock-Regeln.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Companion freigeschaltet: " + companionId + " für " + target.getName(), NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("grant");
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
        if (args.length == 3) return companionService.definitionIds();
        return List.of();
    }
}
