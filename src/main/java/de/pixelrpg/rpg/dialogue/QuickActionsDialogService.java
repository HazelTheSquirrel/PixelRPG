package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.item.SoulboundService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
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
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Builds the player-specific PixelRPG character card opened from the G action. */
public final class QuickActionsDialogService {
    private static final Key CHARACTER_CARD_DIALOG = Key.key("pixelrpg:character_card");
    private final PlayerProfileManager profiles;
    private final StatEngine statEngine;
    private final QuestManager questManager;

    public QuickActionsDialogService(PlayerProfileManager profiles, StatEngine statEngine, QuestManager questManager) {
        this.profiles = profiles;
        this.statEngine = statEngine;
        this.questManager = questManager;
    }

    public PlayerProfileManager profileManager() { return profiles; }
    public StatEngine statEngine() { return statEngine; }
    public boolean isAvailable(Player player) { return profiles.isRegistered(player.getUniqueId()); }

    /** Reopens the registered native G quick-actions dialog. */
    public void openQuickActions(Player player) {
        if (!isAvailable(player)) return;
        Dialog dialog = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).getOrThrow(DialogKeys.create(CHARACTER_CARD_DIALOG));
        player.showDialog(dialog);
    }

    /** Opens the character profile and exposes the explicit soulbind action for the held item. */
    public void openCharacterProfile(Player player, CompanionDialog companionDialog, ProfessionDialog professionDialog) {
        if (!isAvailable(player)) return;
        ActionButton soulbind = ActionButton.builder(Component.text("Gegenstand binden", NamedTextColor.LIGHT_PURPLE))
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) {
                        soulbindHeldItem(target);
                        openCharacterProfile(target, companionDialog, professionDialog);
                    }
                }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build())).width(220).build();
        ActionButton back = ActionButton.builder(Component.text("Zurück", NamedTextColor.WHITE))
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) openQuickActions(target);
                }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build())).width(220).build();
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("PixelRPG – Charakterprofil", NamedTextColor.GOLD))
                    .body(List.of(DialogBody.plainMessage(characterCard(player)), DialogBody.plainMessage(Component.text("Gegenstand binden: Lege einen identifizierten Gegenstand in die Haupthand und bestätige die Aktion.", NamedTextColor.GRAY))))
                    .canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
            builder.type(DialogType.multiAction(List.of(soulbind, back), DialogueEngineCloseButton.create(), 1));
        }));
    }

    /** Applies Soulbound to the identified item currently held in the main hand. */
    private void soulbindHeldItem(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.isEmpty()) {
            player.sendMessage(Component.text("Du hältst keinen Gegenstand in der Haupthand.", NamedTextColor.RED));
            return;
        }
        SoulboundService.Result result = SoulboundService.apply(held);
        switch (result) {
            case SUCCESS -> player.sendMessage(Component.text("Der Gegenstand ist jetzt seelengebunden.", NamedTextColor.LIGHT_PURPLE));
            case ALREADY_SOULBOUND -> player.sendMessage(Component.text("Der Gegenstand ist bereits seelengebunden.", NamedTextColor.YELLOW));
            case NOT_IDENTIFIED -> player.sendMessage(Component.text("Der Gegenstand muss zuerst identifiziert werden.", NamedTextColor.RED));
        }
    }

    /** Opens the active-quest list from the G quick-actions menu. */
    public void openActiveQuests(Player player, CompanionDialog companionDialog, ProfessionDialog professionDialog) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).filter(PlayerProfile::isRegistered).orElse(null);
        if (profile == null) return;
        List<DialogBody> body = new ArrayList<>();
        List<ActionButton> actions = new ArrayList<>();
        if (profile.getActiveQuests().isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text("Du hast aktuell keine aktiven Quests.", NamedTextColor.WHITE)));
        } else {
            body.add(DialogBody.plainMessage(Component.text("Aktive Quests: " + profile.getActiveQuests().size() + "/" + QuestManager.MAX_ACTIVE_QUESTS, NamedTextColor.AQUA)));
            profile.getActiveQuests().forEach((questId, progress) -> {
                Quest quest = questManager.getRepository().getQuest(questId);
                Component label = quest == null
                        ? Component.text(questId, NamedTextColor.YELLOW)
                        : Component.text(quest.title(), NamedTextColor.YELLOW);
                Component description = quest == null
                        ? Component.text("Quest-ID: " + questId, NamedTextColor.GRAY)
                        : Component.text(quest.description(), NamedTextColor.GRAY);
                actions.add(actionButton(label.append(Component.text(" • " + progress.getCurrentAmount() + "/" + (quest == null ? "?" : quest.requiredAmount()), NamedTextColor.WHITE)), target -> openQuestDetails(target, questId, description, companionDialog, professionDialog)));
            });
        }
        actions.add(actionButton(Component.text("Zurück", NamedTextColor.WHITE), this::openQuickActions));
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

    /** Opens detailed information for one active quest and provides the abandon action. */
    private void openQuestDetails(Player player, String questId, Component fallbackDescription, CompanionDialog companionDialog, ProfessionDialog professionDialog) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).filter(PlayerProfile::isRegistered).orElse(null);
        Quest quest = questManager.getRepository().getQuest(questId);
        QuestProgress progress = profile == null ? null : profile.getActiveQuests().get(questId);
        if (profile == null || progress == null) {
            openActiveQuests(player, companionDialog, professionDialog);
            return;
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(quest != null ? quest.title() : questId, NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)));
        body.add(DialogBody.plainMessage(quest != null ? Component.text(quest.description(), NamedTextColor.WHITE) : fallbackDescription));
        if (quest != null) {
            body.add(DialogBody.plainMessage(Component.text("Ziel: " + progress.getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.AQUA)));
            body.add(DialogBody.plainMessage(Component.text("Typ: " + quest.type().name(), NamedTextColor.GRAY)));
            if (quest.rewardExp() > 0L) body.add(DialogBody.plainMessage(Component.text("Belohnung: " + quest.rewardExp() + " XP", NamedTextColor.GREEN)));
            if (quest.rewardMoney() > 0.0D) body.add(DialogBody.plainMessage(Component.text("Belohnung: " + format(quest.rewardMoney()) + " Gold", NamedTextColor.GOLD)));
            if (!quest.rewardItemMaterials().isEmpty()) body.add(DialogBody.plainMessage(Component.text("Gegenstandsbelohnungen: " + String.join(", ", quest.rewardItemMaterials()), NamedTextColor.GREEN)));
            if (quest.rewardsCompanion()) body.add(DialogBody.plainMessage(Component.text("Begleiter: " + quest.rewardCompanionId(), NamedTextColor.LIGHT_PURPLE)));
            if (progress.hasExpiry()) body.add(DialogBody.plainMessage(Component.text("Zeit verbleibend: " + formatRemaining(progress.getExpiryTimestampMillis()), NamedTextColor.RED)));
        }

        ActionButton abandon = actionButton(Component.text("Quest abbrechen", NamedTextColor.RED), target -> {
            questManager.abandonQuest(target, questId);
            openActiveQuests(target, companionDialog, professionDialog);
        });
        ActionButton back = actionButton(Component.text("Zurück", NamedTextColor.WHITE), target -> openActiveQuests(target, companionDialog, professionDialog));
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("Questdetails", NamedTextColor.GOLD))
                    .body(body)
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(List.of(abandon, back), DialogueEngineCloseButton.create(), 1));
        }));
    }

    private List<DialogBody> normalizeBodies(List<DialogBody> bodies) {
        return bodies.stream().map(body -> {
            if (body instanceof io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody plain) return DialogBody.plainMessage(plain.contents().color(NamedTextColor.WHITE), plain.width());
            return body;
        }).toList();
    }

    private ActionButton actionButton(Component label, Consumer<Player> action) {
        return ActionButton.builder(label)
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) action.accept(target);
                }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build()))
                .width(220)
                .build();
    }

    public Component characterCard(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).filter(PlayerProfile::isRegistered).orElseThrow(() -> new IllegalStateException("No registered PixelRPG profile for player"));
        StatEngine.CachedStats stats = statEngine.getCachedStats(player.getUniqueId());
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : stats.maxHealth();
        double armor = player.getAttribute(Attribute.ARMOR) != null ? player.getAttribute(Attribute.ARMOR).getValue() : stats.armor();

        Component section = Component.text("────────────────────────", NamedTextColor.DARK_GRAY);
        Component header = Component.text("CHARAKTER", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        Component identity = Component.text()
                .append(Component.text("Name: ", NamedTextColor.WHITE)).append(Component.text(player.getName(), NamedTextColor.AQUA)).append(Component.newline())
                .append(Component.text("Level: ", NamedTextColor.WHITE)).append(Component.text(profile.getLevel(), NamedTextColor.AQUA)).append(Component.newline())
                .append(Component.text("EXP: ", NamedTextColor.WHITE)).append(Component.text(profile.getExperience(), NamedTextColor.AQUA)).build();
        Component statsComponent = Component.text()
                .append(Component.text("HP: ", NamedTextColor.WHITE)).append(Component.text(format(player.getHealth()) + "/" + format(maxHealth), NamedTextColor.RED)).append(Component.newline())
                .append(Component.text("Armor: ", NamedTextColor.WHITE)).append(Component.text(format(armor), NamedTextColor.GRAY)).append(Component.newline())
                .append(Component.text("Movement Speed: ", NamedTextColor.WHITE)).append(Component.text(format(stats.movementSpeedBonus()), NamedTextColor.AQUA)).append(Component.newline())
                .append(Component.text("Reach: ", NamedTextColor.WHITE)).append(Component.text(format(stats.entityReach()), NamedTextColor.AQUA)).append(Component.newline())
                .append(Component.text("Damage: ", NamedTextColor.WHITE)).append(Component.text(format(stats.bonusDamage()), NamedTextColor.YELLOW)).append(Component.newline())
                .append(Component.text("Crit: ", NamedTextColor.WHITE)).append(Component.text(format(stats.critChance()) + "%", NamedTextColor.YELLOW)).append(Component.newline())
                .append(Component.text("Crit-Schaden: ", NamedTextColor.WHITE)).append(Component.text(format(stats.critDamageMultiplier()), NamedTextColor.YELLOW)).append(Component.newline())
                .append(Component.text("Lifesteal: ", NamedTextColor.WHITE)).append(Component.text(format(stats.lifestealBonus()) + "%", NamedTextColor.LIGHT_PURPLE)).append(Component.newline())
                .append(Component.text("Attack Power: ", NamedTextColor.WHITE)).append(Component.text(format(stats.attackPower()), NamedTextColor.GOLD)).build();
        return Component.text().append(header).append(Component.newline()).append(section).append(Component.newline()).append(identity).append(Component.newline()).append(Component.newline()).append(Component.text("STATS", NamedTextColor.WHITE).decorate(TextDecoration.BOLD)).append(Component.newline()).append(statsComponent).build();
    }

    private String format(double value) { return String.format(Locale.ROOT, "%.1f", value); }

    private String formatRemaining(long expiryTimestampMillis) {
        long seconds = Math.max(0L, (expiryTimestampMillis - System.currentTimeMillis()) / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainingSeconds = seconds % 60L;
        if (hours > 0L) return hours + "h " + minutes + "m";
        return minutes + "m " + remainingSeconds + "s";
    }

    private static final class DialogueEngineCloseButton {
        private static ActionButton create() {
            return ActionButton.builder(Component.text("Schließen", NamedTextColor.GRAY)).action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                if (audience instanceof Player target) target.closeDialog();
            }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build())).width(220).build();
        }
    }
}
