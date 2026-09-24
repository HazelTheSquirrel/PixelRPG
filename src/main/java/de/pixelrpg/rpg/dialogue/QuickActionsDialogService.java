package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.companion.CompanionService;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.SoulboundService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
import de.pixelrpg.rpg.quest.QuestText;
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
import org.bukkit.Material;
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
    private final ItemService itemService;
    private final PartyManager partyManager;
    private final InviteDialogService inviteDialogService;
    private final CompanionService companionService;

    public QuickActionsDialogService(PlayerProfileManager profiles, StatEngine statEngine, QuestManager questManager, ItemService itemService,
                                     PartyManager partyManager, InviteDialogService inviteDialogService, CompanionService companionService) {
        this.profiles = profiles;
        this.statEngine = statEngine;
        this.questManager = questManager;
        this.itemService = itemService;
        this.partyManager = partyManager;
        this.inviteDialogService = inviteDialogService;
        this.companionService = companionService;
    }

    public PlayerProfileManager profileManager() { return profiles; }
    public StatEngine statEngine() { return statEngine; }
    public boolean isAvailable(Player player) { return profiles.isRegistered(player.getUniqueId()); }

    /** Opens the native PixelRPG quick-actions dialog used from the Minecraft G action. */
    public void openQuickActions(Player player) {
        if (!isAvailable(player)) return;
        DialogueEngine engine = new DialogueEngine();
        CompanionDialog companionDialog = new CompanionDialog(companionService, engine, this);
        ProfessionDialog professionDialog = new ProfessionDialog(profiles, engine, this);
        GuildDialog guildDialog = new GuildDialog(
                GuildManager.getInstance(de.pixelrpg.rpg.PixelRPGPlugin.getInstance(), profiles),
                profiles, engine, this, inviteDialogService);
        PartyDialog partyDialog = new PartyDialog(partyManager, profiles, engine, inviteDialogService, this::openQuickActions);

        List<ActionButton> actions = new ArrayList<>();
        actions.add(actionButton(Component.text("Charakterprofil", NamedTextColor.AQUA), target -> openCharacterProfile(target, companionDialog, professionDialog)));
        actions.add(actionButton(Component.text("Aktive Quests", NamedTextColor.YELLOW), target -> openActiveQuests(target, companionDialog, professionDialog)));
        actions.add(actionButton(Component.text("Begleiter", NamedTextColor.LIGHT_PURPLE), companionDialog::open));
        actions.add(actionButton(Component.text("Berufe", NamedTextColor.GREEN), professionDialog::open));
        actions.add(actionButton(Component.text("Party", NamedTextColor.AQUA), partyDialog::open));
        actions.add(actionButton(Component.text("Gilde", NamedTextColor.GOLD), guildDialog::open));
        actions.add(actionButton(Component.text("Schließen", NamedTextColor.GRAY), Player::closeDialog));

        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("PixelRPG – Schnellaktionen", NamedTextColor.GOLD))
                    .body(List.of(DialogBody.plainMessage(Component.text("Wähle eine Funktion.", NamedTextColor.WHITE))))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(actions, null, 2));
        }));
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
                Component label = quest == null ? Component.text("Unbekannte Quest", NamedTextColor.RED) : QuestText.title(quest).color(NamedTextColor.YELLOW);
                Component description = quest == null ? Component.text("Die Questdefinition konnte nicht geladen werden.", NamedTextColor.RED) : QuestText.description(quest).color(NamedTextColor.GRAY);
                Component objective = quest == null ? Component.text("Ziel unbekannt • " + progress.getCurrentAmount() + "/?", NamedTextColor.RED) : QuestText.objectiveWithProgress(quest, progress);
                actions.add(actionButton(label.append(Component.text(" • ", NamedTextColor.DARK_GRAY)).append(objective), target -> openQuestDetails(target, questId, description, companionDialog, professionDialog)));
            });
        }
        actions.add(actionButton(Component.text("Zurück", NamedTextColor.WHITE), this::openQuickActions));
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("PixelRPG – Aktive Quests", NamedTextColor.GOLD)).body(normalizeBodies(body)).canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
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
        body.add(DialogBody.plainMessage(quest != null ? QuestText.title(quest).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD) : Component.text("Unbekannte Quest", NamedTextColor.RED).decorate(TextDecoration.BOLD)));
        body.add(DialogBody.plainMessage(quest != null ? QuestText.description(quest).color(NamedTextColor.WHITE) : fallbackDescription));
        if (quest != null) {
            body.add(DialogBody.plainMessage(QuestText.objectiveWithProgress(quest, progress)));
            body.add(DialogBody.plainMessage(Component.text("Typ: " + quest.type().name(), NamedTextColor.GRAY)));
            if (quest.isProfessionQuest()) body.add(DialogBody.plainMessage(Component.text("Beruf: " + quest.profession().displayName() + " • benötigt Level " + quest.requiredProfessionLevel(), NamedTextColor.AQUA)));
            body.add(DialogBody.plainMessage(rewards(quest)));
            if (progress.hasExpiry()) body.add(DialogBody.plainMessage(Component.text("Zeit verbleibend: " + formatRemaining(progress.getExpiryTimestampMillis()), NamedTextColor.RED)));
        }
        ActionButton abandon = actionButton(Component.text("Quest abbrechen", NamedTextColor.RED), target -> {
            questManager.abandonQuest(target, questId);
            openActiveQuests(target, companionDialog, professionDialog);
        });
        ActionButton back = actionButton(Component.text("Zurück", NamedTextColor.WHITE), target -> openActiveQuests(target, companionDialog, professionDialog));
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("Questdetails", NamedTextColor.GOLD)).body(body).canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
            builder.type(DialogType.multiAction(List.of(abandon, back), DialogueEngineCloseButton.create(), 1));
        }));
    }

    private Component rewards(Quest quest) {
        List<Component> rewards = new ArrayList<>();
        if (quest.rewardExp() > 0L) rewards.add(Component.text(quest.rewardExp() + " XP", NamedTextColor.GREEN));
        if (quest.rewardMoney() > 0.0D) rewards.add(Component.text(format(quest.rewardMoney()) + " Gold", NamedTextColor.GOLD));
        for (String rewardItem : quest.rewardItemMaterials()) rewards.add(rewardItem(rewardItem));
        if (quest.rewardsCompanion()) rewards.add(Component.text(companionRewardName(quest.rewardCompanionId()), NamedTextColor.LIGHT_PURPLE));
        Component result = Component.text("Belohnungen: ", NamedTextColor.YELLOW);
        if (rewards.isEmpty()) return result.append(Component.text("Keine", NamedTextColor.GRAY));
        for (int i = 0; i < rewards.size(); i++) {
            if (i > 0) result = result.append(Component.text(" • ", NamedTextColor.DARK_GRAY));
            result = result.append(rewards.get(i));
        }
        return result;
    }

    private Component rewardItem(String raw) {
        if (raw == null || raw.isBlank()) return Component.text("Unbekannter Gegenstand", NamedTextColor.GRAY);
        String normalized = raw.trim();
        for (ItemDefinition definition : itemService.definitions()) {
            if (definition.id().equalsIgnoreCase(normalized)) return Component.text(definition.name() + " (" + rarityDisplayName(definition.rarity()) + ")", NamedTextColor.GREEN);
        }
        Material material = Material.matchMaterial(normalized);
        if (material != null) return Component.translatable(material.translationKey(), NamedTextColor.GREEN);
        String readable = normalized.replaceFirst("(?i)i(?:common|uncommon|rare|epic|legendary|unique)i\\d+$", "").replace('_', ' ').replace('|', ' ').replaceAll("\\s+", " ").trim();
        if (readable.isBlank()) readable = normalized;
        return Component.text(Character.toUpperCase(readable.charAt(0)) + readable.substring(1), NamedTextColor.GREEN);
    }

    private String rarityDisplayName(de.pixelrpg.rpg.item.ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> "Gewöhnlich";
            case UNCOMMON -> "Ungewöhnlich";
            case RARE -> "Selten";
            case EPIC -> "Episch";
            case LEGENDARY -> "Legendär";
            case UNIQUE -> "Einzigartig";
        };
    }

    private String companionRewardName(String companionId) {
        if (companionId == null || companionId.isBlank()) return "Unbekannter Begleiter";
        return switch (companionId.toLowerCase(Locale.ROOT)) {
            case "rare-bee-queen" -> "Bienenkönigin";
            case "rare-bogged" -> "Bogged";
            case "rare-spider" -> "Spinne";
            case "rare-creeper" -> "Creeper";
            case "epic-nautilus" -> "Nautilus";
            case "epic-creaking" -> "Knarzer";
            case "epic-happy-ghast" -> "Glücklicher Ghast";
            case "legendary-sulfur-cube" -> "Schwefelwürfel";
            default -> companionId.replace('-', ' ');
        };
    }

    private List<DialogBody> normalizeBodies(List<DialogBody> bodies) {
        return bodies.stream().map(body -> {
            if (body instanceof io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody plain) return DialogBody.plainMessage(plain.contents().color(NamedTextColor.WHITE), plain.width());
            return body;
        }).toList();
    }

    private ActionButton actionButton(Component label, Consumer<Player> action) {
        return ActionButton.builder(label).action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
            if (audience instanceof Player target) action.accept(target);
        }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build())).width(220).build();
    }

    public Component characterCard(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).filter(PlayerProfile::isRegistered).orElseThrow(() -> new IllegalStateException("No registered PixelRPG profile for player"));
        StatEngine.CachedStats stats = statEngine.getCachedStats(player.getUniqueId());
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : stats.maxHealth();
        double armor = player.getAttribute(Attribute.ARMOR) != null ? player.getAttribute(Attribute.ARMOR).getValue() : stats.armor();
        Component section = Component.text("────────────────────────", NamedTextColor.DARK_GRAY);
        Component header = Component.text("CHARAKTER", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        Component identity = Component.text().append(Component.text("Name: ", NamedTextColor.WHITE)).append(Component.text(player.getName(), NamedTextColor.AQUA)).append(Component.newline()).append(Component.text("Level: ", NamedTextColor.WHITE)).append(Component.text(profile.getLevel(), NamedTextColor.AQUA)).append(Component.newline()).append(Component.text("Erfahrung: ", NamedTextColor.WHITE)).append(Component.text(profile.getExperience(), NamedTextColor.AQUA)).build();
        Component statsComponent = Component.text().append(Component.text("LP: ", NamedTextColor.WHITE)).append(Component.text(format(player.getHealth()) + "/" + format(maxHealth), NamedTextColor.RED)).append(Component.newline()).append(Component.text("Rüstung: ", NamedTextColor.WHITE)).append(Component.text(format(armor), NamedTextColor.GRAY)).append(Component.newline()).append(Component.text("Bewegungsgeschwindigkeit: ", NamedTextColor.WHITE)).append(Component.text(format(stats.movementSpeedBonus()) + "%", NamedTextColor.AQUA)).append(Component.newline()).append(Component.text("Reichweite: ", NamedTextColor.WHITE)).append(Component.text(format(stats.entityReach()), NamedTextColor.AQUA)).append(Component.newline()).append(Component.text("Kritische Trefferchance: ", NamedTextColor.WHITE)).append(Component.text(format(stats.critChance()) + "%", NamedTextColor.YELLOW)).append(Component.newline()).append(Component.text("Kritischer Schaden: ", NamedTextColor.WHITE)).append(Component.text(format(stats.critDamageMultiplier()), NamedTextColor.YELLOW)).append(Component.newline()).append(Component.text("Lebensraub: ", NamedTextColor.WHITE)).append(Component.text(format(stats.lifestealBonus()) + "%", NamedTextColor.LIGHT_PURPLE)).append(Component.newline()).append(Component.text("Angriffskraft: ", NamedTextColor.WHITE)).append(Component.text(format(stats.attackPower()), NamedTextColor.GOLD)).build();
        return Component.text().append(header).append(Component.newline()).append(section).append(Component.newline()).append(identity).append(Component.newline()).append(Component.newline()).append(Component.text("WERTE", NamedTextColor.WHITE).decorate(TextDecoration.BOLD)).append(Component.newline()).append(statsComponent).build();
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
