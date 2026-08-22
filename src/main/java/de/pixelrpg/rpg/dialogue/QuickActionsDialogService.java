package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.stats.StatEngine;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.keys.DialogKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Builds the player-specific PixelRPG character card opened from the G action. */
public final class QuickActionsDialogService {
    private static final Key CHARACTER_CARD_DIALOG = Key.key("pixelrpg:character_card");

    private final PlayerProfileManager profiles;
    private final StatEngine statEngine;

    public QuickActionsDialogService(PlayerProfileManager profiles, StatEngine statEngine) {
        this.profiles = profiles;
        this.statEngine = statEngine;
    }

    public PlayerProfileManager profileManager() { return profiles; }
    public StatEngine statEngine() { return statEngine; }
    public boolean isAvailable(Player player) { return profiles.isRegistered(player.getUniqueId()); }

    /** Reopens the registered native G quick-actions dialog. */
    public void openQuickActions(Player player) {
        if (!isAvailable(player)) return;
        Dialog dialog = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.DIALOG)
                .getOrThrow(DialogKeys.create(CHARACTER_CARD_DIALOG));
        player.showDialog(dialog);
    }

    /** Opens the character profile; this view only exposes a back button to the G quick-actions menu. */
    public void openCharacterProfile(Player player, CompanionDialog companionDialog, ProfessionDialog professionDialog) {
        if (!isAvailable(player)) return;
        ActionButton back = ActionButton.builder(Component.text("Zurück", NamedTextColor.WHITE))
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick(
                        (response, audience) -> { if (audience instanceof Player target) openQuickActions(target); },
                        net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build()))
                .width(220)
                .build();
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("PixelRPG – Charakterprofil", NamedTextColor.GOLD))
                    .body(List.of(DialogBody.plainMessage(characterCard(player))))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(List.of(back), DialogueEngineCloseButton.create(), 1));
        }));
    }

    /** Opens the active-quest view from the G quick-actions menu. */
    public void openActiveQuests(Player player, CompanionDialog companionDialog, ProfessionDialog professionDialog) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId())
                .filter(PlayerProfile::isRegisteredInGuild)
                .orElse(null);
        if (profile == null) return;

        List<DialogBody> body = new ArrayList<>();
        List<ActionButton> actions = new ArrayList<>();
        if (profile.getActiveQuests().isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text("Du hast aktuell keine aktiven Quests.", NamedTextColor.WHITE)));
        } else {
            body.add(DialogBody.plainMessage(Component.text(
                    "Aktive Quests: " + profile.getActiveQuests().size() + "/" + QuestManager.MAX_ACTIVE_QUESTS, NamedTextColor.AQUA)));
            profile.getActiveQuests().forEach((questId, progress) -> actions.add(actionButton(
                    Component.text(questId + " • " + progress.getCurrentAmount(), NamedTextColor.YELLOW),
                    NamedTextColor.YELLOW,
                    target -> target.sendMessage(Component.text(
                            "Quest " + questId + ": " + progress.getCurrentAmount() + " Fortschritt", NamedTextColor.WHITE)))));
        }

        actions.add(actionButton(Component.text("Zurück"), NamedTextColor.WHITE, this::openQuickActions));
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("PixelRPG – Aktive Quests", NamedTextColor.GOLD))
                    .body(normalizeBodies(body))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(actions, DialogueEngineCloseButton.create(), 1));
        }));
    }

    private List<DialogBody> normalizeBodies(List<DialogBody> bodies) {
        return bodies.stream().map(body -> {
            if (body instanceof io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody plain) {
                return DialogBody.plainMessage(plain.contents().color(NamedTextColor.WHITE), plain.width());
            }
            return body;
        }).toList();
    }

    private ActionButton actionButton(Component label, NamedTextColor color, java.util.function.Consumer<Player> action) {
        return ActionButton.builder(label.color(color))
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick(
                        (response, audience) -> { if (audience instanceof Player target) action.accept(target); },
                        net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build()))
                .width(220)
                .build();
    }

    public Component characterCard(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId())
                .filter(PlayerProfile::isRegisteredInGuild)
                .orElseThrow(() -> new IllegalStateException("No registered PixelRPG profile for player"));
        StatEngine.CachedStats stats = statEngine.getCachedStats(player.getUniqueId());
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : stats.maxHealth();
        double armor = player.getAttribute(Attribute.ARMOR) != null ? player.getAttribute(Attribute.ARMOR).getValue() : stats.armor();

        Component section = Component.text("────────────────────────", NamedTextColor.DARK_GRAY);
        Component header = Component.text("CHARAKTER", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        Component identity = Component.text()
                .append(Component.text("Name: ", NamedTextColor.WHITE)).append(Component.text(player.getName(), NamedTextColor.AQUA)).append(Component.newline())
                .append(Component.text("Level: ", NamedTextColor.WHITE)).append(Component.text(profile.getLevel(), NamedTextColor.AQUA)).append(Component.newline())
                .append(Component.text("Klasse: ", NamedTextColor.WHITE)).append(profile.getPlayerClass().displayName().color(NamedTextColor.LIGHT_PURPLE)).build();
        Component resources = Component.text()
                .append(Component.text("Leben: ", NamedTextColor.WHITE)).append(Component.text(format(player.getHealth()) + "/" + format(maxHealth), NamedTextColor.RED)).append(Component.newline())
                .append(Component.text("Mana: ", NamedTextColor.WHITE)).append(Component.text(format(profile.getCurrentMana()) + "/" + format(stats.maxMana()), NamedTextColor.BLUE)).append(Component.newline())
                .append(Component.text("Rüstung: ", NamedTextColor.WHITE)).append(Component.text(format(armor), NamedTextColor.GRAY)).build();
        Component attributes = Component.text()
                .append(Component.text("Stärke: ", NamedTextColor.WHITE)).append(Component.text(format(stats.strength()), NamedTextColor.AQUA)).append(Component.newline())
                .append(Component.text("Beweglichkeit: ", NamedTextColor.WHITE)).append(Component.text(format(stats.agility()), NamedTextColor.AQUA)).append(Component.newline())
                .append(Component.text("Ausdauer: ", NamedTextColor.WHITE)).append(Component.text(format(stats.stamina()), NamedTextColor.AQUA)).append(Component.newline())
                .append(Component.text("Intelligenz: ", NamedTextColor.WHITE)).append(Component.text(format(stats.intellect()), NamedTextColor.AQUA)).build();
        Component combat = Component.text()
                .append(Component.text("Angriffskraft: ", NamedTextColor.WHITE)).append(Component.text(format(stats.attackPower()), NamedTextColor.YELLOW)).append(Component.newline())
                .append(Component.text("Zauberkraft: ", NamedTextColor.WHITE)).append(Component.text(format(stats.spellPower()), NamedTextColor.LIGHT_PURPLE)).append(Component.newline())
                .append(Component.text("Kritische Trefferchance: ", NamedTextColor.WHITE)).append(Component.text(format(stats.critChance()) + "%", NamedTextColor.YELLOW)).build();

        return Component.text()
                .append(header).append(Component.newline()).append(section).append(Component.newline())
                .append(identity).append(Component.newline()).append(Component.newline())
                .append(Component.text("RESSOURCEN", NamedTextColor.WHITE).decorate(TextDecoration.BOLD)).append(Component.newline()).append(resources).append(Component.newline()).append(Component.newline())
                .append(Component.text("ATTRIBUTE", NamedTextColor.WHITE).decorate(TextDecoration.BOLD)).append(Component.newline()).append(attributes).append(Component.newline()).append(Component.newline())
                .append(Component.text("KAMPF", NamedTextColor.WHITE).decorate(TextDecoration.BOLD)).append(Component.newline()).append(combat).build();
    }

    private String format(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }

    private static final class DialogueEngineCloseButton {
        private static ActionButton create() {
            return ActionButton.builder(Component.text("Schließen", NamedTextColor.GRAY))
                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick(
                            (response, audience) -> { if (audience instanceof Player target) target.closeDialog(); },
                            net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build()))
                    .width(220)
                    .build();
        }
    }
}
