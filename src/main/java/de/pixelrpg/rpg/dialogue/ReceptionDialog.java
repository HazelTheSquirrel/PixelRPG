package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.gui.AttributeTraderGUI;
import de.pixelrpg.rpg.gui.ClassSelectionGUI;
import de.pixelrpg.rpg.gui.PartyGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ReceptionDialog {
    private final Player player;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public ReceptionDialog(Player player, PlayerProfileManager profileManager) {
        this.player = player;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    public void open() {
        player.showDialog(createDialog());
    }

    private Dialog createDialog() {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        boolean registered = profile != null && profile.isRegisteredInGuild();
        List<DialogBody> body = new ArrayList<>();

        body.add(DialogBody.plainMessage(lang.get("reception.card-title", "player", player.getName())
                .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));

        if (registered && profile != null) {
            body.add(DialogBody.plainMessage(lang.get("reception.status").append(
                    lang.get("reception.status-member").color(NamedTextColor.GREEN))));
            body.add(DialogBody.plainMessage(lang.get("reception.level-label")
                    .append(Component.text(String.valueOf(profile.getLevel()), NamedTextColor.YELLOW))));
            body.add(DialogBody.plainMessage(lang.get("reception.class-label")
                    .append(profile.getPlayerClass().displayName())));
            body.add(DialogBody.plainMessage(lang.get("reception.money-label", "amount",
                    String.format("%.2f", profile.getMoney())).color(NamedTextColor.GOLD)));
        } else {
            body.add(DialogBody.plainMessage(lang.get("reception.status").append(
                    lang.get("reception.status-not-registered").color(NamedTextColor.RED))));
        }

        List<ActionButton> actions = new ArrayList<>();
        actions.add(actionButton(registered ? lang.get("reception.membership-active") : lang.get("reception.register-button"),
                registered ? NamedTextColor.GRAY : NamedTextColor.GREEN, target -> {
                    if (registered) {
                        lang.send(target, "reception.already-registered");
                        return;
                    }
                    profileManager.registerToGuild(target);
                    new ReceptionDialog(target, profileManager).open();
                }));
        actions.add(actionButton(lang.get("reception.profession-choice"), NamedTextColor.LIGHT_PURPLE, target -> {
            if (!isRegistered(target)) { lang.send(target, "common.not-registered"); return; }
            new ClassSelectionGUI(target, profileManager).open(target);
        }));
        actions.add(actionButton(lang.get("reception.attribute-distribution"), NamedTextColor.AQUA, target -> {
            if (!isRegistered(target)) { lang.send(target, "common.not-registered"); return; }
            new AttributeTraderGUI(target, profileManager, PixelRPGPlugin.getInstance().getStatEngine()).open(target);
        }));
        actions.add(actionButton(lang.get("reception.party-button"), NamedTextColor.LIGHT_PURPLE, target -> {
            if (!isRegistered(target)) { lang.send(target, "common.not-registered"); return; }
            new PartyGUI(target, PixelRPGPlugin.getInstance().getPartyManager(), profileManager).open(target);
        }));
        actions.add(actionButton(lang.get("reception.resign-button"), NamedTextColor.RED, target -> {
            if (!isRegistered(target)) { lang.send(target, "reception.not-member"); return; }
            openLeaveConfirmation(target);
        }));

        return Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(lang.get("reception.guild-reception-title")).body(body)
                    .canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
            builder.type(DialogType.multiAction(actions, null, 2));
        });
    }

    private ActionButton actionButton(Component label, NamedTextColor color, java.util.function.Consumer<Player> action) {
        DialogAction dialogAction = DialogAction.customClick((response, audience) -> {
            if (audience instanceof Player target) action.accept(target);
        }, ClickCallback.Options.builder().uses(1).build());
        return ActionButton.builder(label.color(color).decoration(TextDecoration.ITALIC, false))
                .action(dialogAction).width(220).build();
    }

    private boolean isRegistered(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        return profile != null && profile.isRegisteredInGuild();
    }

    private void openLeaveConfirmation(Player player) {
        ActionButton yes = ActionButton.builder(lang.get("reception.yes-resign").color(NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) {
                        profileManager.leaveGuild(target);
                        lang.send(target, "reception.left-guild");
                    }
                }, ClickCallback.Options.builder().uses(1).build())).width(220).build();
        ActionButton no = ActionButton.builder(lang.get("reception.no-cancel").color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) new ReceptionDialog(target, profileManager).open();
                }, ClickCallback.Options.builder().uses(1).build())).width(220).build();
        Dialog confirmation = Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(lang.get("reception.resign-title")).body(List.of(
                    DialogBody.plainMessage(lang.get("reception.resign-warning").color(NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false))))
                    .canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
            builder.type(DialogType.confirmation(yes, no));
        });
        player.showDialog(confirmation);
    }
}
