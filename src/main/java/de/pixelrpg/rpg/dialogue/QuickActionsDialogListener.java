package de.pixelrpg.rpg.dialogue;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public final class QuickActionsDialogListener implements Listener {
    private static final Key CHARACTER_CARD_ACTION = Key.key("pixelrpg:character_card/open");
    private final QuickActionsDialogService service;

    public QuickActionsDialogListener(QuickActionsDialogService service) {
        this.service = service;
    }

    /** Handles the native quick-action button and opens the player's dynamic character card. */
    @EventHandler
    public void onCharacterCardAction(PlayerCustomClickEvent event) {
        if (!CHARACTER_CARD_ACTION.equals(event.getIdentifier())) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;

        Player player = connection.getPlayer();
        if (!service.isAvailable(player)) return;

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(Component.text("PixelRPG – Charakterkarte"))
                        .body(List.of(DialogBody.plainMessage(service.characterCard(player))))
                        .build())
                .type(DialogType.notice())));
    }
}
