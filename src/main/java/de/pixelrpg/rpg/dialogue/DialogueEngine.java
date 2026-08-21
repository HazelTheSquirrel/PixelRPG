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

/** Central factory for PixelRPG native Minecraft dialogs. */
public final class DialogueEngine {
    public void openNotice(Player player, Component title, Component body, Component closeLabel) {
        Objects.requireNonNull(player, "player");
        player.showDialog(createNotice(title, body, closeLabel));
    }

    private Dialog createNotice(Component title, Component body, Component closeLabel) {
        Component normalizedTitle = title.decoration(TextDecoration.ITALIC, false);
        Component normalizedBody = body.decoration(TextDecoration.ITALIC, false);
        Component normalizedCloseLabel = closeLabel.decoration(TextDecoration.ITALIC, false);

        ActionButton close = ActionButton.builder(normalizedCloseLabel)
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) target.closeDialog();
                }, ClickCallback.Options.builder().uses(1).build()))
                .width(220)
                .build();

        return Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(normalizedTitle)
                    .body(List.of(DialogBody.plainMessage(normalizedBody)))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.notice(close));
        });
    }

    public void openUnavailable(Player player, String title, String message) {
        openNotice(player,
                Component.text(title, NamedTextColor.GOLD),
                Component.text(message, NamedTextColor.GRAY),
                Component.text("Schließen", NamedTextColor.GREEN));
    }
}
