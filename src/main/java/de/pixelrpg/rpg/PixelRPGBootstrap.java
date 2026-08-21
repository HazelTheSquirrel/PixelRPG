package de.pixelrpg.rpg;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.plugin.provider.PluginProviderContext;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

public final class PixelRPGBootstrap implements PluginBootstrap {
    private static final Key CHARACTER_CARD_DIALOG = Key.key("pixelrpg:character_card");
    private static final Key CHARACTER_CARD_ACTION = Key.key("pixelrpg:character_card/open");

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose()
                .newHandler(event -> event.registry().register(
                        DialogKeys.create(CHARACTER_CARD_DIALOG),
                        builder -> builder
                                .base(DialogBase.builder(Component.text("PixelRPG"))
                                        .body(List.of(io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(
                                                Component.text("Charakterkarte öffnen"))))
                                        .build())
                                .type(DialogType.notice(ActionButton.create(
                                        Component.text("Charakterkarte"),
                                        Component.text("Öffnet dein PixelRPG-Spielerprofil"),
                                        200,
                                        DialogAction.customClick(CHARACTER_CARD_ACTION, null)
                                )))
                )));

        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG),
                event -> event.registrar().addToTag(
                        DialogTagKeys.QUICK_ACTIONS,
                        Set.of(DialogKeys.create(CHARACTER_CARD_DIALOG))
                )
        );
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new PixelRPGPlugin();
    }
}
