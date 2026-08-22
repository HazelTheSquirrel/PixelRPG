package de.pixelrpg.rpg.dialogue;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Central factory for all PixelRPG native Minecraft dialogs. */
public final class DialogueEngine {
    public void openNotice(Player player, Component title, Component body, Component closeLabel) {
        Objects.requireNonNull(player, "player");
        player.showDialog(createNotice(title, body, closeLabel));
    }

    public void openMultiAction(Player player, Component title, List<DialogBody> body,
                                List<ActionButton> actions, int columns) {
        openMultiAction(player, title, body, actions, columns, null);
    }

    public void openMultiAction(Player player, Component title, List<DialogBody> body,
                                List<ActionButton> actions, int columns, Consumer<Player> backAction) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(actions, "actions");
        int safeColumns = Math.max(1, Math.min(3, columns));

        List<ActionButton> safeActions = new ArrayList<>(actions);
        ActionButton back = null;
        if (backAction != null) {
            back = actionButton(Component.text("Zurück"), NamedTextColor.WHITE, backAction);
        }
        ActionButton close = actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog);

        if (back != null) {
            safeActions.add(back);
        }

        ActionButton finalBack = back;
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(normalize(title))
                    .body(normalizeBody(body))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(safeActions, close, safeColumns));
        }));
    }

    public void openNumberRangeAction(Player player, Component title, List<DialogBody> body,
                                      DialogInput input, Component actionLabel,
                                      NamedTextColor actionColor,
                                      BiConsumer<Player, DialogResponseView> action) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(actionLabel, "actionLabel");
        Objects.requireNonNull(actionColor, "actionColor");
        Objects.requireNonNull(action, "action");

        DialogAction dialogAction = DialogAction.customClick((response, audience) -> {
            if (audience instanceof Player target) action.accept(target, response);
        }, ClickCallback.Options.builder().uses(1).build());

        ActionButton confirm = ActionButton.builder(normalize(actionLabel.color(actionColor)))
                .action(dialogAction)
                .width(220)
                .build();
        ActionButton cancel = actionButton(Component.text("Abbrechen"), NamedTextColor.RED, Player::closeDialog);

        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(normalize(title))
                    .body(normalizeBody(body))
                    .inputs(List.of(input))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(List.of(confirm, cancel), null, 2));
        }));
    }

    public void openConfirmation(Player player, Component title, List<DialogBody> body,
                                 ActionButton yes, ActionButton no) {
        Objects.requireNonNull(player, "player");
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(normalize(title))
                    .body(normalizeBody(body))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.confirmation(yes, no));
        }));
    }

    public ActionButton actionButton(Component label, NamedTextColor color, Consumer<Player> action) {
        Objects.requireNonNull(action, "action");
        DialogAction dialogAction = DialogAction.customClick((response, audience) -> {
            if (audience instanceof Player target) action.accept(target);
        }, ClickCallback.Options.builder().uses(1).build());

        return ActionButton.builder(normalize(label.color(color)))
                .action(dialogAction)
                .width(220)
                .build();
    }

    public ActionButton actionButton(Component label, Consumer<Player> action) {
        return actionButton(label, NamedTextColor.WHITE, action);
    }

    public void openUnavailable(Player player, String title, String message) {
        openNotice(player,
                Component.text(title, NamedTextColor.GOLD),
                Component.text(message, NamedTextColor.WHITE),
                Component.text("Schließen", NamedTextColor.GRAY));
    }

    private Dialog createNotice(Component title, Component body, Component closeLabel) {
        ActionButton close = actionButton(closeLabel, NamedTextColor.GRAY, Player::closeDialog);
        return Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(normalize(title))
                    .body(List.of(DialogBody.plainMessage(normalize(body))))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.notice(close));
        });
    }

    private List<DialogBody> normalizeBody(List<DialogBody> body) {
        return body.stream().map(this::normalizeBodyEntry).toList();
    }

    private DialogBody normalizeBodyEntry(DialogBody entry) {
        return DialogBody.plainMessage(normalize(entry.content()));
    }

    private Component normalize(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
