package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Admin command for inspecting and modifying active PixelRPG player profiles. */
public final class PlayerAdminSubCommand implements SubCommand {
    private final PlayerProfileManager profileManager;

    public PlayerAdminSubCommand() {
        this.profileManager = PixelRPGPlugin.getInstance().getPlayerProfileManager();
    }

    @Override public String name() { return "player"; }
    @Override public String permission() { return "rpg.admin"; }
    @Override public String description() { return "PixelRPG-Spielerdaten verwalten"; }
    @Override public String usage() { return "/rpgadmin player <info|set-level|set-xp|set-gold|set-profession|reset> <player> ..."; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Spieler muss online sein.", NamedTextColor.RED));
            return true;
        }
        PlayerProfile profile = profileManager.getProfile(target.getUniqueId()).orElse(null);
        if (profile == null) {
            sender.sendMessage(Component.text("Kein aktives PixelRPG-Profil gefunden.", NamedTextColor.RED));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> sendInfo(sender, target, profile);
            case "set-level" -> setLevel(sender, target, profile, args);
            case "set-xp" -> setXp(sender, target, profile, args);
            case "set-gold" -> setGold(sender, target, profile, args);
            case "set-profession" -> setProfession(sender, profile, args);
            case "reset" -> reset(sender, target, profile);
            default -> { return false; }
        }
        return true;
    }

    private void sendInfo(CommandSender sender, Player player, PlayerProfile profile) {
        sender.sendMessage(Component.text("PixelRPG: " + player.getName(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Level: " + profile.getLevel() + " | XP: " + profile.getExperience(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Gold: " + profile.getMoney(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Aktive Quests: " + profile.getActiveQuests().size() + " | Abgeschlossen: " + profile.getCompletedQuests().size(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Berufe: " + profile.getProfessionLevels(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Erlernte Berufe: " + profile.getLearnedProfessions(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Ausrüstungsslots: " + profile.getEquipment().keySet(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Spielzeit: " + profile.getPlaytimeMillis() + " ms", NamedTextColor.GRAY));
    }

    private void setLevel(CommandSender sender, Player target, PlayerProfile profile, String[] args) {
        if (args.length < 3) { sender.sendMessage(Component.text("Usage: /rpgadmin player set-level <player> <1-99>", NamedTextColor.RED)); return; }
        try {
            int level = Integer.parseInt(args[2]);
            if (!Level.isValidNormalLevel(level)) throw new IllegalArgumentException();
            profile.setExperience(Level.getRequiredExperience(level));
            profileManager.saveProfileAsync(target.getUniqueId());
            sender.sendMessage(Component.text("Level von " + target.getName() + " auf " + level + " gesetzt.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text("Level muss zwischen 1 und 99 liegen.", NamedTextColor.RED));
        }
    }

    private void setXp(CommandSender sender, Player target, PlayerProfile profile, String[] args) {
        if (args.length < 3) { sender.sendMessage(Component.text("Usage: /rpgadmin player set-xp <player> <xp>", NamedTextColor.RED)); return; }
        try {
            long xp = Long.parseLong(args[2]);
            if (xp < 0) throw new IllegalArgumentException();
            profile.setExperience(xp);
            profileManager.saveProfileAsync(target.getUniqueId());
            sender.sendMessage(Component.text("XP gesetzt: " + xp + " (Level " + profile.getLevel() + ").", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text("Ungültige XP-Menge.", NamedTextColor.RED));
        }
    }

    private void setGold(CommandSender sender, Player target, PlayerProfile profile, String[] args) {
        if (args.length < 3) { sender.sendMessage(Component.text("Usage: /rpgadmin player set-gold <player> <amount>", NamedTextColor.RED)); return; }
        try {
            double amount = Double.parseDouble(args[2]);
            if (!Double.isFinite(amount) || amount < 0.0D) throw new IllegalArgumentException();
            profile.setMoney(amount);
            profileManager.saveProfileAsync(target.getUniqueId());
            sender.sendMessage(Component.text("Gold gesetzt: " + amount + ".", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text("Ungültiger Goldbetrag.", NamedTextColor.RED));
        }
    }

    private void setProfession(CommandSender sender, PlayerProfile profile, String[] args) {
        if (args.length < 4) { sender.sendMessage(Component.text("Usage: /rpgadmin player set-profession <player> <profession> <level>", NamedTextColor.RED)); return; }
        try {
            Profession profession = Profession.valueOf(args[2].toUpperCase(Locale.ROOT));
            int level = Integer.parseInt(args[3]);
            profile.setProfessionLevel(profession, level);
            profile.learnProfession(profession);
            sender.sendMessage(Component.text("Beruf " + profession + " auf Level " + profile.getProfessionLevel(profession) + " gesetzt.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text("Ungültiger Beruf oder Level.", NamedTextColor.RED));
        }
    }

    private void reset(CommandSender sender, Player target, PlayerProfile profile) {
        profile.resetProgress();
        profileManager.saveProfileAsync(target.getUniqueId());
        sender.sendMessage(Component.text("PixelRPG-Profil von " + target.getName() + " wurde zurückgesetzt.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("info", "set-level", "set-xp", "set-gold", "set-profession", "reset");
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("set-profession")) return Arrays.stream(Profession.values()).map(Enum::name).toList();
        return List.of();
    }
}
