package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.companion.Companion;
import de.pixelrpg.rpg.companion.CompanionDefinition;
import de.pixelrpg.rpg.companion.CompanionService;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native current Paper dialog for companion ownership, progression, activation and equipment. */
public final class CompanionDialog {
    private static final String EQUIP_ACTION_PREFIX = "pixelrpg:companion_equip/";
    private static final String HIDDEN_QUICK_ACTION_COMPANION_ID = "common-chicken";

    private final CompanionService companionService;
    private final DialogueEngine dialogueEngine;
    private final QuickActionsDialogService quickActions;

    public CompanionDialog(CompanionService companionService, DialogueEngine dialogueEngine, QuickActionsDialogService quickActions) {
        this.companionService = companionService;
        this.dialogueEngine = dialogueEngine;
        this.quickActions = quickActions;
    }

    public void open(Player player) {
        List<Companion> companions = companionService.getCompanions(player.getUniqueId()).stream()
                .filter(companion -> !HIDDEN_QUICK_ACTION_COMPANION_ID.equalsIgnoreCase(companion.id()))
                .toList();
        List<DialogBody> body = new ArrayList<>();
        List<ActionButton> actions = new ArrayList<>();

        for (Companion companion : companions) {
            CompanionDefinition definition = companionService.definition(companion.id());

            // Der Active-State steuert ausschließlich Rufen und Wegschicken.
            if (!companion.active()) {
                actions.add(dialogueEngine.actionButton(
                        Component.text("Rufen: ").append(Component.text(companion.name(), companion.rarity().isUnique() ? NamedTextColor.GOLD : NamedTextColor.LIGHT_PURPLE)),
                        NamedTextColor.GREEN,
                        target -> {
                            companionService.setActive(target, companion.id());
                            open(target);
                        }
                ));
            } else {
                actions.add(dialogueEngine.actionButton(
                        Component.text("Wegschicken: ").append(Component.text(companion.name(), companion.rarity().isUnique() ? NamedTextColor.GOLD : NamedTextColor.LIGHT_PURPLE)),
                        NamedTextColor.RED,
                        target -> {
                            companionService.clearActive(target);
                            open(target);
                        }
                ));
            }

            // Equipment ist eine Definitionseigenschaft und steht nicht nur Unique-Companions zur Verfügung.
            if (definition.equipment().enabled()) {
                actions.add(ActionButton.builder(Component.text("Ausrüstung: ").append(Component.text(companion.name(), NamedTextColor.LIGHT_PURPLE)))
                        .action(DialogAction.customClick(Key.key(EQUIP_ACTION_PREFIX + companion.id()), null))
                        .width(220)
                        .build());
            }

            // Normale Companions können umbenannt werden; Unique-Companions haben einen festen Namen.
            if (definition.renameable() && !companion.rarity().isUnique()) {
                actions.add(dialogueEngine.actionButton(
                        Component.text("Umbenennen: ").append(Component.text(companion.name(), NamedTextColor.LIGHT_PURPLE)),
                        NamedTextColor.YELLOW,
                        target -> openRename(target, companion)
                ));
            }
        }

        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, quickActions::openQuickActions));
        dialogueEngine.openMultiAction(player, Component.text("PixelRPG – Begleiter", NamedTextColor.GOLD), body, actions, 2);
    }

    public static boolean isEquipAction(Key identifier) {
        return identifier.asString().startsWith(EQUIP_ACTION_PREFIX);
    }

    public static String companionIdFromEquipAction(Key identifier) {
        if (!isEquipAction(identifier)) return null;
        String id = identifier.asString().substring(EQUIP_ACTION_PREFIX.length()).strip();
        return id.isBlank() ? null : id;
    }

    private void openRename(Player player, Companion companion) {
        DialogInput input = DialogInput.text("name", 220, Component.text("Neuer Name", NamedTextColor.WHITE), true, companion.name(), 24, null);
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

        ActionButton confirm = ActionButton.builder(Component.text("Umbenennen", NamedTextColor.GREEN)).action(rename).width(220).build();
        ActionButton cancel = dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, this::open);

        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("Begleiter umbenennen", NamedTextColor.GOLD))
                    .body(List.of(DialogBody.plainMessage(Component.text("Vergib einen Namen für deinen Begleiter.", NamedTextColor.WHITE))))
                    .inputs(List.of(input))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(List.of(confirm, cancel), null, 2));
        }));
    }
}
