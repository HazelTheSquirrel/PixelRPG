package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.dialogue.ReceptionDialog;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DialogueCommand implements CommandExecutor {
    private final PlayerProfileManager profileManager;

    public DialogueCommand(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by a player.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("reception")) {
            new ReceptionDialog(player, profileManager).open();
            return true;
        }

        player.sendMessage(Component.text("Usage: /dialogue [reception]", NamedTextColor.YELLOW));
        return true;
    }
}
