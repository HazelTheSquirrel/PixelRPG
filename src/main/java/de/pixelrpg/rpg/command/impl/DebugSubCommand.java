package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.quest.Quest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** Developer-only diagnostics for PixelRPG runtime state. */
public final class DebugSubCommand implements SubCommand {
    private final PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();

    @Override public String name() { return "debug"; }
    @Override public String permission() { return "rpg.admin"; }
    @Override public String description() { return "Entwickler- und Testwerkzeuge"; }
    @Override public String usage() { return "/rpgadmin debug <player|npc|quest|boss|stats> ..."; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        switch (args[0].toLowerCase()) {
            case "player" -> player(sender, args);
            case "npc" -> npc(sender);
            case "quest" -> quests(sender);
            case "boss" -> bosses(sender);
            case "stats" -> stats(sender, args);
            default -> { return false; }
        }
        return true;
    }

    private void player(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("Usage: /rpgadmin debug player <name>", NamedTextColor.RED)); return; }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) { sender.sendMessage(Component.text("Spieler nicht online.", NamedTextColor.RED)); return; }
        PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) { sender.sendMessage(Component.text("Kein aktives Profil.", NamedTextColor.RED)); return; }
        sender.sendMessage(Component.text("DEBUG PLAYER " + player.getName(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("UUID=" + player.getUniqueId() + " registered=" + profile.isRegistered() + " level=" + profile.getLevel() + " xp=" + profile.getExperience(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("gold=" + profile.getMoney() + " activeQuests=" + profile.getActiveQuests().size() + " dirty=" + profile.isDirty(), NamedTextColor.GRAY));
    }

    private void npc(CommandSender sender) {
        sender.sendMessage(Component.text("DEBUG NPC count=" + plugin.getNpcManager().getAll().size(), NamedTextColor.GOLD));
        plugin.getNpcManager().getAll().forEach(npc -> sender.sendMessage(Component.text(npc.id() + " | " + npc.type() + " | " + npc.name(), NamedTextColor.GRAY)));
    }

    private void quests(CommandSender sender) {
        sender.sendMessage(Component.text("DEBUG QUEST count=" + plugin.getQuestRepository().getAllQuests().size(), NamedTextColor.GOLD));
        for (Quest quest : plugin.getQuestRepository().getAllQuests()) sender.sendMessage(Component.text(quest.id() + " | " + quest.type() + " | " + quest.title(), NamedTextColor.GRAY));
    }

    private void bosses(CommandSender sender) {
        sender.sendMessage(Component.text("DEBUG BOSS active=" + plugin.getBossManager().getActiveBosses().size(), NamedTextColor.GOLD));
        plugin.getBossManager().getActiveBosses().forEach(boss -> sender.sendMessage(Component.text(boss.id() + " | " + boss.definition().id(), NamedTextColor.GRAY)));
    }

    private void stats(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("Usage: /rpgadmin debug stats <name>", NamedTextColor.RED)); return; }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) { sender.sendMessage(Component.text("Spieler nicht online.", NamedTextColor.RED)); return; }
        sender.sendMessage(Component.text("DEBUG STATS " + player.getName(), NamedTextColor.GOLD));
        var stats = plugin.getStatEngine().calculate(player);
        sender.sendMessage(Component.text(stats.toString(), NamedTextColor.GRAY));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("player", "npc", "quest", "boss", "stats");
        if (args.length == 2 && (args[0].equalsIgnoreCase("player") || args[0].equalsIgnoreCase("stats"))) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}
