package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.gui.QuestLogGUI;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/** Opens the player quest log through the current Paper command API. */
public final class QuestLogCommand implements BasicCommand {
    private final QuestService quests;
    private final PlayerProfileManager profiles;
    private final ItemService itemService;

    public QuestLogCommand(QuestService quests, PlayerProfileManager profiles, ItemService itemService) {
        this.quests = quests;
        this.profiles = profiles;
        this.itemService = itemService;
    }

    @Override public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) return;
        if (!profiles.isRegistered(player.getUniqueId())) return;
        new QuestLogGUI(player, quests, profiles, itemService).open(player);
    }

    @Override public Collection<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }

    @Override public String permission() {
        return "rpg.member";
    }
}
