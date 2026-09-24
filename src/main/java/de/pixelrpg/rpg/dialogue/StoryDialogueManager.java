package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
import de.pixelrpg.rpg.quest.QuestText;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Native branching dialogue controller for the persistent lore campaign. */
public final class StoryDialogueManager {
    private final StoryManager storyManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final QuestManager questManager;

    public StoryDialogueManager(StoryManager storyManager,
                                PlayerProfileManager profileManager,
                                DialogueEngine dialogueEngine) {
        this.storyManager = Objects.requireNonNull(storyManager);
        this.profileManager = Objects.requireNonNull(profileManager);
        this.dialogueEngine = Objects.requireNonNull(dialogueEngine);
        this.questManager = PixelRPGPlugin.getInstance().getQuestManager();
    }

    public void openChapter(Player player, StoryChapter chapter) {
        openChapter(player, chapter, null);
    }

    public void openChapter(Player player, StoryChapter chapter, RPGNpc npc) {
        if (!valid(player) || chapter == null || !storyManager.isNextChapter(player.getUniqueId(), chapter)) {
            return;
        }
        if ("under_the_stone".equals(chapter.id()) && npc != null) {
            openEryn(player, chapter, npc);
            return;
        }
        openGeneric(player, chapter, npc);
    }

    public void openEpilogue(Player player) {
        if (!valid(player)) return;
        dialogueEngine.openMultiAction(
                player,
                Component.text("Die Archive", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                "Die bekannten Kapitel sind abgeschlossen. Sculk, Nether, Strongholds und das End "
                                        + "bilden ein Muster, aber die Welt liefert keine vollständige Erklärung.", NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                "Offene Fragen bleiben offen. Das ist kein Fehler der Chronik, sondern Teil ihrer Geschichte.", NamedTextColor.GRAY))
                ),
                List.of(dialogueEngine.actionButton(
                        Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog)), 1);
    }

    private void openGeneric(Player player, StoryChapter chapter, RPGNpc npc) {
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(chapter.title(), NamedTextColor.GOLD)));
        for (String line : chapter.dialogueLines()) {
            body.add(DialogBody.plainMessage(Component.text(line, NamedTextColor.WHITE)));
        }

        List<ActionButton> actions = new ArrayList<>();
        String questId = chapter.startQuestId();
        if (!questId.isBlank() && questManager != null) {
            Quest quest = questManager.getRepository().getQuest(questId);
            if (quest != null) {
                PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
                if (profile != null && profile.hasActiveQuest(questId)) {
                    QuestProgress progress = profile.getActiveQuests().get(questId);
                    if (progress != null && progress.getCurrentAmount() >= quest.requiredAmount() && npc != null) {
                        actions.add(dialogueEngine.actionButton(Component.text("Kapitel abschließen"), NamedTextColor.GREEN,
                                target -> finishChapterAtNpc(target, chapter, npc)));
                    } else {
                        actions.add(dialogueEngine.actionButton(Component.text("Queststatus anzeigen"), NamedTextColor.YELLOW,
                                target -> showQuestStatus(target, quest)));
                    }
                } else if (profile != null && !profile.hasCompletedQuest(questId)) {
                    actions.add(dialogueEngine.actionButton(Component.text("Quest annehmen"), NamedTextColor.GREEN,
                            target -> acceptStoryQuest(target, quest)));
                }
            }
        }

        if (questId.isBlank()) {
            actions.add(dialogueEngine.actionButton(Component.text("Kapitel abschließen"), NamedTextColor.GREEN,
                    target -> finishChapter(target, chapter)));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text(chapter.title(), NamedTextColor.GOLD), body, actions, 1);
    }

    private void openEryn(Player player, StoryChapter chapter, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        Quest expedition = questManager == null ? null : questManager.getRepository().getQuest("story_under_stone_expedition");
        Quest echoQuest = questManager == null ? null : questManager.getRepository().getQuest("story_under_stone_echo_fragments");
        QuestProgress expeditionProgress = profile.getActiveQuests().get("story_under_stone_expedition");
        QuestProgress echoProgress = profile.getActiveQuests().get("story_under_stone_echo_fragments");

        if (expeditionProgress != null && expeditionProgress.getCurrentAmount() >= expedition.requiredAmount()) {
            openErynArrival(player, chapter, npc, expedition);
            return;
        }
        if (echoProgress != null) {
            openErynEchoQuest(player, chapter, npc, echoQuest, echoProgress);
            return;
        }
        if (profile.hasCompletedQuest("story_under_stone_echo_fragments")) {
            finishChapterAtNpc(player, chapter, npc);
            return;
        }

        openErynHistory(player, chapter, npc);
    }

    private void openErynHistory(Player player, StoryChapter chapter, RPGNpc npc) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Eryn – Letzte Hüterin", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                "Eryn sieht dich lange an. "Du bist nicht aus Neugier hierher gekommen. "
                                        + "Du hast die Stadt gefunden. Das allein unterscheidet dich von den meisten."",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                ""Ich kenne keine sichere Geschichte über die Erbauer. Ich kenne nur die Spuren: "
                                        + "Sculk, gewaltige Hallen, Echo Shards und eine Stadt, die ihre Bewohner verloren hat."",
                                NamedTextColor.WHITE))
                ),
                List.of(
                        dialogueEngine.actionButton(Component.text("Erzähl mir von der Stadt"), NamedTextColor.YELLOW,
                                target -> openErynCity(target, chapter, npc)),
                        dialogueEngine.actionButton(Component.text("Wer bist du?"), NamedTextColor.AQUA,
                                target -> openErynIdentity(target, chapter, npc)),
                        dialogueEngine.actionButton(Component.text("Ich suche Antworten"), NamedTextColor.GREEN,
                                target -> openErynQuestOffer(target, chapter, npc)),
                        dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog)
                ), 1);
    }

    private void openErynCity(Player player, StoryChapter chapter, RPGNpc npc) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Die Stadt ohne Himmel", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                ""Diese Anlagen liegen tief im Deep Dark. Die Stadt selbst ist real. "
                                        + "Die Absicht ihrer Erbauer ist es nicht, zumindest nicht für uns."",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                ""Der Warden ist keine Truhe mit Beinen und kein Wächter, den jemand vor einem Schatz abgestellt hat. "
                                        + "Er reagiert auf die Welt über Vibrationen und Geruch."",
                                NamedTextColor.WHITE))
                ),
                List.of(
                        dialogueEngine.actionButton(Component.text("Was ist Sculk?"), NamedTextColor.LIGHT_PURPLE,
                                target -> openErynSculk(target, chapter, npc)),
                        dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                                target -> openErynHistory(target, chapter, npc))
                ), 1);
    }

    private void openErynIdentity(Player player, StoryChapter chapter, RPGNpc npc) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Eryn", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text(
                        ""Ich war Teil einer Expedition. Wir fanden eine Stadt, bevor wir verstanden, was wir gefunden hatten. "
                                + "Die anderen wollten Antworten. Ich wollte, dass wenigstens jemand die Fragen überlebt."",
                        NamedTextColor.WHITE))),
                List.of(
                        dialogueEngine.actionButton(Component.text("Was hast du gelernt?"), NamedTextColor.YELLOW,
                                target -> openErynSculk(target, chapter, npc)),
                        dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                                target -> openErynHistory(target, chapter, npc))
                ), 1);
    }

    private void openErynSculk(Player player, StoryChapter chapter, RPGNpc npc) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Sculk und Echo", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                ""Sculk reagiert auf Aktivität. Das ist beobachtbar. Warum es hier entstanden ist, ist eine andere Frage."",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                ""Echo Shards findest du in den Truhen der Ancient Cities. Sie sind Teil der Welt, "
                                        + "aber ihre Bedeutung für die verschwundene Kultur kennen wir nicht."",
                                NamedTextColor.WHITE))
                ),
                List.of(
                        dialogueEngine.actionButton(Component.text("Dann gib mir eine Aufgabe"), NamedTextColor.GREEN,
                                target -> openErynQuestOffer(target, chapter, npc)),
                        dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                                target -> openErynHistory(target, chapter, npc))
                ), 1);
    }

    private void openErynQuestOffer(Player player, StoryChapter chapter, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || questManager == null) return;
        Quest quest = questManager.getRepository().getQuest("story_under_stone_echo_fragments");
        if (quest == null) return;

        boolean active = profile.hasActiveQuest(quest.id());
        boolean completed = profile.hasCompletedQuest(quest.id());
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        ""Bring mir drei Echo Shards. Nicht als Trophäe. Ich will sehen, ob ihr Echo wirklich "
                                + "etwas mit den Aufzeichnungen dieser Stadt verbindet."",
                        NamedTextColor.WHITE)),
                DialogBody.plainMessage(QuestText.objective(quest).color(NamedTextColor.AQUA))
        );

        List<ActionButton> actions = new ArrayList<>();
        if (!active && !completed) {
            actions.add(dialogueEngine.actionButton(Component.text("Echo-Fragmente sammeln"), NamedTextColor.GREEN,
                    target -> {
                        PlayerProfile current = profileManager.getProfile(target.getUniqueId()).orElse(null);
                        if (current == null || current.hasActiveQuest(quest.id()) || current.hasCompletedQuest(quest.id())) return;
                        questManager.acceptQuest(target, quest);
                        openErynEchoQuest(target, chapter, npc, quest, current.getActiveQuests().get(quest.id()));
                    }));
        }
        if (active) {
            QuestProgress progress = profile.getActiveQuests().get(quest.id());
            actions.add(dialogueEngine.actionButton(Component.text("Fortschritt anzeigen"), NamedTextColor.YELLOW,
                    target -> openErynEchoQuest(target, chapter, npc, quest,
                            profileManager.getProfile(target.getUniqueId()).map(p -> p.getActiveQuests().get(quest.id())).orElse(null))));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text("Eine Spur sichern", NamedTextColor.GOLD), body, actions, 1);
    }

    private void openErynArrival(Player player, StoryChapter chapter, RPGNpc npc, Quest expedition) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Eryn – Die Stadt gefunden", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                ""Du hast sie gefunden. Jetzt weißt du, warum ich keine Legende erzählen wollte. "
                                        + "Die Stadt muss man sehen, bevor man über sie spricht."",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                "Deine Expedition ist abgeschlossen. Eryn bietet dir nun an, Echo-Fragmente zu sichern.",
                                NamedTextColor.GRAY))
                ),
                List.of(
                        dialogueEngine.actionButton(Component.text("Expedition abgeben"), NamedTextColor.GREEN,
                                target -> {
                                    if (questManager.completeQuestAtNpc(target, expedition.id(), npc.id())) {
                                        openErynQuestOffer(target, chapter, npc);
                                    }
                                }),
                        dialogueEngine.actionButton(Component.text("Geschichte hören"), NamedTextColor.YELLOW,
                                target -> openErynCity(target, chapter, npc))
                ), 1);
    }

    private void openErynEchoQuest(Player player, StoryChapter chapter, RPGNpc npc, Quest quest, QuestProgress progress) {
        if (quest == null || progress == null) return;
        boolean complete = progress.getCurrentAmount() >= quest.requiredAmount();
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(QuestText.objectiveWithProgress(quest, progress).color(NamedTextColor.AQUA)),
                DialogBody.plainMessage(Component.text(
                        complete ? ""Du hast genug gesammelt. Bring die Fragmente zu mir."" :
                                ""Drei Stück. Mehr brauche ich zunächst nicht."",
                        NamedTextColor.WHITE))
        );
        List<ActionButton> actions = new ArrayList<>();
        if (complete) {
            actions.add(dialogueEngine.actionButton(Component.text("Echo-Fragmente abgeben"), NamedTextColor.GREEN,
                    target -> {
                        if (questManager.completeQuestAtNpc(target, quest.id(), npc.id())) {
                            if (storyManager.completeChapter(target, chapter)) {
                                target.sendMessage(Component.text(
                                        "Kapitel abgeschlossen: " + chapter.title(), NamedTextColor.GOLD));
                                target.closeDialog();
                            }
                        }
                    }));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Geschichte erfahren"), NamedTextColor.YELLOW,
                target -> openErynCity(target, chapter, npc)));
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text("Echo-Fragmente", NamedTextColor.GOLD), body, actions, 1);
    }

    private void acceptStoryQuest(Player player, Quest quest) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || profile.hasActiveQuest(quest.id()) || profile.hasCompletedQuest(quest.id())) return;
        questManager.acceptQuest(player, quest);
        showQuestStatus(player, quest);
    }

    private void showQuestStatus(Player player, Quest quest) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        QuestProgress progress = profile.getActiveQuests().get(quest.id());
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(QuestText.description(quest).color(NamedTextColor.WHITE)));
        if (progress != null) body.add(DialogBody.plainMessage(QuestText.objectiveWithProgress(quest, progress).color(NamedTextColor.AQUA)));
        dialogueEngine.openMultiAction(player, QuestText.title(quest).color(NamedTextColor.GOLD), body,
                List.of(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog)), 1);
    }

    private void finishChapterAtNpc(Player player, StoryChapter chapter, RPGNpc npc) {
        String questId = chapter.completionQuestId();
        if (questId.isBlank() || questManager.completeQuestAtNpc(player, questId, npc.id())) {
            if (storyManager.completeChapter(player, chapter)) {
                player.sendMessage(Component.text("Kapitel abgeschlossen: " + chapter.title(), NamedTextColor.GOLD));
                player.closeDialog();
            }
        }
    }

    private void finishChapter(Player player, StoryChapter chapter) {
        if (storyManager.completeChapter(player, chapter)) {
            player.sendMessage(Component.text("Kapitel abgeschlossen: " + chapter.title(), NamedTextColor.GOLD));
            player.closeDialog();
        }
    }

    private boolean valid(Player player) {
        return player != null && player.isOnline() && profileManager.isRegistered(player.getUniqueId());
    }
}
