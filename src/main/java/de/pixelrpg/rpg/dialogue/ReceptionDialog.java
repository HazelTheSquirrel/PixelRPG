package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.gui.AttributeTraderGUI;
import de.pixelrpg.rpg.gui.ClassSelectionGUI;
import de.pixelrpg.rpg.gui.PartyGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ReceptionDialog {
    private final Player player;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final LanguageManager lang;

    public ReceptionDialog(Player player, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.player = player;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    public void open() {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        boolean registered = profile != null && profile.isRegisteredInGuild();
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(normalize(lang.get("reception.card-title", "player", player.getName())
                .color(NamedTextColor.AQUA))));

        if (registered && profile != null) {
            body.add(DialogBody.plainMessage(lang.get("reception.status")
                    .append(lang.get("reception.status-member").color(NamedTextColor.GREEN))));
            body.add(DialogBody.plainMessage(Component.text("Level: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(profile.getLevel()), NamedTextColor.YELLOW))));
            body.add(DialogBody.plainMessage(lang.get("reception.class-label")
                    .append(profile.getPlayerClass().displayName())));
            body.add(DialogBody.plainMessage(lang.get("reception.money-label", "amount",
                    String.format("%.2f", profile.getMoney())).color(NamedTextColor.GOLD)));
        } else {
            body.add(DialogBody.plainMessage(lang.get("reception.status")
                    .append(lang.get("reception.status-not-registered").color(NamedTextColor.RED))));
        }

        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(
                registered ? lang.get("reception.membership-active") : lang.get("reception.register-button"),
                registered ? NamedTextColor.GRAY : NamedTextColor.GREEN,
                target -> {
                    if (registered) {
                        lang.send(target, "reception.already-registered");
                        return;
                    }
                    profileManager.registerToGuild(target);
                    new ReceptionDialog(target, profileManager, dialogueEngine).open();
                }));
        actions.add(dialogueEngine.actionButton(lang.get("reception.profession-choice"), NamedTextColor.LIGHT_PURPLE, target -> {
            if (!isRegistered(target)) {
                lang.send(target, "common.not-registered");
                return;
            }
            new ClassSelectionGUI(target, profileManager).open(target);
        }));
        actions.add(dialogueEngine.actionButton(lang.get("reception.attribute-distribution"), NamedTextColor.AQUA, target -> {
            if (!isRegistered(target)) {
                lang.send(target, "common.not-registered");
                return;
            }
            new AttributeTraderGUI(target, profileManager, PixelRPGPlugin.getInstance().getStatEngine()).open(target);
        }));
        actions.add(dialogueEngine.actionButton(lang.get("reception.party-button"), NamedTextColor.LIGHT_PURPLE, target -> {
            if (!isRegistered(target)) {
                lang.send(target, "common.not-registered");
                return;
            }
            new PartyGUI(target, PixelRPGPlugin.getInstance().getPartyManager(), profileManager).open(target);
        }));
        actions.add(dialogueEngine.actionButton(lang.get("reception.resign-button"), NamedTextColor.RED, target -> {
            if (!isRegistered(target)) {
                lang.send(target, "reception.not-member");
                return;
            }
            openLeaveConfirmation(target);
        }));

        dialogueEngine.openMultiAction(
                player,
                lang.get("reception.guild-reception-title"),
                body,
                actions,
                2);
    }

    private boolean isRegistered(Player player) {
        return profileManager.isRegistered(player.getUniqueId());
    }

    private void openLeaveConfirmation(Player player) {
        ActionButton yes = dialogueEngine.actionButton(
                lang.get("reception.yes-resign"), NamedTextColor.RED,
                target -> {
                    profileManager.leaveGuild(target);
                    lang.send(target, "reception.left-guild");
                });
        ActionButton no = dialogueEngine.actionButton(
                lang.get("reception.no-cancel"), NamedTextColor.GREEN,
                target -> new ReceptionDialog(target, profileManager, dialogueEngine).open());

        dialogueEngine.openConfirmation(
                player,
                lang.get("reception.resign-title"),
                List.of(DialogBody.plainMessage(normalize(lang.get("reception.resign-warning").color(NamedTextColor.RED)))),
                yes,
                no);
    }

    private Component normalize(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
