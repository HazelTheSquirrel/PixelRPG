package de.pixelrpg.rpg.lore;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.PlayerKnowledgeStore;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class LoreCommand implements CommandExecutor, TabCompleter {
    private final PlayerProfileManager profileManager;
    private final PlayerKnowledgeStore knowledgeStore;
    private final LoreRegistry loreRegistry;
    private final DialogueEngine dialogueEngine;

    public LoreCommand(PlayerProfileManager profileManager, PlayerKnowledgeStore knowledgeStore,
                       LoreRegistry loreRegistry, DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.knowledgeStore = knowledgeStore;
        this.loreRegistry = loreRegistry;
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du bist noch nicht für PixelRPG registriert.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Component.text("Verwendung: /pixelrpglore <id>", NamedTextColor.YELLOW));
            return true;
        }

        String id = args[0];
        LoreEntry entry = loreRegistry.get(id).orElse(null);
        if (entry == null || !knowledgeStore.knows(player.getUniqueId(), id)) {
            dialogueEngine.openUnavailable(player, "Lore", "Dieser Eintrag wurde noch nicht entdeckt.");
            return true;
        }

        dialogueEngine.openNotice(
                player,
                Component.text(entry.title(), NamedTextColor.GOLD),
                Component.text(entry.text(), NamedTextColor.WHITE),
                Component.text("Schließen", NamedTextColor.GRAY));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase();
        return loreRegistry.all().stream()
                .map(LoreEntry::id)
                .filter(id -> id.toLowerCase().startsWith(prefix))
                .sorted()
                .toList();
    }
}
