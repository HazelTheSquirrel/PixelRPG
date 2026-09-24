package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestText;
import de.pixelrpg.rpg.quest.QuestType;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;\nimport java.util.function.Consumer;

public final class QuestBehavior implements NpcBehavior {
    private static final int MAX_NORMAL_LEVEL = 99;

    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public QuestBehavior(QuestManager questManager, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public NpcType type() { return NpcType.QUEST; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        onInteract(player, npc, Player::closeDialog);
    }

    @Override
    public void onInteract(Player player, RPGNpc npc, Consumer<Player> backAction) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openNotice(
                    player,
                    Component.text("Questgeber", NamedTextColor.GOLD),
                    Component.text("Du musst zuerst registriertes Rathausmitglied sein.", NamedTextColor.WHITE),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }
        openQuestRanges(player);
    }

    private int unlockBuffer() { return questManager.getRepository().unlockEarlyLevels(); }

    private boolean isWorldQuest(Quest quest) { return quest.profession() == null && quest.type() != QuestType.GLOBAL_EVENT; }

    private void openQuestRanges(Player player, Consumer<Player> backAction) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Wähle deinen gewünschten Levelbereich. Die Questbereiche sind fest auf 1–10, 11–20 usw. bis 91–99 aufgeteilt.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Dein aktuelles Level: " + profile.getLevel(), NamedTextColor.AQUA))
        );

        List<ActionButton> actions = new ArrayList<>();
        for (int start = 1; start <= MAX_NORMAL_LEVEL; start += 10) {
            final int rangeStart = start;
            final int rangeEnd = Math.min(MAX_NORMAL_LEVEL, start + 9);
            boolean current = profile.getLevel() >= rangeStart && profile.getLevel() <= rangeEnd;
            boolean unlocked = profile.getLevel() >= Math.max(1, rangeStart - unlockBuffer());
            actions.add(dialogueEngine.actionButton(Component.text("Level " + rangeStart + "–" + rangeEnd),
                    current ? NamedTextColor.GREEN : unlocked ? NamedTextColor.YELLOW : NamedTextColor.RED,
                    target -> openQuestCategory(target, rangeStart, rangeEnd)));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, backAction));\n        if (actions.size() == 1) {
            dialogueEngine.openNotice(player, Component.text("Questgeber", NamedTextColor.GOLD),
                    Component.text("Aktuell sind keine Questbereiche konfiguriert.", NamedTextColor.WHITE), Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }
        dialogueEngine.openMultiAction(player, Component.text("Questgeber", NamedTextColor.GOLD), body, actions, 2);
    }

    private void openQuestCategory(Player player, int start, int end) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        List<Quest> quests = questManager.getRepository().getAllQuests().stream()
                .filter(this::isWorldQuest).filter(quest -> quest.categoryLevel() >= start && quest.categoryLevel() <= end)
                .sorted(Comparator.comparingInt(Quest::requiredLevel).thenComparing(Quest::title)).toList();
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Questbereich Level " + start + "–" + end, NamedTextColor.AQUA)),
                DialogBody.plainMessage(Component.text("Quests werden bis zu " + unlockBuffer() + " Level vor ihrem empfohlenen Level freigeschaltet.", NamedTextColor.WHITE))
        );

        List<ActionButton> actions = new ArrayList<>();
        for (Quest quest : quests) {
            QuestStatus status = status(profile, quest);
            Component label = Component.text(status.prefix())
                    .append(QuestText.title(quest))
                    .append(Component.text(" • Level " + quest.requiredLevel(), NamedTextColor.GRAY));
            actions.add(dialogueEngine.actionButton(label, status.color(), target -> openQuestDetails(target, quest, start, end)));
        }
        if (actions.isEmpty()) body = List.of(DialogBody.plainMessage(Component.text("In diesem Bereich sind aktuell keine Quests verfügbar.", NamedTextColor.WHITE)));
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, this::openQuestRanges));
        dialogueEngine.openMultiAction(player, Component.text("Level " + start + "–" + end, NamedTextColor.GOLD), body, actions, 2);
    }

    private QuestStatus status(PlayerProfile profile, Quest quest) {
        if (profile.hasCompletedQuest(quest.id())) return QuestStatus.COMPLETED;
        if (profile.hasActiveQuest(quest.id())) return QuestStatus.ACTIVE;
        if (questManager.canAccept(profile, quest)) return QuestStatus.AVAILABLE;
        return QuestStatus.UNAVAILABLE;
    }

    private void openQuestDetails(Player player, Quest quest, int start, int end) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        QuestStatus status = status(profile, quest);
        int unlockLevel = Math.max(1, quest.requiredLevel() - unlockBuffer());

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(QuestText.description(quest).color(NamedTextColor.WHITE)));
        if (profile.hasActiveQuest(quest.id())) body.add(DialogBody.plainMessage(QuestText.objectiveWithProgress(quest, profile.getActiveQuests().get(quest.id()))));
        else body.add(DialogBody.plainMessage(QuestText.objective(quest).color(NamedTextColor.AQUA)));
        if (quest.type() == QuestType.COLLECT) body.add(DialogBody.plainMessage(QuestText.requiredItem(quest).color(NamedTextColor.AQUA)));
        body.add(DialogBody.plainMessage(Component.text(
                "Empfohlen ab Level " + quest.requiredLevel() + " • Freigeschaltet ab Level " + unlockLevel,
                status == QuestStatus.UNAVAILABLE ? NamedTextColor.RED : NamedTextColor.AQUA)));
        body.add(DialogBody.plainMessage(Component.text("Status: " + status.label(), status.color())));
        body.add(DialogBody.plainMessage(Component.text("Belohnung: " + quest.rewardMoney() + " Gold, " + quest.rewardExp() + " EP", NamedTextColor.GOLD)));

        List<ActionButton> actions = new ArrayList<>();
        if (status == QuestStatus.AVAILABLE) {
            actions.add(dialogueEngine.actionButton(Component.text("Quest annehmen"), NamedTextColor.GREEN, target -> {
                questManager.acceptQuest(target, quest);
                openQuestCategory(target, start, end);
            }));
        }
        if (status == QuestStatus.ACTIVE) {
            actions.add(dialogueEngine.actionButton(Component.text("Quest abgeben"), NamedTextColor.YELLOW, target -> {
                questManager.completeQuest(target, quest.id());
                openQuestCategory(target, start, end);
            }));
            actions.add(dialogueEngine.actionButton(Component.text("Quest abbrechen"), NamedTextColor.RED, target -> {
                questManager.abandonQuest(target, quest.id());
                openQuestCategory(target, start, end);
            }));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, target -> openQuestCategory(target, start, end)));
        dialogueEngine.openMultiAction(player, QuestText.title(quest).color(NamedTextColor.GOLD), body, actions, 2);
    }

    private enum QuestStatus {
        AVAILABLE("", "Verfügbar", NamedTextColor.GREEN),
        ACTIVE("[Aktiv] ", "Angenommen", NamedTextColor.YELLOW),
        UNAVAILABLE("", "Nicht verfügbar", NamedTextColor.RED),
        COMPLETED("", "Abgeschlossen", NamedTextColor.GRAY);

        private final String prefix;
        private final String label;
        private final NamedTextColor color;

        QuestStatus(String prefix, String label, NamedTextColor color) {
            this.prefix = prefix;
            this.label = label;
            this.color = color;
        }

        String prefix() { return prefix; }
        String label() { return label; }
        NamedTextColor color() { return color; }
    }
}
