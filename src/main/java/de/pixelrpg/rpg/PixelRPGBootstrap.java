package de.pixelrpg.rpg;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
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
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

public final class PixelRPGBootstrap implements PluginBootstrap {
    private static final Key CHARACTER_CARD_DIALOG = Key.key("pixelrpg:character_card");
    private static final Key COMPANIONS_ACTION = Key.key("pixelrpg:character_card/companions");
    private static final Key PROFESSIONS_ACTION = Key.key("pixelrpg:character_card/professions");
    private static final Key CLOSE_ACTION = Key.key("pixelrpg:character_card/close");

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose()
                .newHandler(event -> event.registry().register(
                        DialogKeys.create(CHARACTER_CARD_DIALOG),
                        builder -> builder
                                .base(DialogBase.builder(Component.text("PixelRPG – Charakter", NamedTextColor.GOLD))
                                        .body(List.of(DialogBody.plainMessage(characterCardBody())))
                                        .build())
                                .type(DialogType.multiAction(List.of(
                                        ActionButton.create(
                                                Component.text("Begleiter", NamedTextColor.LIGHT_PURPLE),
                                                Component.text("Deine Begleiter verwalten"),
                                                180,
                                                DialogAction.customClick(COMPANIONS_ACTION, null)
                                        ),
                                        ActionButton.create(
                                                Component.text("Berufe", NamedTextColor.GREEN),
                                                Component.text("Berufe, Rezepte und Fortschritt verwalten"),
                                                180,
                                                DialogAction.customClick(PROFESSIONS_ACTION, null)
                                        ),
                                        ActionButton.create(
                                                Component.text("Schließen", NamedTextColor.GRAY),
                                                Component.text("Charakterkarte schließen"),
                                                180,
                                                DialogAction.customClick(CLOSE_ACTION, null)
                                        )
                                )).build())
                )));

        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG),
                event -> event.registrar().addToTag(
                        DialogTagKeys.QUICK_ACTIONS,
                        Set.of(DialogKeys.create(CHARACTER_CARD_DIALOG))
                )
        );
    }

    private static Component characterCardBody() {
        Component separator = Component.text("────────────────────────", NamedTextColor.DARK_GRAY);
        Component identity = Component.text()
                .append(Component.text("Name: ", NamedTextColor.GRAY))
                .append(Component.selector("@s", Component.text(", ")))
                .append(Component.newline())
                .append(Component.text("Level: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_lvl").color(NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("Klasse: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_class").color(NamedTextColor.WHITE))
                .build();

        Component resources = Component.text()
                .append(Component.text("Leben: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_hp").color(NamedTextColor.RED))
                .append(Component.text(" / ", NamedTextColor.DARK_GRAY))
                .append(Component.score("@s", "px_cc_maxhp").color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.text("Mana: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_mana").color(NamedTextColor.BLUE))
                .append(Component.text(" / ", NamedTextColor.DARK_GRAY))
                .append(Component.score("@s", "px_cc_maxmana").color(NamedTextColor.BLUE))
                .append(Component.newline())
                .append(Component.text("Rüstung: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_armor").color(NamedTextColor.AQUA))
                .build();

        Component attributes = Component.text()
                .append(Component.text("Stärke: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_str"))
                .append(Component.newline())
                .append(Component.text("Beweglichkeit: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_agi"))
                .append(Component.newline())
                .append(Component.text("Ausdauer: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_sta"))
                .append(Component.newline())
                .append(Component.text("Intelligenz: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_int"))
                .build();

        Component combat = Component.text()
                .append(Component.text("Angriffskraft: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_atk"))
                .append(Component.newline())
                .append(Component.text("Zauberkraft: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_spell"))
                .append(Component.newline())
                .append(Component.text("Kritische Trefferchance: ", NamedTextColor.GRAY))
                .append(Component.score("@s", "px_cc_crit"))
                .append(Component.text("%", NamedTextColor.GRAY))
                .build();

        return Component.text()
                .append(Component.text("CHARAKTER", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(separator).append(Component.newline())
                .append(identity).append(Component.newline()).append(Component.newline())
                .append(Component.text("RESSOURCEN", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                .append(Component.newline()).append(resources).append(Component.newline()).append(Component.newline())
                .append(Component.text("ATTRIBUTE", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD))
                .append(Component.newline()).append(attributes).append(Component.newline()).append(Component.newline())
                .append(Component.text("KAMPF", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .append(Component.newline()).append(combat)
                .build();
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new PixelRPGPlugin();
    }
}
