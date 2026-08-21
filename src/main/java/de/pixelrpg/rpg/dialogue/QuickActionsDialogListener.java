package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public final class QuickActionsDialogListener implements Listener {
    private static final Key CHARACTER_CARD_ACTION = Key.key("pixelrpg:character_card/open");
    private final QuickActionsDialogService service;
    private final CompanionDialog companionDialog;
    private final ProfessionDialog professionDialog;
    private final DialogueEngine dialogueEngine;

    public QuickActionsDialogListener(QuickActionsDialogService service,
                                      CompanionDialog companionDialog,
                                      PlayerProfileManager profileManager,
                                      DialogueEngine dialogueEngine) {
        this.service = service;
        this.companionDialog = companionDialog;
        this.professionDialog = new ProfessionDialog(profileManager, dialogueEngine);
        this.dialogueEngine = dialogueEngine;
    }

    /** Handles the G-triggered native action and opens the complete player character card. */
    @EventHandler
    public void onCharacterCardAction(PlayerCustomClickEvent event) {
        if (!CHARACTER_CARD_ACTION.equals(event.getIdentifier())) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;

        Player player = connection.getPlayer();
        if (!service.isAvailable(player)) return;

        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(
                Component.text("Begleiter"), NamedTextColor.LIGHT_PURPLE,
                companionDialog::open));
        actions.add(dialogueEngine.actionButton(
                Component.text("Berufe"), NamedTextColor.GREEN,
                professionDialog::open));
        actions.add(dialogueEngine.actionButton(
                Component.text("Schließen"), NamedTextColor.GRAY,
                Player::closeDialog));

        dialogueEngine.openMultiAction(
                player,
                Component.text("PixelRPG – Charakter", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(service.characterCard(player))),
                actions,
                1);
    }
}
