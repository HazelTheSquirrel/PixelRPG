package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Admin command for directly manipulating a player's quest state for administration and testing. */
public final class QuestAdminSubCommand implements SubCommand {
    private final QuestManager questManager;
    private final PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();

    public QuestAdminSubCommand(QuestManager questManager) { this.questManager = questManager; }
    @Override public String name() { return "quest"; }
    @Override public String permission() { return "rpg.admin"; }
    @Override public String description() { return "Quests verwalten und testen"; }
    @Override public String usage() { return "/rpgadmin quest <reload|list|give|remove|reset|progress|complete> ..."; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "list" -> list(sender);
            case "give" -> give(sender, args);
            case "remove", "abandon" -> remove(sender, args);
            case "reset" -> reset(sender, args);
            case "progress" -> progress(sender, args);
            case "complete" -> complete(sender, args);
            default -> false;
        };
    }

    private boolean reload(CommandSender sender) {
        questManager.getRepository().load();
        sender.sendMessage(Component.text("Quests reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean list(CommandSender sender) {
        sender.sendMessage(Component.text("Quests:", NamedTextColor.GOLD));
        for (Quest quest : questManager.getRepository().getAllQuests()) sender.sendMessage(Component.text(" - " + quest.id() + " [" + quest.type() + "] " + quest.title(), NamedTextColor.YELLOW));
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        Player target = player(sender, args[1]);
        Quest quest = quest(args[2]);
        if (target == null || quest == null) return true;
        PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(target.getUniqueId()).orElse(null);
        if (profile == null) { error(sender, "Kein aktives Profil."); return true; }
        long expiry = quest.hasTimeLimit() ? System.currentTimeMillis() + quest.durationMinutes() * 60_000L : 0L;
        profile.startQuest(new QuestProgress(quest.id(), 0, expiry));
        plugin.getPlayerProfileManager().saveProfileAsync(target.getUniqueId());
        sender.sendMessage(Component.text("Quest vergeben: " + quest.id() + " an " + target.getName(), NamedTextColor.GREEN));
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        Player target = player(sender, args[1]);
        Quest quest = quest(args[2]);
        if (target == null || quest == null) return true;
        PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(target.getUniqueId()).orElse(null);
        if (profile == null || !profile.hasActiveQuest(quest.id())) { error(sender, "Quest ist nicht aktiv."); return true; }
        profile.removeActiveQuest(quest.id());
        plugin.getPlayerProfileManager().saveProfileAsync(target.getUniqueId());
        sender.sendMessage(Component.text("Quest entfernt: " + quest.id(), NamedTextColor.GREEN));
        return true;
    }

    private boolean reset(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        Player target = player(sender, args[1]);
        Quest quest = quest(args[2]);
        if (target == null || quest == null) return true;
        PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(target.getUniqueId()).orElse(null);
        if (profile == null) { error(sender, "Kein aktives Profil."); return true; }
        profile.removeActiveQuest(quest.id());
        HashSet<String> completed = new HashSet<>(profile.getCompletedQuests());
        completed.remove(quest.id());
        profile.setCompletedQuests(completed);
        plugin.getPlayerProfileManager().saveProfileAsync(target.getUniqueId());
        sender.sendMessage(Component.text("Queststatus vollständig zurückgesetzt: " + quest.id(), NamedTextColor.GREEN));
        return true;
    }

    private boolean progress(CommandSender sender, String[] args) {
        if (args.length < 4) return false;
        Player target = player(sender, args[1]);
        Quest quest = quest(args[2]);
        if (target == null || quest == null) return true;
        try {
            int amount = Integer.parseInt(args[3]);
            if (amount < 0) throw new IllegalArgumentException();
            PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(target.getUniqueId()).orElse(null);
            if (profile == null || !profile.hasActiveQuest(quest.id())) { error(sender, "Quest ist nicht aktiv."); return true; }
            profile.getActiveQuests().get(quest.id()).setCurrentAmount(Math.min(amount, quest.requiredAmount()));
            plugin.getPlayerProfileManager().saveProfileAsync(target.getUniqueId());
            sender.sendMessage(Component.text("Quest-Fortschritt gesetzt: " + amount + "/" + quest.requiredAmount(), NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) { error(sender, "Ungültiger Fortschritt."); }
        return true;
    }

    private boolean complete(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        Player target = player(sender, args[1]);
        Quest quest = quest(args[2]);
        if (target == null || quest == null) return true;
        PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(target.getUniqueId()).orElse(null);
        if (profile == null || !profile.hasActiveQuest(quest.id())) { error(sender, "Quest ist nicht aktiv."); return true; }
        profile.removeActiveQuest(quest.id());
        profile.markQuestCompleted(quest.id());
        if (quest.rewardMoney() > 0.0D) profile.addMoney(quest.rewardMoney());
        if (quest.rewardExp() > 0L) plugin.getPlayerProfileManager().addExperience(target.getUniqueId(), quest.rewardExp());
        for (String reward : quest.rewardItemMaterials()) giveReward(target, reward, quest.requiredLevel());
        plugin.getPlayerProfileManager().saveProfileAsync(target.getUniqueId());
        sender.sendMessage(Component.text("Quest administrativ abgeschlossen: " + quest.id(), NamedTextColor.GREEN));
        return true;
    }

    private void giveReward(Player player, String raw, int fallbackLevel) {
        String[] parts = raw.split("\\|", -1);
        try {
            Material material = Material.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            if (parts.length == 1) { player.getInventory().addItem(new ItemStack(material)); return; }
            ItemRarity rarity = ItemRarity.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
            int level = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : fallbackLevel;
            RPGItemBuilder.createItem(material, rarity, Math.max(1, level)).ifPresent(item -> player.getInventory().addItem(item));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid quest admin reward: " + raw);
        }
    }

    private Player player(CommandSender sender, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) error(sender, "Spieler muss online sein.");
        return player;
    }

    private Quest quest(String id) {
        Quest quest = questManager.getRepository().getQuest(id);
        if (quest == null) return null;
        return quest;
    }

    private void error(CommandSender sender, String message) { sender.sendMessage(Component.text(message, NamedTextColor.RED)); }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("reload", "list", "give", "remove", "reset", "progress", "complete");
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 3) return questManager.getRepository().getAllQuests().stream().map(Quest::id).sorted().toList();
        return List.of();
    }
}
