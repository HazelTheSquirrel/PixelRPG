package de.pixelrpg.rpg.dialogue;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Central factory for all PixelRPG native Minecraft dialogs. */
public final class DialogueEngine {
    public void openNotice(Player player, Component title, Component body, Component closeLabel) {
        Objects.requireNonNull(player, "player");
        player.showDialog(createNotice(title, body, closeLabel));
    }

    public void openMultiAction(Player player, Component title, List<DialogBody> body,
                                List<ActionButton> actions, int columns) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(actions, "actions");
        int safeColumns = Math.max(1, Math.min(3, columns));

        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(normalize(title))
                    .body(body)
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(actions, null, safeColumns));
        }));
    }

    public void openConfirmation(Player player, Component title, List<DialogBody> body,
                                 ActionButton yes, ActionButton no) {
        Objects.requireNonNull(player, "player");
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(normalize(title))
                    .body(body)
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
                Component.text(message, NamedTextColor.GRAY),
                Component.text("Schließen", NamedTextColor.GREEN));
    }

    private Dialog createNotice(Component title, Component body, Component closeLabel) {
        ActionButton close = actionButton(closeLabel, NamedTextColor.GREEN, Player::closeDialog);
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

    private Component normalize(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
