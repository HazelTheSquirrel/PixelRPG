package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.PixelRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class DebugSubCommand implements SubCommand {
    private final PixelRPGPlugin plugin;

    public DebugSubCommand(PixelRPGPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() { return "debug"; }

    @Override
    public String permission() { return "pixelrpg.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /rpgadmin debug <player|npc|quest|boss|stats>", NamedTextColor.RED)); return; }
        switch (args[0].toLowerCase()) {
            case "player" -> player(sender, args);
            case "npc" -> npc(sender);
            case "quest" -> quest(sender);
            case "boss" -> boss(sender);
            case "stats" -> stats(sender, args);
            default -> sender.sendMessage(Component.text("Unknown debug target.", NamedTextColor.RED));
        }
    }

    private void player(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("Usage: /rpgadmin debug player <name>", NamedTextColor.RED)); return; }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) { sender.sendMessage(Component.text("Spieler nicht online.", NamedTextColor.RED)); return; }
        sender.sendMessage(Component.text("DEBUG PLAYER " + player.getName(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("UUID=" + player.getUniqueId(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("World=" + player.getWorld().getName(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Location=" + player.getLocation(), NamedTextColor.GRAY));
    }

    private void npc(CommandSender sender) {
        sender.sendMessage(Component.text("DEBUG NPC registered=" + plugin.getNpcManager().getRegisteredCount(), NamedTextColor.GOLD));
    }

    private void quest(CommandSender sender) {
        sender.sendMessage(Component.text("DEBUG QUEST loaded=" + plugin.getQuestManager().getRepository().size(), NamedTextColor.GOLD));
    }

    private void boss(CommandSender sender) {
        sender.sendMessage(Component.text("DEBUG BOSS active=" + plugin.getBossManager().getActiveBossCount(), NamedTextColor.GOLD));
    }

    private void stats(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("Usage: /rpgadmin debug stats <name>", NamedTextColor.RED)); return; }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) { sender.sendMessage(Component.text("Spieler nicht online.", NamedTextColor.RED)); return; }
        plugin.getStatEngine().recalculate(player);
        var stats = plugin.getStatEngine().getCachedStats(player.getUniqueId());
        sender.sendMessage(Component.text("DEBUG STATS " + player.getName(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("HP=" + stats.maxHealth() + " Armor=" + stats.armor(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Crit=" + stats.critChance() + "% CritDamage=" + stats.critDamageMultiplier() + "x Lifesteal=" + stats.lifestealBonus() + "%", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Reach=" + stats.reach() + " AttackPower=" + stats.attackPower(), NamedTextColor.GRAY));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("player", "npc", "quest", "boss", "stats");
        if (args.length == 2 && (args[0].equalsIgnoreCase("player") || args[0].equalsIgnoreCase("stats"))) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}