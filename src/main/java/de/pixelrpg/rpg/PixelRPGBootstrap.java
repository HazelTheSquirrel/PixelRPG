package de.pixelrpg.rpg;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Set;

public final class PixelRPGBootstrap implements PluginBootstrap {
    private static final Key CHARACTER_CARD_DIALOG = Key.key("pixelrpg:character_card");
    private static final Key PROFILE_ACTION = Key.key("pixelrpg:character_card/profile");
    private static final Key ACTIVE_QUESTS_ACTION = Key.key("pixelrpg:character_card/active_quests");
    private static final Key COMPANIONS_ACTION = Key.key("pixelrpg:character_card/companions");
    private static final Key PROFESSIONS_ACTION = Key.key("pixelrpg:character_card/professions");
    private static final Key GUILD_ACTION = Key.key("pixelrpg:character_card/guild");
    private static final Key CLOSE_ACTION = Key.key("pixelrpg:character_card/close");

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose()
                .newHandler(event -> event.registry().register(
                        DialogKeys.create(CHARACTER_CARD_DIALOG),
                        builder -> builder
                                .base(DialogBase.builder(Component.text("PixelRPG – Charakter", NamedTextColor.GOLD))
                                        .body(List.of(DialogBody.plainMessage(Component.text("Wähle eine Charakterfunktion aus.", NamedTextColor.WHITE))))
                                        .canCloseWithEscape(true)
                                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                                        .build())
                                .type(DialogType.multiAction(List.of(
                                        ActionButton.create(Component.text("Charakterprofil", NamedTextColor.WHITE), Component.text("Deine aktuellen Charakterwerte anzeigen", NamedTextColor.WHITE), 220, DialogAction.customClick(PROFILE_ACTION, null)),
                                        ActionButton.create(Component.text(" "), null, 220, null),
                                        ActionButton.create(Component.text("Aktive Quests", NamedTextColor.AQUA), Component.text("Deine aktuell laufenden Quests anzeigen", NamedTextColor.WHITE), 220, DialogAction.customClick(ACTIVE_QUESTS_ACTION, null)),
                                        ActionButton.create(Component.text("Begleiter", NamedTextColor.LIGHT_PURPLE), Component.text("Deine Begleiter verwalten", NamedTextColor.WHITE), 220, DialogAction.customClick(COMPANIONS_ACTION, null)),
                                        ActionButton.create(Component.text("Berufe", NamedTextColor.GREEN), Component.text("Berufe, Rezepte und Fortschritt verwalten", NamedTextColor.WHITE), 220, DialogAction.customClick(PROFESSIONS_ACTION, null)),
                                        ActionButton.create(Component.text("Gilde", NamedTextColor.GOLD), Component.text("Gilde gründen oder verwalten", NamedTextColor.WHITE), 220, DialogAction.customClick(GUILD_ACTION, null))
                                ), ActionButton.create(Component.text("Schließen", NamedTextColor.GRAY), Component.text("Charaktermenü schließen", NamedTextColor.WHITE), 220, DialogAction.customClick(CLOSE_ACTION, null)), 1))
                )));

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG),
                event -> event.registrar().addToTag(DialogTagKeys.QUICK_ACTIONS, Set.of(DialogKeys.create(CHARACTER_CARD_DIALOG))));
    }
}
