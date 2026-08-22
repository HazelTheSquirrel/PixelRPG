package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.companion.Companion;
import de.pixelrpg.rpg.companion.CompanionService;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
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
        companionService.ensureTestWolf(player.getUniqueId());
        List<Companion> companions = companionService.getCompanions(player.getUniqueId());
        List<DialogBody> body = new ArrayList<>();
        List<ActionButton> actions = new ArrayList<>();

        body.add(DialogBody.plainMessage(Component.text(
                "Begleiter sammeln eigene Erfahrung und können bis Level 99 aufsteigen. Nur aktive Begleiter erhalten Erfahrung.",
                NamedTextColor.WHITE)));

        for (Companion companion : companions) {
            long currentXp = companionService.experienceWithinLevel(companion);
            long nextXp = companionService.experienceNeededForCurrentLevel(companion);
            Component status = companion.active()
                    ? Component.text("Aktiv", NamedTextColor.GREEN)
                    : Component.text("Inaktiv", NamedTextColor.GRAY);

            body.add(DialogBody.plainMessage(Component.text()
                    .append(Component.text(companion.name(), companion.rarity().isUnique() ? NamedTextColor.GOLD : NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(companion.rarity().name(), NamedTextColor.YELLOW))
                    .append(Component.text("  •  Level ", NamedTextColor.WHITE))
                    .append(Component.text(companion.level(), NamedTextColor.AQUA))
                    .append(Component.text("  •  XP ", NamedTextColor.WHITE))
                    .append(Component.text(currentXp + "/" + (nextXp == Long.MAX_VALUE ? "MAX" : nextXp), NamedTextColor.AQUA))
                    .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                    .append(status)
                    .build()));

            if (!companion.active()) {
                actions.add(dialogueEngine.actionButton(
                        Component.text("Rufen: ").append(Component.text(companion.name(), NamedTextColor.LIGHT_PURPLE)),
                        NamedTextColor.GREEN,
                        target -> {
                            companionService.setActive(target, companion.id());
                            open(target);
                        }));
            } else {
                actions.add(dialogueEngine.actionButton(
                        Component.text("Wegschicken"), NamedTextColor.RED,
                        target -> {
                            companionService.clearActive(target);
                            open(target);
                        }));
            }

            if (!companion.rarity().isUnique()) {
                actions.add(dialogueEngine.actionButton(
                        Component.text("Umbenennen"), NamedTextColor.YELLOW,
                        target -> openRename(target, companion)));
            }
        }

        dialogueEngine.openMultiAction(
                player,
                Component.text("PixelRPG – Begleiter", NamedTextColor.GOLD),
                body,
                actions,
                2);
    }

    private void openRename(Player player, Companion companion) {
        DialogInput input = DialogInput.text(
                "name",
                220,
                Component.text("Neuer Name", NamedTextColor.WHITE),
                true,
                companion.name(),
                24,
                null);

        DialogAction rename = DialogAction.customClick((response, audience) -> {
            if (!(audience instanceof Player target)) return;
            String name = response.getText("name");
            if (name == null || name.isBlank()) {
                openRename(target, companion);
                return;
            }
            companionService.rename(target.getUniqueId(), companion.id(), name);
            open(target);
        }, ClickCallback.Options.builder().uses(1).build());

        ActionButton confirm = ActionButton.builder(Component.text("Umbenennen", NamedTextColor.GREEN))
                .action(rename)
                .width(220)
                .build();
        ActionButton cancel = dialogueEngine.actionButton(
                Component.text("Abbrechen"), NamedTextColor.RED, this::open);

        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("Begleiter umbenennen", NamedTextColor.GOLD))
                    .body(List.of(DialogBody.plainMessage(Component.text(
                            "Vergib einen Namen für deinen Begleiter.", NamedTextColor.WHITE))))
                    .inputs(List.of(input))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(List.of(confirm, cancel), null, 2));
        }));
    }
}
