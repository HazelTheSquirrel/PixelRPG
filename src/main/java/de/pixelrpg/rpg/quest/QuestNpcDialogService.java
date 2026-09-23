package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Native-dialog presentation of the quest domain for QUEST NPCs. */
public final class QuestNpcDialogService {
    private final QuestService quests;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogs;

    public QuestNpcDialogService(QuestService quests, PlayerProfileManager profiles, DialogueEngine dialogs) {
        this.quests = quests;
        this.profiles = profiles;
        this.dialogs = dialogs;
    }

    public void open(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            dialogs.openNotice(player, Component.text("Questgeber", NamedTextColor.GOLD),
                    Component.text("Du musst zuerst registriertes Rathausmitglied sein."),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }
        List<QuestDefinition> available = quests.repository().getAll().stream()
                .filter(q -> q.type() != QuestType.GLOBAL_EVENT)
                .sorted(Comparator.comparingInt(QuestDefinition::requiredLevel).thenComparing(QuestDefinition::title))
                .toList();
        List<ActionButton> actions = new ArrayList<>();
        for (QuestDefinition quest : available) {
            String prefix = profile.hasCompletedQuest(quest.id()) ? "✓ " : profile.hasActiveQuest(quest.id()) ? "• " : "";
            actions.add(dialogs.actionButton(Component.text(prefix + quest.title()),
                    profile.hasCompletedQuest(quest.id()) ? NamedTextColor.GRAY : NamedTextColor.YELLOW,
                    target -> openDetails(target, quest)));
        }
        if (actions.isEmpty()) {
            dialogs.openNotice(player, Component.text("Questgeber", NamedTextColor.GOLD),
                    Component.text("Aktuell sind keine Quests konfiguriert."),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }
        dialogs.openMultiAction(player, Component.text("Questgeber", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Wähle eine Quest aus.")),
                        DialogBody.plainMessage(Component.text("Dein Level: " + profile.getLevel(), NamedTextColor.AQUA))),
                actions, 2);
    }

    private void openDetails(Player player, QuestDefinition quest) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        boolean active = profile.hasActiveQuest(quest.id());
        boolean completed = profile.hasCompletedQuest(quest.id());
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(quest.description())));
        body.add(DialogBody.plainMessage(Component.text("Ziel: " + quest.targetKey() + " × " + quest.requiredAmount(), NamedTextColor.AQUA)));
        body.add(DialogBody.plainMessage(Component.text("Empfohlen ab Level " + quest.requiredLevel(), NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(Component.text("Belohnung: " + quest.reward().money() + " Gold, " + quest.reward().experience() + " EP", NamedTextColor.GOLD)));
        if (active) {
            QuestProgress progress = profile.getActiveQuests().get(quest.id());
            body.add(DialogBody.plainMessage(Component.text("Fortschritt: " + progress.getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.YELLOW)));
        }
        List<ActionButton> actions = new ArrayList<>();
        if (!completed && !active && quests.canStart(profile, quest.id())) {
            actions.add(dialogs.actionButton(Component.text("Quest annehmen"), NamedTextColor.GREEN, target -> {
                PlayerProfile current = profiles.getProfile(target.getUniqueId()).orElse(null);
                if (current != null && quests.start(current, quest.id())) profiles.saveProfileAsync(target.getUniqueId());
                openDetails(target, quest);
            }));
        }
        if (active) {
            actions.add(dialogs.actionButton(Component.text("Quest abgeben"), NamedTextColor.YELLOW, target -> {
                PlayerProfile current = profiles.getProfile(target.getUniqueId()).orElse(null);
                if (current != null && quests.isComplete(current, quest.id())) {
                    quests.complete(current, quest.id());
                    profiles.saveProfileAsync(target.getUniqueId());
                }
                openDetails(target, quest);
            }));
            actions.add(dialogs.actionButton(Component.text("Quest abbrechen"), NamedTextColor.RED, target -> {
                PlayerProfile current = profiles.getProfile(target.getUniqueId()).orElse(null);
                if (current != null && quests.abandon(current, quest.id())) profiles.saveProfileAsync(target.getUniqueId());
                openDetails(target, quest);
            }));
        }
        actions.add(dialogs.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogs.openMultiAction(player, Component.text(quest.title(), NamedTextColor.GOLD), body, actions, 2);
    }
}
