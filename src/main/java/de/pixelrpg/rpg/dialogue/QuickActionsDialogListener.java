package de.pixelrpg.rpg.dialogue;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class QuickActionsDialogListener implements Listener {
    private static final Key PROFILE_ACTION = Key.key("pixelrpg:character_card/profile");
    private static final Key ACTIVE_QUESTS_ACTION = Key.key("pixelrpg:character_card/active_quests");
    private static final Key COMPANIONS_ACTION = Key.key("pixelrpg:character_card/companions");
    private static final Key PROFESSIONS_ACTION = Key.key("pixelrpg:character_card/professions");
    private static final Key GUILD_ACTION = Key.key("pixelrpg:character_card/guild");
    private static final Key CLOSE_ACTION = Key.key("pixelrpg:character_card/close");

    private final QuickActionsDialogService service;

    public QuickActionsDialogListener(QuickActionsDialogService service) {
        this.service = service;
    }

    // Handles the character profile action from the native quick-actions dialog.
    @EventHandler
    public void onProfile(PlayerCustomClickEvent event) {
        handle(event, PROFILE_ACTION, service::openProfile);
    }

    // Handles the active-quests action from the native quick-actions dialog.
    @EventHandler
    public void onActiveQuests(PlayerCustomClickEvent event) {
        handle(event, ACTIVE_QUESTS_ACTION, service::openActiveQuests);
    }

    // Handles the companion action from the native quick-actions dialog.
    @EventHandler
    public void onCompanions(PlayerCustomClickEvent event) {
        handle(event, COMPANIONS_ACTION, service::openCompanions);
    }

    // Handles the profession action from the native quick-actions dialog.
    @EventHandler
    public void onProfessions(PlayerCustomClickEvent event) {
        handle(event, PROFESSIONS_ACTION, service::openProfessions);
    }

    // Handles the guild action from the native quick-actions dialog.
    @EventHandler
    public void onGuild(PlayerCustomClickEvent event) {
        handle(event, GUILD_ACTION, service::openGuild);
    }

    // Handles closing the native quick-actions dialog.
    @EventHandler
    public void onClose(PlayerCustomClickEvent event) {
        handle(event, CLOSE_ACTION, Player::closeDialog);
    }

    private void handle(PlayerCustomClickEvent event, Key identifier, java.util.function.Consumer<Player> action) {
        if (!identifier.equals(event.getIdentifier())) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        action.accept(connection.getPlayer());
    }
}
