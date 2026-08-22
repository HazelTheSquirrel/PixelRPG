// src/main/java/de/pixelrpg/rpg/command/impl/BlacksmithSubCommand.java
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.gui.BlacksmithGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BlacksmithSubCommand implements SubCommand {

    private final BlacksmithGUI blacksmithGUI;

    public BlacksmithSubCommand(BlacksmithGUI blacksmithGUI) {
        this.blacksmithGUI = blacksmithGUI;
    }

    @Override
    public String name() {
        return "blacksmith";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        blacksmithGUI.open(player);
        return true;
    }
}