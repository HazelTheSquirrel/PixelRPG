// src/main/java/de/pixelrpg/rpg/quest/QuestLogCommand.java
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.gui.QuestLogGUI;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class QuestLogCommand implements CommandExecutor {

    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;

    public QuestLogCommand(QuestManager questManager, PlayerProfileManager profileManager) {
        this.questManager = questManager;
        this.profileManager = profileManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a registered guild member.", NamedTextColor.RED));
            return true;
        }
        new QuestLogGUI(player, questManager, profileManager).open(player);
        return true;
    }
}