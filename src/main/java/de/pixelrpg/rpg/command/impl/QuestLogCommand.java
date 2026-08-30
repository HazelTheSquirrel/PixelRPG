package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.gui.QuestLogGUI;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class QuestLogCommand implements SubCommand, CommandExecutor {

    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;

    public QuestLogCommand(QuestManager questManager, PlayerProfileManager profileManager) {
        this.questManager = questManager;
        this.profileManager = profileManager;
    }

    @Override
    public String name() {
        return "questlog";
    }

    @Override
    public String permission() {
        return "rpg.member";
    }

    @Override
    public String description() {
        return "Öffnet das Questlog";
    }

    @Override
    public String usage() {
        return "/pixelrpg questlog";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return onCommand(sender, null, "pixelrpg", args);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können diesen Befehl nutzen.", NamedTextColor.RED));
            return true;
        }
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst registriertes Rathausmitglied sein.", NamedTextColor.RED));
            return true;
        }
        new QuestLogGUI(player, questManager, profileManager).open(player);
        return true;
    }
}
