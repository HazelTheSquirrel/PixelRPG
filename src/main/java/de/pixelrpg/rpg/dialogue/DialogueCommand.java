package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class DialogueCommand implements CommandExecutor, TabCompleter {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public DialogueCommand(PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du bist noch nicht für PixelRPG registriert.", NamedTextColor.RED));
            return true;
        }

        dialogueEngine.openUnavailable(
                player,
                "PixelRPG Dialog",
                "Der zentrale Dialogdienst ist aktiv. NPC-Dialoge verwenden dieselbe Dialog-Engine.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
