package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.npc.RPGNpc;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.List;

public final class StoryNpcDialogue {
    private final PlayerProfileManager profileManager;

    public StoryNpcDialogue(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    public void begin(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du bist noch nicht für PixelRPG registriert.", NamedTextColor.RED));
            return;
        }

        player.showDialog(createDialog(npc));
    }

    private Dialog createDialog(RPGNpc npc) {
        Component title = Component.text("Geschichte", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false);
        Component body = Component.text("Für dich gibt es momentan kein neues Kapitel. "
                        + "Kehre später zu diesem NPC zurück.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
        ActionButton close = ActionButton.builder(Component.text("Schließen", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) target.closeDialog();
                }, ClickCallback.Options.builder().uses(1).build()))
                .width(220)
                .build();

        return Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(title)
                    .body(List.of(DialogBody.plainMessage(body)))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.notice(close));
        });
    }
}
