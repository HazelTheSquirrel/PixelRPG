package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.companion.CompanionDialogService;
import de.pixelrpg.rpg.companion.CompanionSystem;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionDialogService;
import de.pixelrpg.rpg.profession.ProfessionSystem;
import de.pixelrpg.rpg.quest.QuestDefinition;
import de.pixelrpg.rpg.quest.QuestService;
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
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class QuickActionsDialogService {
    private static final Key CHARACTER_CARD_DIALOG = Key.key("pixelrpg:character_card");
    private final PlayerProfileManager profiles;
    private final StatEngine stats;
    private final QuestService quests;
    private final CompanionSystem companions;
    private final ProfessionSystem professions;
    private final DialogueEngine dialogue = new DialogueEngine();

    public QuickActionsDialogService(PlayerProfileManager profiles, StatEngine stats, QuestService quests,
                                     CompanionSystem companions, ProfessionSystem professions) {
        this.profiles = Objects.requireNonNull(profiles);
        this.stats = Objects.requireNonNull(stats);
        this.quests = Objects.requireNonNull(quests);
        this.companions = Objects.requireNonNull(companions);
        this.professions = Objects.requireNonNull(professions);
    }

    public boolean isAvailable(Player player) {
        return profiles.isRegistered(player.getUniqueId());
    }

    public void openQuickActions(Player player) {
        if (!isAvailable(player)) return;
        Dialog dialog = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG)
                .getOrThrow(DialogKeys.create(CHARACTER_CARD_DIALOG));
        player.showDialog(dialog);
    }

    public void openProfile(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        StatEngine.CachedStats cached = stats.getCachedStats(player.getUniqueId());
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Name: " + player.getName(), NamedTextColor.AQUA)),
                DialogBody.plainMessage(Component.text("Level: " + profile.getLevel() + " • Erfahrung: " + profile.getExperience(), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Angriffskraft: " + cached.attackPower() + " • Rüstung: " + cached.armor(), NamedTextColor.GRAY))
        );
        openActions(player, Component.text("PixelRPG – Charakterprofil", NamedTextColor.GOLD), body,
                List.of(action("Zurück", NamedTextColor.WHITE, this::openQuickActions)));
    }

    public void openActiveQuests(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        List<ActionButton> actions = new ArrayList<>();
        for (String id : profile.getActiveQuests().keySet()) {
            QuestDefinition definition = quests.find(id).orElse(null);
            String label = definition == null ? id : definition.title();
            actions.add(action(label, NamedTextColor.AQUA, target -> openQuest(target, id)));
        }
        if (actions.isEmpty()) {
            openActions(player, Component.text("PixelRPG – Aktive Quests", NamedTextColor.GOLD),
                    List.of(DialogBody.plainMessage(Component.text("Du hast aktuell keine aktiven Quests.", NamedTextColor.WHITE))),
                    List.of(action("Zurück", NamedTextColor.WHITE, this::openQuickActions)));
            return;
        }
        actions.add(action("Zurück", NamedTextColor.WHITE, this::openQuickActions));
        openActions(player, Component.text("PixelRPG – Aktive Quests", NamedTextColor.GOLD), List.of(), actions);
    }

    private void openQuest(Player player, String id) {
        QuestDefinition definition = quests.find(id).orElse(null);
        if (definition == null) {
            openActions(player, Component.text("Quest", NamedTextColor.GOLD),
                    List.of(DialogBody.plainMessage(Component.text("Die Questdefinition ist nicht geladen.", NamedTextColor.RED))),
                    List.of(action("Zurück", NamedTextColor.WHITE, this::openActiveQuests)));
            return;
        }
        openActions(player, Component.text(definition.title(), NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text(definition.description(), NamedTextColor.WHITE))),
                List.of(action("Aufgeben", NamedTextColor.RED, target -> {
                    PlayerProfile p = profiles.getProfile(target.getUniqueId()).orElse(null);
                    if (p != null && quests.abandon(p, id)) profiles.saveProfileAsync(target.getUniqueId());
                    openActiveQuests(target);
                }), action("Zurück", NamedTextColor.WHITE, this::openActiveQuests)));
    }

    public void openCompanions(Player player) {
        if (!isAvailable(player)) return;
        new CompanionDialogService(companions.service(), dialogue).open(player);
    }

    public void openProfessions(Player player) {
        if (!isAvailable(player)) return;
        ProfessionDialogService service = new ProfessionDialogService(
                profiles, professions.professionService(), professions.craftingService(), dialogue);
        List<ActionButton> actions = new ArrayList<>();
        for (Profession profession : Profession.values()) {
            actions.add(action(profession.displayName(), NamedTextColor.GREEN,
                    target -> service.openProfession(target, profession)));
        }
        actions.add(action("Zurück", NamedTextColor.WHITE, this::openQuickActions));
        openActions(player, Component.text("PixelRPG – Berufe", NamedTextColor.GOLD), List.of(), actions);
    }

    public void openUnavailable(Player player, String message) {
        dialogue.openUnavailable(player, "PixelRPG", message);
    }

    private ActionButton action(String label, NamedTextColor color, Consumer<Player> consumer) {
        return dialogue.actionButton(Component.text(label), color, consumer);
    }

    private void openActions(Player player, Component title, List<DialogBody> body, List<ActionButton> actions) {
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(title).body(body).canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE).build());
            builder.type(DialogType.multiAction(actions, action("Schließen", NamedTextColor.GRAY, Player::closeDialog), 1));
        }));
    }
}
