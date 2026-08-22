package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class QuestBehavior implements NpcBehavior {
    private static final int MAX_LEVEL = 100;

    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public QuestBehavior(QuestManager questManager, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public NpcType type() {
        return NpcType.QUEST;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst zuerst Rathausmitglied sein.", NamedTextColor.RED));
            return;
        }
        openQuestRanges(player);
    }

    private void openQuestRanges(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        List<Integer> categoryLevels = new TreeSet<>(questManager.getRepository().getAllQuests().stream()
                .filter(quest -> quest.type() != de.pixelrpg.rpg.quest.QuestType.GLOBAL_EVENT)
                .map(Quest::categoryLevel)
                .toList()).stream().toList();

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        "Wähle zuerst deinen gewünschten Levelbereich. Danach siehst du die Quests dieses Bereichs.",
                        NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "Dein aktuelles Level: " + profile.getLevel(), NamedTextColor.AQUA))
        );

        List<ActionButton> actions = new ArrayList<>();
        for (int index = 0; index < categoryLevels.size(); index++) {
            int start = categoryLevels.get(index);
            int end = index + 1 < categoryLevels.size()
                    ? Math.min(MAX_LEVEL, categoryLevels.get(index + 1) - 1)
                    : MAX_LEVEL;
            boolean current = profile.getLevel() >= start && profile.getLevel() <= end;
            boolean unlocked = profile.getLevel() >= start;

            actions.add(dialogueEngine.actionButton(
                    Component.text("Level " + start + "–" + end),
                    current ? NamedTextColor.GREEN : unlocked ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY,
                    target -> openQuestCategory(target, start, end)));
        }

        if (actions.isEmpty()) {
            dialogueEngine.openNotice(
                    player,
                    Component.text("Questgeber", NamedTextColor.GOLD),
                    Component.text("Aktuell sind keine Questbereiche konfiguriert.", NamedTextColor.WHITE),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }

        dialogueEngine.openMultiAction(
                player,
                Component.text("Questgeber", NamedTextColor.GOLD),
                body,
                actions,
                2);
    }

    private void openQuestCategory(Player player, int start, int end) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        List<Quest> quests = questManager.getRepository().getAllQuests().stream()
                .filter(quest -> quest.type() != de.pixelrpg.rpg.quest.QuestType.GLOBAL_EVENT)
                .filter(quest -> quest.categoryLevel() == start)
                .sorted(java.util.Comparator.comparingInt(Quest::requiredLevel).thenComparing(Quest::title))
                .toList();

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        "Questbereich Level " + start + "–" + end, NamedTextColor.AQUA)),
                DialogBody.plainMessage(Component.text(
                        "Wähle eine Quest für die vollständigen Anforderungen und Belohnungen.", NamedTextColor.WHITE))
        );

        List<ActionButton> actions = new ArrayList<>();
        for (Quest quest : quests) {
            boolean active = profile.hasActiveQuest(quest.id());
            boolean levelAvailable = profile.getLevel() >= quest.requiredLevel();
            actions.add(dialogueEngine.actionButton(
                    Component.text((active ? "[Aktiv] " : "") + quest.title()
                            + " • Level " + quest.requiredLevel()),
                    active ? NamedTextColor.YELLOW : levelAvailable ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> openQuestDetails(target, quest, start, end)));
        }

        if (actions.isEmpty()) {
            body = List.of(
                    DialogBody.plainMessage(Component.text(
                            "In diesem Bereich sind aktuell keine Quests verfügbar.", NamedTextColor.WHITE)));
        }

        actions.add(dialogueEngine.actionButton(
                Component.text("Zurück"), NamedTextColor.WHITE, this::openQuestRanges));
        dialogueEngine.openMultiAction(
                player,
                Component.text("Level " + start + "–" + end, NamedTextColor.GOLD),
                body,
                actions,
                2);
    }

    private void openQuestDetails(Player player, Quest quest, int start, int end) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        boolean active = profile.hasActiveQuest(quest.id());
        boolean completed = profile.hasCompletedQuest(quest.id());
        boolean levelAvailable = profile.getLevel() >= quest.requiredLevel();
        String status = completed ? "Bereits abgeschlossen" : active ? "Aktiv" : levelAvailable ? "Verfügbar" : "Noch nicht verfügbar";
        String rewards = "Belohnung: " + quest.rewardMoney() + " Gold, " + quest.rewardExp() + " EP";

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(quest.description(), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "Level " + quest.requiredLevel() + "+ • " + quest.requiredAmount() + "x • " + status,
                        levelAvailable ? NamedTextColor.AQUA : NamedTextColor.RED)),
                DialogBody.plainMessage(Component.text(rewards, NamedTextColor.GOLD))
        );

        List<ActionButton> actions = new ArrayList<>();
        if (!active && !completed && levelAvailable && questManager.canAccept(profile, quest)) {
            actions.add(dialogueEngine.actionButton(
                    Component.text("Quest annehmen"), NamedTextColor.GREEN,
                    target -> {
                        questManager.acceptQuest(target, quest);
                        openQuestCategory(target, start, end);
                    }));
        }
        if (active) {
            actions.add(dialogueEngine.actionButton(
                    Component.text("Quest abgeben"), NamedTextColor.YELLOW,
                    target -> {
                        questManager.completeQuest(target, quest.id());
                        openQuestCategory(target, start, end);
                    }));
            actions.add(dialogueEngine.actionButton(
                    Component.text("Quest abbrechen"), NamedTextColor.RED,
                    target -> {
                        questManager.abandonQuest(target, quest.id());
                        openQuestCategory(target, start, end);
                    }));
        }
        actions.add(dialogueEngine.actionButton(
                Component.text("Zurück"), NamedTextColor.WHITE,
                target -> openQuestCategory(target, start, end)));

        dialogueEngine.openMultiAction(
                player,
                Component.text(quest.title(), NamedTextColor.GOLD),
                body,
                actions,
                2);
    }
}
