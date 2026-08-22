package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.companion.Companion;
import de.pixelrpg.rpg.companion.CompanionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native dialog for viewing and managing the player's companions. */
public final class CompanionDialog {
    private final CompanionService companionService;
    private final DialogueEngine dialogueEngine;

    public CompanionDialog(CompanionService companionService, DialogueEngine dialogueEngine) {
        this.companionService = companionService;
        this.dialogueEngine = dialogueEngine;
    }

    public void open(Player player) {
        List<Companion> companions = companionService.getCompanions(player.getUniqueId());
        List<DialogBody> body = new ArrayList<>();
        List<ActionButton> actions = new ArrayList<>();

        if (companions.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text(
                    "Du hast noch keine Begleiter freigeschaltet.", NamedTextColor.WHITE)));
        } else {
            body.add(DialogBody.plainMessage(Component.text(
                    "Deine Begleiter", NamedTextColor.WHITE)));
            for (Companion companion : companions) {
                body.add(DialogBody.plainMessage(Component.text()
                        .append(Component.text(companion.name(), NamedTextColor.WHITE))
                        .append(Component.text("  •  Level ", NamedTextColor.WHITE))
                        .append(Component.text(companion.level(), NamedTextColor.WHITE))
                        .append(Component.text(companion.active() ? "  •  Aktiv" : "  •  Inaktiv", NamedTextColor.WHITE))
                        .build()));
                if (!companion.active()) {
                    actions.add(dialogueEngine.actionButton(
                            Component.text("Aktivieren: ").append(Component.text(companion.name())),
                            NamedTextColor.GREEN,
                            target -> {
                                companionService.setActive(target.getUniqueId(), companion.id());
                                open(target);
                            }));
                }
            }
            if (companionService.getActive(player.getUniqueId()) != null) {
                actions.add(dialogueEngine.actionButton(
                        Component.text("Begleiter ablegen"), NamedTextColor.RED,
                        target -> {
                            companionService.clearActive(target.getUniqueId());
                            open(target);
                        }));
            }
        }

        dialogueEngine.openMultiAction(player, Component.text("PixelRPG – Begleiter", NamedTextColor.GOLD),
                body, actions, 1);
    }
}
