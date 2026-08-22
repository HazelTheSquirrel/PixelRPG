package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native guild reception dialog with textual player information instead of a player-card view. */
public final class ReceptionDialog {
    private final Player player;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final LanguageManager lang;

    public ReceptionDialog(Player player, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.player = player;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.lang = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getLanguageManager();
    }

    public void open() {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        boolean registered = profile != null && profile.isRegisteredInGuild();

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(
                "Willkommen im Rathaus. Hier verwalten wir deine Mitgliedschaft und deinen Charakterstatus.",
                NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(
                lang.get("reception.status").append(
                        registered
                                ? lang.get("reception.status-member").color(NamedTextColor.GREEN)
                                : lang.get("reception.status-not-registered").color(NamedTextColor.RED))));

        List<ActionButton> actions = new ArrayList<>();
        if (!registered) {
            actions.add(dialogueEngine.actionButton(
                    lang.get("reception.register-button"),
                    NamedTextColor.GREEN,
                    target -> {
                        profileManager.registerToGuild(target);
                        new ReceptionDialog(target, profileManager, dialogueEngine).open();
                    }));
        } else {
            actions.add(dialogueEngine.actionButton(
                    lang.get("reception.resign-button"),
                    NamedTextColor.RED,
                    this::openLeaveConfirmation));
        }

        dialogueEngine.openMultiAction(
                player,
                lang.get("reception.guild-reception-title"),
                body,
                actions,
                1);
    }

    private void openLeaveConfirmation(Player target) {
        ActionButton yes = dialogueEngine.actionButton(
                lang.get("reception.yes-resign"),
                NamedTextColor.RED,
                player -> {
                    profileManager.leaveGuild(player);
                    lang.send(player, "reception.left-guild");
                });
        ActionButton no = dialogueEngine.actionButton(
                lang.get("reception.no-cancel"),
                NamedTextColor.GREEN,
                player -> new ReceptionDialog(player, profileManager, dialogueEngine).open());
        dialogueEngine.openConfirmation(
                target,
                lang.get("reception.resign-title"),
                List.of(DialogBody.plainMessage(
                        lang.get("reception.resign-warning").color(NamedTextColor.WHITE))),
                yes,
                no);
    }
}
