// src/main/java/de/pixelrpg/rpg/command/impl/FusionSubCommand.java
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.gui.ItemFusionGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FusionSubCommand implements SubCommand {

    private final ItemFusionGUI itemFusionGUI;

    public FusionSubCommand(ItemFusionGUI itemFusionGUI) {
        this.itemFusionGUI = itemFusionGUI;
    }

    @Override
    public String name() {
        return "fusion";
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
        itemFusionGUI.open(player);
        return true;
    }
}