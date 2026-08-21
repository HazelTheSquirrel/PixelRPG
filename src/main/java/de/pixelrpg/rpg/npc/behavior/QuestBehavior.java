package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class QuestBehavior implements NpcBehavior {
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
        openQuestDialog(player);
    }

    private void openQuestDialog(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(
                "Hier findest du Quests passend zu deinem Spielerlevel. Aktive Quests können hier auch abgegeben werden.",
                NamedTextColor.GRAY)));

        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = new ArrayList<>();

        for (Quest quest : questManager.getRepository().getQuestsByCategory(profile.getLevel()).stream().limit(6).toList()) {
            boolean active = profile.hasActiveQuest(quest.id());
            String label = active ? "[Aktiv] " + quest.title() : quest.title();
            actions.add(dialogueEngine.actionButton(
                    Component.text(label),
                    active ? NamedTextColor.YELLOW : NamedTextColor.GREEN,
                    target -> openQuestDetails(target, quest)
            ));
        }

        for (String questId : profile.getActiveQuests().keySet().stream().limit(3).toList()) {
            Quest quest = questManager.getRepository().getQuest(questId);
            if (quest == null || questManager.getRepository().getQuestsByCategory(profile.getLevel()).contains(quest)) continue;
            actions.add(dialogueEngine.actionButton(
                    Component.text("[Aktiv] " + quest.title()),
                    NamedTextColor.YELLOW,
                    target -> openQuestDetails(target, quest)
            ));
        }

        if (actions.isEmpty()) {
            dialogueEngine.openNotice(
                    player,
                    Component.text("Questgeber", NamedTextColor.GOLD),
                    Component.text("Aktuell gibt es für dein Level keine verfügbaren Quests."),
                    Component.text("Schließen", NamedTextColor.GREEN)
            );
            return;
        }

        dialogueEngine.openMultiAction(
                player,
                Component.text("Questgeber", NamedTextColor.GOLD),
                body,
                actions,
                2
        );
    }

    private void openQuestDetails(Player player, Quest quest) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        boolean active = profile.hasActiveQuest(quest.id());
        boolean completed = profile.hasCompletedQuest(quest.id());
        String status = completed ? "Bereits abgeschlossen" : active ? "Aktiv" : "Verfügbar";
        String rewards = "Belohnung: " + quest.rewardMoney() + " Gold, " + quest.rewardExp() + " EP";

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(quest.description(), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "Level " + quest.requiredLevel() + "+ • " + quest.requiredAmount() + "x • " + status,
                        NamedTextColor.GRAY)),
                DialogBody.plainMessage(Component.text(rewards, NamedTextColor.GOLD))
        );

        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = new ArrayList<>();
        if (!active && !completed && questManager.canAccept(profile, quest)) {
            actions.add(dialogueEngine.actionButton(
                    Component.text("Quest annehmen"),
                    NamedTextColor.GREEN,
                    target -> {
                        questManager.acceptQuest(target, quest);
                        openQuestDialog(target);
                    }
            ));
        }
        if (active) {
            actions.add(dialogueEngine.actionButton(
                    Component.text("Quest abgeben"),
                    NamedTextColor.YELLOW,
                    target -> {
                        questManager.completeQuest(target, quest.id());
                        openQuestDialog(target);
                    }
            ));
            actions.add(dialogueEngine.actionButton(
                    Component.text("Quest abbrechen"),
                    NamedTextColor.RED,
                    target -> {
                        questManager.abandonQuest(target, quest.id());
                        openQuestDialog(target);
                    }
            ));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, this::openQuestDialog));

        dialogueEngine.openMultiAction(
                player,
                Component.text(quest.title(), NamedTextColor.GOLD),
                body,
                actions,
                2
        );
    }
}
