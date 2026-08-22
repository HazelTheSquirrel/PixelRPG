// src/main/java/de/pixelrpg/rpg/command/impl/BossSubCommand.java (VOLLSTÄNDIG, ersetzt alte Datei — spawn-Befehl bleibt für manuelles Testen erhalten)
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.boss.BossDefinition;
import de.pixelrpg.rpg.boss.BossManager;
import de.pixelrpg.rpg.boss.BossRepository;
import de.pixelrpg.rpg.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public final class BossSubCommand implements SubCommand {

    private final BossRepository repository;
    private final BossManager bossManager;

    public BossSubCommand(BossRepository repository, BossManager bossManager) {
        this.repository = repository;
        this.bossManager = bossManager;
    }

    @Override
    public String name() {
        return "boss";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /rpgadmin boss <spawn|reload|list>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /rpgadmin boss spawn <id>", NamedTextColor.RED));
                    return true;
                }
                BossDefinition definition = repository.get(args[1]);
                if (definition == null) {
                    sender.sendMessage(Component.text("Boss not found.", NamedTextColor.RED));
                    return true;
                }
                bossManager.spawnWorldBoss(definition, player.getLocation());
                sender.sendMessage(Component.text("Boss spawned: " + definition.getDisplayName(), NamedTextColor.GREEN));
            }
            case "reload" -> {
                repository.load();
                sender.sendMessage(Component.text("Bosses reloaded.", NamedTextColor.GREEN));
            }
            case "list" -> {
                sender.sendMessage(Component.text("Bosses:", NamedTextColor.GOLD));
                for (BossDefinition definition : repository.getAll()) {
                    sender.sendMessage(Component.text(" - " + definition.getId() + " (" + definition.getDisplayName() + ")", NamedTextColor.YELLOW));
                }
            }
            default -> sender.sendMessage(Component.text("Unknown boss action.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("spawn", "reload", "list");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return repository.getAll().stream().map(BossDefinition::getId).toList();
        }
        return List.of();
    }
}