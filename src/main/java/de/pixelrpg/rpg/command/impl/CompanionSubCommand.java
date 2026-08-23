package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.companion.CompanionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;

/** Administrative companion management with fixed Unique companion grants. */
public final class CompanionSubCommand implements SubCommand {
    private static final String HAZEL_ID = "unique-hazel";
    private static final String HAZEL_NAME = "Hazel";

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
        if (args.length != 3 || !args[0].equalsIgnoreCase("grant") || !args[2].equalsIgnoreCase(HAZEL_ID)) {
            sender.sendMessage(Component.text(
                    "Usage: /rpgadmin companion grant <player> unique-hazel", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Spieler ist nicht online.", NamedTextColor.RED));
            return true;
        }

        // Hazel is a fixed Unique definition; no command argument can override its name or entity type.
        if (!companionService.unlockUnique(target.getUniqueId(), HAZEL_ID, HAZEL_NAME, EntityType.MANNEQUIN)) {
            sender.sendMessage(Component.text(
                    "Hazel konnte nicht freigeschaltet werden.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text(
                "Unique-Begleiter für " + target.getName() + " freigeschaltet: " + HAZEL_NAME,
                NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("grant");
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
        if (args.length == 3) return List.of(HAZEL_ID);
        return List.of();
    }
}
