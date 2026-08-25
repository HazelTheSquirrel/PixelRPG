package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.boss.BossDefinition;
import de.pixelrpg.rpg.boss.BossKind;
import de.pixelrpg.rpg.boss.BossManager;
import de.pixelrpg.rpg.boss.BossRepository;
import de.pixelrpg.rpg.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class BossSubCommand implements SubCommand {
    private final BossRepository repository;
    private final BossManager bossManager;

    public BossSubCommand(BossRepository repository, BossManager bossManager) {
        this.repository = repository;
        this.bossManager = bossManager;
    }

    @Override
    public String name() { return "boss"; }

    @Override
    public String permission() { return "rpg.admin"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /rpgadmin boss <event|reload|list>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "event" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /rpgadmin boss event <world-boss-id>", NamedTextColor.RED));
                    return true;
                }
                BossDefinition definition = repository.get(args[1]);
                if (definition == null || definition.getKind() != BossKind.WORLD_EVENT) {
                    sender.sendMessage(Component.text("World event boss not found.", NamedTextColor.RED));
                    return true;
                }
                if (bossManager.hasActiveBossOfType(definition.getId())) {
                    sender.sendMessage(Component.text("That world boss is already active.", NamedTextColor.RED));
                    return true;
                }
                bossManager.spawnWorldBoss(definition, player.getLocation());
                sender.sendMessage(Component.text("World boss event started: " + definition.getDisplayName(), NamedTextColor.GREEN));
            }
            case "reload" -> {
                repository.load();
                sender.sendMessage(Component.text("Boss definitions reloaded.", NamedTextColor.GREEN));
            }
            case "list" -> {
                sender.sendMessage(Component.text("Boss definitions:", NamedTextColor.GOLD));
                for (BossDefinition definition : repository.getAll()) {
                    String location = definition.getKind() == BossKind.WORLD_EVENT
                            ? "WORLD_EVENT"
                            : "BIOME=" + definition.getBiome();
                    sender.sendMessage(Component.text(" - " + definition.getId() + " (" + definition.getDisplayName() + ", " + location + ")", NamedTextColor.YELLOW));
                }
            }
            default -> sender.sendMessage(Component.text("Unknown boss action.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("event", "reload", "list");
        if (args.length == 2 && args[0].equalsIgnoreCase("event")) {
            return repository.getWorldBosses().stream().map(BossDefinition::getId).toList();
        }
        return List.of();
    }
}
