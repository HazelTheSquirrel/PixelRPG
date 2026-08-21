package de.pixelrpg.rpg.bootstrap;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.Set;

public final class PixelRPGBootstrap implements PluginBootstrap {
    public static final TypedKey<Dialog> CHARACTER_CARD = DialogKeys.create(Key.key("pixelrpg:character_card"));
    public static final Key CHARACTER_CARD_ACTION = Key.key("pixelrpg:open_character_card");

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose()
                .newHandler(event -> event.registry().register(
                        CHARACTER_CARD,
                        builder -> builder
                                .base(DialogBase.builder(Component.text("PixelRPG Charakterkarte"))
                                        .body(DialogBody.plainMessage(Component.text("Öffne deine persönliche PixelRPG-Charakterkarte.")))
                                        .build())
                                .type(DialogType.notice(ActionButton.create(
                                        Component.text("Charakter öffnen"),
                                        Component.text("Zeigt deine aktuellen PixelRPG-Werte."),
                                        180,
                                        DialogAction.customClick(CHARACTER_CARD_ACTION, null)
                                )))
                )));

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG)
                .newHandler(event -> {
                    PostFlattenTagRegistrar<Dialog> registrar = event.registrar();
                    registrar.addToTag(DialogTagKeys.QUICK_ACTIONS, Set.of(CHARACTER_CARD));
                }));
    }
}
